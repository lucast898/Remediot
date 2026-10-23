# Roteiro de testes na placa — RemedIoT (MVP)

Todos os testes rodam no **modo demo**, com doses a cada 3 min, em poucos minutos cada. Cada teste traz o objetivo, os passos e o resultado esperado no **Monitor Serial**, no **LED**, no **buzzer** e no **MQTT**.

Antes destes testes, a lógica já foi verificada no simulador do PC (`firmware/testes-pc/rodar.sh`, 140 verificações). Este roteiro confirma o comportamento no hardware real.

## Preparação (uma vez)

1. No `config.h`, descomente `#define MODO_DEMO`, grave e abra o Monitor Serial em 115200.
2. Anote o `deviceId` que aparece no início do log (ex.: `A4CF12B3C4D5`). Nos exemplos abaixo, `{id}` é esse valor.
3. Abra o **Online Test** do console do EMQX (ou o MQTTX) com o usuário do **backend** e assine `remediot/v1/#`. Tudo o que o ESP publica aparece ali.
4. **Dica para a banca:** use o **roteador do celular** como Wi-Fi. Assim dá para "cortar a internet" na hora, só desligando o roteador do celular.
5. Para mandar um comando sem o app, publique em `remediot/v1/{id}/comando` (QoS 1, **retained desligado**). Troque o `commandId` a cada envio.

**Horários do modo demo.** As doses caem nos minutos múltiplos de 3 (`xx:00`, `xx:03`, `xx:06`...), e o log mostra o nome e o **peso** de cada uma (ex.: `nova ocorrência: demo-20:06 ... peso 1`). O peso alterna entre 1, 2 e 3 de uma dose para a outra. As regras são as do app, encurtadas. Para uma dose das `xx:03`:

| Período | LED e buzzer | Se a gaveta abrir |
|---|---|---|
| `xx:02:30` a `xx:03:00` | normal | Dose antecipada (app: `TOMADA`) |
| `xx:03:00` a `xx:04:00` | LED azul piscando, **sem som** | Dose confirmada (app: `TOMADA`) |
| `xx:04:00` a `xx:05:30` | LED vermelho; peso 2 e 3 apitam de `xx:04:00` a `xx:04:30` | Dose atrasada (app: `ATRASADA`) |
| `xx:05:30` | vira não retirada (app: `NAO_RETIRADA`) | conta para a próxima dose, cuja tolerância começa nesse instante |

Se a dose já tiver sido registrada, uma abertura entre `xx:04:00` e `xx:05:30` é **fora de horário**.

"Abrir/fechar" = afastar/aproximar o ímã do sensor (ou abrir a gaveta de verdade).

**Sessão de 30 s.** Entre dois testes que dependem de "abertura nova", deixe a gaveta **fechada por mais de 30 s**. O log mostra `sessão de abertura encerrada`.

---

### T01 — Primeira configuração do Wi-Fi

**Objetivo:** configurar o Wi-Fi sem senha no código.

1. Apague a rede salva: grave uma vez com *Erase All Flash Before Sketch Upload* ligado (depois volte para *Disabled*), ou mande `RESETAR_WIFI` (T19).
2. Ligue o ESP32.
3. No celular, conecte na rede `RemedIoT-XXXX` com a senha `PORTAL_SENHA`, abra `192.168.4.1` e escolha a rede da casa.

**Resultado esperado:**
- Serial: `portal aberto`, depois `conectado a "..."` e `hora acertada pelo NTP`.
- LED: branco piscando lento enquanto o portal está aberto; depois verde.

---

### T02 — Conexão segura e status online

**Objetivo:** conectar com TLS e usuário/senha e publicar o heartbeat.

1. Com o Wi-Fi configurado, reinicie o ESP32 (botão EN).

**Resultado esperado:**
- Serial: `conectando a ...:8883 (TLS)...` e `conectado como remediot-{id}`.
- MQTT: em `remediot/v1/{id}/status` chega `"online":true`, com `fonteHora`, `rssi` e `filaOffline`. No modo demo, um novo status a cada 15 s.
- MQTT: em `eventos`, chega `DISPOSITIVO_INICIADO` com `"motivoReset":"POWERON"`. No ESP32, o botão EN conta como "ligado na energia".
- No Online Test do EMQX (ou no MQTTX), a mensagem de status aparece como **retained**.
- LED: amarelo (sem servidor) por alguns segundos enquanto conecta, depois verde. Se ficar amarelo com o Wi-Fi conectado, é o T21.

---

### T03 — Agenda pelo app: aplicada e rejeitada

**Objetivo:** receber, validar e confirmar a agenda no formato do app (um remédio por horário, com peso).

1. Publique em `remediot/v1/{id}/config/agenda` com **retained ligado** e QoS 1:
   ```json
   {"versao":1,"configId":"cfg-t03a","doses":[{"doseId":"s1","hora":"08:00","peso":1},{"doseId":"s2","hora":"08:00","peso":3},{"doseId":"s3","hora":"08:10","peso":2}]}
   ```
2. Publique a versão 2 com um peso inválido:
   ```json
   {"versao":2,"configId":"cfg-t03b","doses":[{"doseId":"s1","hora":"08:00","peso":5}]}
   ```
3. Reinicie o ESP32.

**Resultado esperado:**
1. Ack em `remediot/v1/{id}/ack` com `"commandId":"cfg-t03a","status":"APLICADA","doses":3`. O Serial lista a agenda com o peso e "todos os dias". Dois remédios no mesmo horário e um a 10 min são aceitos (horários livres, como no app).
2. Ack `"status":"REJEITADA"` com `"codigo":"PESO_INVALIDO"`. A agenda v1 continua valendo.
3. Depois do reinício, o broker reentrega a agenda retida (a v2, que é a última publicada). O ESP rejeita de novo e continua na v1, que está salva na NVS. Na prática isso não acontece com o app, porque o backend valida a agenda antes de publicar.

No modo demo as doses usadas são as sintéticas (a cada 3 min). A agenda é validada e salva do mesmo jeito.

---

### T04 — Dose antecipada

1. Espere o log `nova ocorrência: demo-HH:MM` da próxima dose.
2. Abra a gaveta nos **30 s antes** do horário e feche.

**Resultado esperado:**
- Serial: `AGUARDANDO -> ANTECIPADA` em destaque.
- MQTT: `DOSE_ANTECIPADA` com `antecedenciaSeg` entre 1 e 30 e o `peso`.
- No horário da dose, o LED **não** pisca azul e nada apita.

---

### T05 — Dose confirmada (no horário, sem som)

1. Com a gaveta fechada, espere o horário da próxima dose.
2. Uns 10 s depois, abra a gaveta.

**Resultado esperado:**
- No horário: `AGUARDANDO -> ALARME_ATIVO`, evento `ALARME_INICIADO` com `"sonoro":false`. LED azul piscando rápido e **buzzer quieto** (igual ao app: o lembrete de voz é da Alexa).
- Ao abrir: `ALARME_ATIVO -> CONFIRMADA`, `DOSE_CONFIRMADA` com `atrasoSeg` perto de 10. O LED volta ao verde.

---

### T06 — Dose em atraso e alarme por peso

1. Escolha uma dose de **peso 2 ou 3** (o log mostra o peso) e não abra a gaveta por 60 s depois do horário.
2. Repita com uma dose de **peso 1**.

**Resultado esperado:**
1. Aos 60 s: `ALARME_ATIVO -> EM_ATRASO`, `DOSE_EM_ATRASO` com `"motivo":"SEM_ABERTURA"` e `"alarmeSonoro":true`. O buzzer toca (3 bipes e pausa) por 30 s com o LED azul; depois o LED fica vermelho.
2. Peso 1: o mesmo evento com `"alarmeSonoro":false`, **sem som**, LED vermelho.

---

### T07 — Dose atrasada e dose não retirada

1. Logo depois do T06 (antes de `xx:05:30`), abra a gaveta.
2. Na dose seguinte, não abra a gaveta por 150 s depois do horário.

**Resultado esperado:**
1. `EM_ATRASO -> ATRASADA`, `DOSE_ATRASADA` com `atrasoSeg`. **Não** sai `ABERTURA_FORA_HORARIO`. O LED sai do vermelho e o buzzer para, se estava tocando.
2. Aos 150 s (`xx:05:30`): `EM_ATRASO -> PERDIDA`, `DOSE_PERDIDA` com `"motivo":"SEM_ABERTURA"` (no app: `NAO_RETIRADA`). LED vermelho até a próxima abertura.

---

### T08 — Abertura fora de horário

1. Depois de uma dose registrada (antecipada, confirmada ou atrasada), com a gaveta fechada há mais de 30 s, abra a gaveta entre `xx:04:00` e `xx:05:30`.

**Resultado esperado:**
- `ABERTURA_FORA_HORARIO` imediato, com `proximaDose` mostrando a próxima `demo-HH:MM`.
- O LED fica vermelho. Anote o `eventId` (ex.: `{id}-00000042`).

---

### T09 — "Fui eu"

1. Com o alerta do T08 pendente (LED vermelho), publique em `comando`:
   ```json
   {"commandId":"t09-1","tipo":"RECONHECER_ALERTA","parametros":{"eventId":"{id}-00000042"}}
   ```
2. Publique de novo com `"commandId":"t09-2"` e o mesmo `eventId`.

Sem MQTT, dá para usar a tecla `k` no Serial, que reconhece o alerta mais recente.

**Resultado esperado:**
1. Ack `"status":"OK"`, Serial `alerta reconhecido` e o LED volta ao verde.
2. Ack `"status":"IGNORADO"` com `"mensagem":"ALERTA_NAO_PENDENTE"`.

Com o backend rodando, o mesmo fluxo é `POST /api/dispositivos/{id}/alertas/{eventId}/fui-eu`, e o evento fica marcado como `CUIDADOR` no banco.

---

### T10 — Possível dose dupla e sessão de 30 s

1. Confirme uma dose (T05) logo no começo do alarme e feche a gaveta.
2. **Antes de 30 s**, abra e feche de novo.
3. Espere a gaveta ficar fechada por **mais de 30 s** e abra de novo, ainda dentro da tolerância (até `xx:04:00`).

**Resultado esperado:**
2. Serial `ABRIU (mesma sessão)` e **nenhum evento**.
3. Serial `ABRIU (sessão nova)` e `POSSIVEL_DOSE_DUPLA` com `"aberturasNaJanela":2` (um evento por dose; no demo há uma dose por horário). O LED fica vermelho.

---

### T11 — Modo reabastecer

1. Logo depois de registrar uma dose (ex.: confirme em `xx:03:10` e ative em `xx:03:20`), publique:
   ```json
   {"commandId":"t11-1","tipo":"MODO_REABASTECER","parametros":{"acao":"INICIAR"}}
   ```
   Pelo Serial: tecla `r`.
2. Abra e feche a gaveta duas vezes, com mais de 30 s entre as aberturas.
3. Espere o modo acabar (60 s no demo).
4. Tente ativar de novo **dentro dos 30 s antes de uma dose**, ou com um alarme de atraso tocando.
5. Ative a uns 50 s antes de uma dose (com a dose anterior já registrada) e espere.

**Resultado esperado:**
1. Ack `OK`, `MODO_REABASTECER_INICIADO` e LED roxo.
2. Duas vezes `REABASTECIMENTO` e **nenhum** `ABERTURA_FORA_HORARIO`.
3. `MODO_REABASTECER_ENCERRADO` com `"motivo":"TEMPO"` e `"aberturas":2`.
4. Ack `"status":"ERRO","mensagem":"DOSE_EM_ANDAMENTO"`.
5. Quando faltam 30 s para a dose, `MODO_REABASTECER_ENCERRADO` com `"motivo":"JANELA_DE_DOSE"`. A abertura seguinte conta como `DOSE_ANTECIPADA`.

---

### T12 — Gaveta esquecida aberta

1. Abra a gaveta (fora do modo reabastecer) e deixe aberta por mais de 20 s.
2. Feche.

**Resultado esperado:**
1. Aos 20 s: `GAVETA_ESQUECIDA_ABERTA` com `abertaHaSeg`, 2 bipes curtos repetidos a cada 10 s e LED vermelho.
2. Ao fechar: `GAVETA_FECHADA_APOS_ALERTA` com `abertaPorSeg`. O LED sai do vermelho.

---

### T13 — Queda de internet com reenvio da fila

1. Desligue o roteador do celular.
2. Faça duas aberturas fora de horário, com mais de 30 s entre elas.
3. Em menos de 1 min, religue o roteador. Com mais tempo, o portal abre (é o T20).

**Resultado esperado:**
1. Serial `Wi-Fi desconectado` e `[LED  ] AMARELO (sem servidor: Wi-Fi desconectado)`. O LED fica amarelo, ou pisca amarelo a cada 5 s por cima de outra cor.
2. Os eventos aparecem no Serial, mas **não** chegam no MQTT. No `s` do Serial, a `fila` aumenta.
3. Ao reconectar, chegam os dois `ABERTURA_FORA_HORARIO`, com o **`ts` do momento da abertura** e não do envio.
   - Sem backend confirmando, cada evento é reenviado a cada 15 s com `"reenvio":true` e o **mesmo `eventId`**.
   - Com o backend rodando, a fila zera.
   - Sem backend, dá para confirmar à mão em `remediot/v1/{id}/eventos/confirmacao`: `{"eventIds":["{id}-000000NN"]}`.
4. O LED das doses e o alarme de atraso continuam funcionando normalmente durante a queda.

---

### T14 — Falta de energia (power bank)

**Objetivo:** verificar se o power bank escolhido segura o aparelho quando a tomada cai.

1. Com o ESP32 no power bank e o power bank na tomada, **tire o carregador da tomada**.
2. Espere 1 min e recoloque.
3. Deixe 1 hora só no power bank, sem tomada.

**Resultado esperado:**
- O ESP32 **continua ligado**: nenhum `DISPOSITIVO_INICIADO` novo e o `uptimeSeg` do status continua crescendo.
- Se ele reiniciar ao tirar ou recolocar o carregador, o power bank corta a saída na troca. Aparece um novo `DISPOSITIVO_INICIADO` com `BROWNOUT` ou `POWERON`. **Anotem o modelo**: com ele, uma queda de energia sem internet deixa o aparelho sem hora (T15).
- Depois de 1 hora, o aparelho ainda está ligado. Alguns power banks se desligam sozinhos com consumo baixo.

O firmware do MVP **não detecta** a falta de energia nem mede a bateria (decisão do grupo).

---

### T15 — Reinício sem internet (sem RTC)

**Objetivo:** documentar o comportamento sem RTC. Com o RTC, que é evolução futura, a dose seguinte dispararia normalmente.

1. Desligue o roteador do celular.
2. Desligue e religue o ESP32 da alimentação.
3. Espere 30 s.
4. Abra e feche a gaveta.
5. Religue o roteador.

Se os passos 2 a 4 levarem mais de 1 min, o portal de Wi-Fi abre (LED branco), o que é esperado (T20).

**Resultado esperado:**
2. LED **laranja piscando**. Serial com hora em formato `[+12.3s]`, porque não há hora.
3. `FALHA_RELOGIO` com `"motivo":"SEM_HORA_CONFIAVEL"`, que fica na fila.
4. `ABERTURA_NAO_CLASSIFICADA`. **Nenhuma dose dispara** enquanto não houver hora.
5. `hora acertada pelo NTP`, depois `RELOGIO_RESTABELECIDO` com `semHoraSeg`. Os eventos da fila chegam ao MQTT com `"tsEstimado":true` e o `ts` calculado. As doses voltam a funcionar.

---

### T16 — Reinício durante o alarme de atraso

**Objetivo:** o estado da dose sobrevive a um reinício.

1. Com internet, deixe uma dose de **peso 2 ou 3** entrar em atraso (buzzer tocando).
2. Uns 5 s depois, aperte o botão **EN**.
3. Abra a gaveta antes de `xx:05:30`.

**Resultado esperado:**
2. Novo `DISPOSITIVO_INICIADO` com `"motivoReset":"POWERON"`. Assim que o NTP responde, alguns segundos depois, **o alarme de atraso volta a tocar** (se ainda estiver nos 30 s) sem um novo `DOSE_EM_ATRASO`. Passados os 30 s, o LED fica vermelho.
3. `DOSE_ATRASADA` normal.

---

### T17 — Reinício por comando

1. Publique:
   ```json
   {"commandId":"t17-1","tipo":"REINICIAR"}
   ```

**Resultado esperado:**
- Ack `OK`.
- Status `"online":false,"motivo":"REINICIO_SOLICITADO"`.
- O ESP32 reinicia e manda `DISPOSITIVO_INICIADO` com `"motivoReset":"SW"`, seguido de status `"online":true`.

---

### T18 — Dispositivo offline (LWT)

1. Desligue o ESP32 da alimentação de uma vez (o power bank inteiro) e espere uns 45 s.
2. Religue.

**Resultado esperado:**
1. O **broker** publica em `status`: `{"online":false,...,"motivo":"CONEXAO_PERDIDA"}` (retained). O backend loga o alerta de dispositivo offline.
2. O primeiro heartbeat substitui a mensagem retida por `"online":true`.

---

### T19 — Outros comandos

Publique um de cada vez, trocando o `commandId`:

| Comando | Resultado esperado |
|---|---|
| `{"commandId":"t19-1","tipo":"LOCALIZAR","parametros":{"duracaoSeg":5}}` | Ack `OK` e bipes rápidos por 5 s |
| `{"commandId":"t19-1","tipo":"LOCALIZAR"}` (mesmo id) | Ack `DUPLICADO`, sem som |
| `{"commandId":"t19-2","tipo":"LOCALIZAR","validoAte":1}` | Ack `EXPIRADO`, sem som |
| `{"commandId":"t19-3","tipo":"SILENCIAR_ALARME"}` durante um alarme de atraso (peso 2 ou 3) | Ack `OK` e `ALARME_SILENCIADO`. O som para, mas o LED continua azul até o fim dos 30 s. Se ninguém abrir: `DOSE_PERDIDA` com `"alarmeSilenciado":true` |
| `{"commandId":"t19-4","tipo":"SILENCIAR_ALARME"}` sem som tocando (inclusive no horário da dose, quando só o LED pisca) | Ack `IGNORADO` |
| `{"commandId":"t19-5","tipo":"PEDIR_STATUS"}` | Ack `OK` e um status imediato |
| `{"commandId":"t19-6","tipo":"RESSINCRONIZAR_AGENDA"}` | Ack `OK` com `dados.agendaVersao`. O Serial mostra a assinatura refeita e a agenda retida reentregue |
| `{"commandId":"t19-7","tipo":"FAZER_CAFE"}` | Ack `ERRO` com `TIPO_DESCONHECIDO` |
| `{"commandId":"t19-8","tipo":"RESETAR_WIFI"}` | Ack `OK` e status `"online":false,"motivo":"TROCANDO_WIFI"` (os dois chegam antes de o Wi-Fi desligar); a rede salva é apagada e o portal abre (volta ao T01) |

---

### T20 — Portal automático sem Wi-Fi

1. Desligue o roteador do celular e espere 1 min (demo).
2. Deixe uma dose chegar com o portal aberto.
3. Espere o portal fechar (2 min no demo) e religue o roteador.

**Resultado esperado:**
1. Serial `sem Wi-Fi há 60 s: abrindo o portal`. LED branco piscando e a rede `RemedIoT-XXXX` aparece.
2. O LED fica azul no horário da dose (tem prioridade sobre o portal) e, se a dose atrasar com peso 2 ou 3, o buzzer toca normalmente.
3. `portal fechado`, depois reconecta sozinho na rede antiga.

---

### T21 — Sem servidor com o Wi-Fi funcionando

**Objetivo:** mostrar que o amarelo quer dizer "sem servidor" e que o log aponta a causa.

1. No `secrets.h`, troque uma letra da `MQTT_SENHA` e grave.
2. Observe o Monitor Serial por 1 min. Abra e feche a gaveta uma vez.
3. Corrija a senha e grave de novo.

**Resultado esperado:**
1. Serial `conectado a "..."` (o Wi-Fi está ok), seguido de `[MQTT ] falhou (estado 5: acesso negado ...)` (ou `estado 4: usuário ou senha errados`) e da dica `confira MQTT_USUARIO e MQTT_SENHA ...`. As tentativas se espaçam: 2 s, 4 s, 8 s... até 60 s.
2. `[LED  ] AMARELO (sem servidor)`, **sem** "Wi-Fi desconectado". A abertura gera o evento no Serial e ele fica na fila (`s` mostra a `fila`).
3. Conecta, o LED fica verde e o evento guardado chega no MQTT com o `ts` da abertura.

---

## Testes que voltam com o RTC

O roteiro original tinha "queda de energia sem internet com a dose disparando pelo RTC", "RTC com bateria removida" e "correção do RTC pelo NTP". Eles saem deste MVP junto com o módulo DS3231. O comportamento atual nesses casos é o do **T15**.

## Registro dos resultados

| Teste | Data | Quem testou | OK / Falhou | Observações |
|---|---|---|---|---|
| T01 | | | | |
| T02 | | | | |
| T03 | | | | |
| T04 | | | | |
| T05 | | | | |
| T06 | | | | |
| T07 | | | | |
| T08 | | | | |
| T09 | | | | |
| T10 | | | | |
| T11 | | | | |
| T12 | | | | |
| T13 | | | | |
| T14 | | | | |
| T15 | | | | |
| T16 | | | | |
| T17 | | | | |
| T18 | | | | |
| T19 | | | | |
| T20 | | | | |
