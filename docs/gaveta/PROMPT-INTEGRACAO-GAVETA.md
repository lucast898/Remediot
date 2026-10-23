# PROMPT-INTEGRACAO-GAVETA.md — ligando o app à gaveta (ESP32)

Este é o plano para integrar o app com a gaveta real. O firmware do ESP32 já está pronto (parte do Cesar) e conversa só por **MQTT** (EMQX Cloud Serverless, gratuito). Falta o lado do app: receber os eventos da gaveta, mandar a agenda e os comandos, e dar ao usuário as telas para conectar e controlar a gaveta.

Como nas fases anteriores, cole os blocos no Claude Code **um de cada vez** e faça `git commit` ao fim de cada fase que funcionar.

> **Enquanto isso não é feito:** a pasta `ponte-app/` do pacote do firmware liga a gaveta ao app **como ele está hoje**, sem mudar nada no app. Ela faz o papel do Node-RED: lê os remédios pela API, manda a agenda para a gaveta e chama `POST /api/devices/events` quando a gaveta registra uma dose. A lista do que falta no app (horário real do evento, alertas da gaveta, comandos, pareamento) está em `ponte-app/LEIA-ME.md` e é o que as fases abaixo resolvem. Quando a integração definitiva estiver pronta, a ponte deixa de ser usada.

## Antes de começar

1. Crie a pasta `docs/gaveta/` na raiz do repositório e coloque nela, tirados do pacote do firmware:
   - `CONTRATO-MQTT.md`: o formato de **todas** as mensagens entre a gaveta e o backend. É a referência principal.
   - a pasta `backend-exemplo/`: exemplo funcional em Spring Boot (MQTT com TLS, `record`s do contrato, idempotência, testes com JSON real do firmware). Ele usa Boot 3.5, então serve de **referência para adaptar**, não para copiar como está.
   - este arquivo.
2. Confira a seção **Decisões do grupo** logo abaixo. Os valores marcados como *sugestão* ainda precisam ser confirmados pelo grupo. Se algo mudar, edite a seção antes de colar as fases.
3. Nada no contrato muda sem falar com o Cesar: o firmware depende dele.

## Decisões do grupo

As regras de dose ficaram **iguais às do app por enquanto** (o grupo vai rever depois). O firmware já foi ajustado a elas, então o `DoseStatusService`, os horários livres e os pesos do app **não mudam**.

| # | Tema | Decisão |
|---|---|---|
| D1 | Janela da dose | **Igual ao app:** de 30 min antes até 30 min depois = `TOMADA`; depois disso, até 3 h = `ATRASADA`; 3 h sem abrir = `NAO_RETIRADA`. O firmware usa os mesmos valores e manda o status pronto no evento |
| D2 | Horários dos remédios | **Igual ao app:** horários livres por remédio, sem dias da semana. Cada `MedicationSchedule` vira uma entrada da agenda da gaveta |
| D3 | Limites | Sem limite por dia e sem intervalo mínimo. Só o limite de memória da gaveta: **40 entradas** (remédio x horário) por paciente |
| D4 | Uma abertura | A gaveta não sabe qual remédio saiu: uma abertura registra **todas** as doses no prazo e manda **um evento por dose** (por `MedicationSchedule`). O alerta de cada dose usa o peso do próprio remédio, como o `AlertService` já faz |
| D5 | Buzzer e peso | **Igual ao app:** no horário a gaveta só pisca o LED (o lembrete de voz é da Alexa). Quando a dose atrasa, peso 2 e 3 apitam na própria gaveta, sem precisar de comando. A agenda leva o peso de cada remédio |
| D6 | Ponte MQTT | *Sugestão:* **direto no Spring** (Spring Integration MQTT). O roadmap do app cita Node-RED; o firmware funciona com os dois, mas com Node-RED ele também teria de publicar as confirmações, a agenda e os comandos |
| D7 | Vínculo gaveta ↔ paciente | *Sugestão:* depois de configurar o Wi-Fi, o usuário digita os **4 caracteres** do nome da rede da gaveta (`RemedIoT-C4D5`). Uma gaveta por paciente |
| D8 | Wi-Fi | *Sugestão:* **portal da própria gaveta, guiado pelo app**. O app nativo com Bluetooth fica como evolução |
| D9 | Eventos que não são de dose | *Sugestão:* fora de horário, dose dupla, gaveta esquecida e gaveta offline viram **alerta para o cuidador** e aparecem no **histórico do paciente** |
| D10 | "Fui eu" e reabastecer | *Sugestão:* cuidador e paciente podem usar. Os botões ficam na **tela do paciente** e no **alerta** |
| D11 | Simulador | *Sugestão:* "Simular abertura" só aparece para pacientes **sem gaveta vinculada** |

---

## Fase G0 — Ler o contrato e planejar (sem código)

```
Leia o CLAUDE.md, o docs/gaveta/CONTRATO-MQTT.md, o código de docs/gaveta/backend-exemplo
e a seção "Decisões do grupo" do docs/gaveta/PROMPT-INTEGRACAO-GAVETA.md.

O hardware agora existe: a gaveta (ESP32) publica e recebe mensagens MQTT no EMQX Cloud
seguindo o CONTRATO-MQTT.md. Esta fase do projeto passa a INCLUIR a integração real com
MQTT. Atualize o CLAUDE.md: remova "não implementar MQTT nesta fase", troque o diagrama
(ESP32 --MQTT/TLS--> EMQX Cloud <--MQTT--> API Spring Boot, sem Node-RED, conforme a D6)
e registre as decisões D1 a D11.

As regras de dose (DoseStatusService, horários livres, pesos) NÃO mudam: o firmware foi
ajustado para seguir as regras do app (D1 a D5).

Ainda sem escrever código: me mostre um plano com as entidades novas, os serviços que
mudam (DoseEventService, DoseSchedulerService, MedicationService, AlertService,
DeviceEventController) e as telas novas. Aponte qualquer conflito entre o
contrato, as decisões e o código atual antes de seguir.
```

## Fase G1 — Modelo de dados

```
Implemente as entidades novas (identificadores em inglês, como no resto do projeto):

- Device: deviceId (String de 12 hexadecimais, único, vem do MAC), patient (OneToOne,
  nulo até o pareamento), online, lastSeenAt, firmwareVersion, timeSource (NTP/NENHUMA),
  offlineQueueSize, appliedAgendaVersion, lastStatusJson, pairedAt.
- DeviceEvent: eventId (String, CHAVE PRIMÁRIA: é a idempotência), device, type (String,
  para não quebrar com tipos novos do firmware), occurredAt (Instant), timeReliable
  (false quando o ts veio null), receivedAt, classification (nulo ou CAREGIVER para o
  "fui eu"), acknowledged (alerta já reconhecido), payload (JSON completo, TEXT).
- DeviceCommand: commandId (PK), device, type, status (PENDENTE, OK, ERRO, IGNORADO,
  DUPLICADO, EXPIRADO, SEM_RESPOSTA), message, sentAt, answeredAt.
- DeviceAgenda: device, version (Long, só cresce: ver Fase G4), configId, json enviado,
  status (ENVIADA, APLICADA, REJEITADA), errors.

Os horários dos remédios continuam livres (D2). A única regra nova (D3): um paciente com
gaveta vinculada pode ter no máximo 40 horários no total (soma dos MedicationSchedule de
todos os remédios). Valide no MedicationService com mensagem amigável e coloque a
constante MAX_DEVICE_SCHEDULES = 40 num só lugar. Teste JUnit desse limite.
```

## Fase G2 — Conexão MQTT com o EMQX Cloud

```
Adicione spring-boot-starter-integration e spring-integration-mqtt (Eclipse Paho v3) e
implemente a conexão com base em docs/gaveta/backend-exemplo/.../mqtt/
(MqttConfig, MqttGateway, MqttRoteador, Topicos), adaptando para o nosso projeto:

- Spring Boot 4 usa Jackson 3: o ObjectMapper é tools.jackson.databind (JsonMapper), as
  exceções são unchecked (JacksonException) e as anotações continuam em
  com.fasterxml.jackson.annotation. Java 17.
- Conexão TLS: ssl://HOST:8883, com usuário e senha vindos de variáveis de ambiente
  MQTT_HOST, MQTT_USUARIO, MQTT_SENHA (nada de credencial no repositório; atualize
  o .env.example). Use a credencial do BACKEND, não a dos dispositivos.
- Propriedade remediot.mqtt.enabled (padrão false em dev e nos testes). Com false, o app
  sobe sem broker e as publicações só vão para o log. Faça isso com uma interface
  DeviceMessagePublisher com duas implementações (MQTT e log), para os testes não
  precisarem de broker.
- Assine remediot/v1/+/eventos, remediot/v1/+/status e remediot/v1/+/ack com QoS 1, e
  cleanSession=false. clientIds diferentes para entrada e saída.
- Roteie pelo tópico para três serviços: DeviceEventIngestService (eventos),
  DeviceStatusService (status/LWT) e DeviceCommandService (acks).
- Nenhuma exceção pode sair do handler MQTT (no Paho ela derruba a conexão): capture,
  registre no log e siga.

Crie os records do contrato (envelope do evento, dados de dose, status, ack, agenda,
comando, confirmação). Os nomes dos campos no JSON são os do contrato (em português).
Pode reaproveitar os records do exemplo. Leia o campo "tipo" de forma que um tipo
desconhecido NÃO quebre a leitura: String com conversão segura para enum, ou
@JsonEnumDefaultValue com a feature correspondente do Jackson 3 ligada.
```

## Fase G3 — Eventos da gaveta → doses

```
Implemente o DeviceEventIngestService seguindo o CONTRATO-MQTT.md, seção 3 e seção 4:

1. Descarte (só log) o evento cujo deviceId é diferente do deviceId do tópico.
2. Grave o DeviceEvent. Se o eventId já existir, é reenvio: não processe de novo.
3. Publique a confirmação {"eventIds":[...]} em remediot/v1/{deviceId}/eventos/confirmacao
   (QoS 1, sem retained) SÓ DEPOIS de gravar (após o commit), e confirme também os
   duplicados. Se o banco falhar, não confirme: a gaveta reenvia sozinha.
4. Horário: occurredAt = ts (epoch UTC, em segundos). Se ts vier null, use o horário de
   chegada e timeReliable=false. Converta para horário local SEMPRE com
   ZoneId.of("America/Sao_Paulo"). Não use LocalDateTime.now() sem zona: no deploy o
   servidor roda em UTC. Revise os outros usos de LocalDateTime.now() no projeto com
   esse mesmo cuidado.
5. Gaveta ainda não vinculada a um paciente: grave e confirme, mas não mexa em doses.

Efeito de cada tipo (D1, D4). Os eventos de dose vêm UM POR REMÉDIO: dados.doseId é
"s" + id do MedicationSchedule (é o que a Fase G4 manda na agenda). A Dose é a do par
(scheduleId, dados.dataReferencia); chame antes o DoseGenerationService.ensureDoseForDate,
porque as doses são criadas sob demanda. O status vem pronto no evento (mesma regra do
DoseStatusService), não recalcule pelo horário de chegada:
- DOSE_ANTECIPADA, DOSE_CONFIRMADA -> TOMADA, registeredAt = horário do evento; um
  OpeningEvent(source=DEVICE).
- DOSE_ATRASADA -> ATRASADA, registeredAt = horário do evento; OpeningEvent(DEVICE);
  chame o AlertService como já acontece hoje quando a dose fica ATRASADA.
- DOSE_PERDIDA -> NAO_RETIRADA; chame o AlertService como hoje. Se dados.motivo =
  DISPOSITIVO_INDISPONIVEL, o alerta diz que a gaveta estava desligada ou sem hora (não
  sabemos se o paciente retirou).
- DOSE_EM_ATRASO -> a dose continua AGENDADA; só histórico por enquanto (é o momento em
  que a gaveta começa a apitar para peso 2 e 3). Deixe um comentário: se o grupo quiser,
  aqui dá para disparar o escalonamento "atrasado" mais cedo.
- ALARME_INICIADO, ALARME_SILENCIADO -> só histórico (útil para a tela "hora do remédio").
- ABERTURA_FORA_HORARIO, POSSIVEL_DOSE_DUPLA, GAVETA_ESQUECIDA_ABERTA, FALHA_RELOGIO ->
  alerta para o cuidador + histórico do paciente (D9).
- Os demais tipos -> só histórico.

Eventos podem chegar atrasados (a gaveta guarda os eventos quando está sem internet). O
status do evento sempre vale, mesmo que o DoseSchedulerService já tenha marcado
NAO_RETIRADA pelas 3 h. O DoseSchedulerService e o DoseStatusService continuam como estão.

O gancho triggerSoundAlarm do LoggingAlertService não precisa publicar nada: a gaveta já
apita sozinha no atraso para peso 2 e 3 (D5). Deixe só o log, citando isso.

Crie no AlertService um método para alertas da gaveta (fora de horário, dose dupla etc.),
separado do de dose. Nos textos, nunca afirme que o remédio foi "tomado": use
"compartimento aberto" e "dose registrada", como manda o CLAUDE.md.

Testes JUnit: cada tipo de evento, reenvio com o mesmo eventId (sem alerta duplicado),
deviceId diferente do tópico, ts null, evento DOSE_ATRASADA chegando depois de a dose
já estar NAO_RETIRADA, e 3 remédios no mesmo horário (3 eventos, 3 doses). Use os JSONs
reais do firmware em docs/gaveta/backend-exemplo/src/test/resources/firmware/.
```

## Fase G4 — Agenda: do app para a gaveta

```
Implemente o AgendaPublisher seguindo o CONTRATO-MQTT.md, seção 7:

- Monte a agenda com UMA ENTRADA POR MedicationSchedule do paciente (D2): doseId =
  "s" + schedule.id (ex.: "s17"), hora = schedule.time em "HH:mm", peso =
  medication.weight. Não mande diasSemana (sem ele, vale todos os dias, como no app).
  NUNCA coloque o nome do remédio nem do paciente (LGPD): não mande descricao.
- Valide com as mesmas regras do firmware antes de publicar (ver AgendaValidador no
  exemplo).
- version = o maior entre Instant.now().getEpochSecond() e (versão anterior + 1), como
  Long. NÃO comece em 1: a gaveta recusa versão menor que a que já tem (VERSAO_ANTIGA),
  então apagar o banco faria ela recusar toda agenda nova. Em segundos, nunca em
  milissegundos (a gaveta guarda 32 bits). configId = "cfg-" + 8 caracteres aleatórios.
- Tamanho: se o JSON passar de 7.500 bytes, recuse com erro claro e NÃO publique. A
  gaveta descarta em silêncio mensagem maior que o buffer dela (8 KB), sem ack.
- Publique em remediot/v1/{deviceId}/config/agenda com QoS 1 e RETAINED. A gaveta recebe
  na hora se estiver online, ou quando reconectar.
- Dispare a publicação depois do commit (@TransactionalEventListener AFTER_COMMIT) sempre
  que mudar: remédio criado, editado (inclusive peso e horários) ou excluído, gaveta
  vinculada. Ao desvincular, publique uma agenda vazia (doses: []), para a gaveta parar
  de apitar. Remédio excluído no meio do alarme: a gaveta para de tocar sozinha quando
  recebe a agenda nova sem ele, e essa dose não vira DOSE_PERDIDA.
- O ack chega com commandId = configId: atualize DeviceAgenda para APLICADA ou REJEITADA
  (guarde os erros). Ack de uma versão que não é a mais recente só vai para o histórico.

Testes: montagem da agenda (dois remédios no mesmo horário viram duas entradas), peso,
versão crescente, agenda vazia, recusa acima de 40 entradas.
```

## Fase G5 — Pareamento e status da gaveta

```
DeviceStatusService (tópico status, CONTRATO-MQTT.md seção 6):
- Status de um deviceId desconhecido: crie um Device sem paciente (gaveta aguardando
  pareamento).
- Atualize online, lastSeenAt, firmwareVersion, timeSource, offlineQueueSize,
  appliedAgendaVersion.
- online=false com motivo REINICIO_SOLICITADO ou TROCANDO_WIFI: offline esperado (foi um
  comando do próprio app). Mostre "reiniciando" ou "aguardando o Wi-Fi novo" e NÃO alerte.
- online=false com motivo CONEXAO_PERDIDA (LWT): marque offline. Se continuar offline por
  mais de 10 min, alerte o cuidador uma vez.

Endpoints (sempre com o PatientAccessGuard):
- POST /api/patients/{id}/device/pair {"code":"C4D5"} (D7): procure uma gaveta SEM
  paciente cujo deviceId termina com esses 4 caracteres e que mandou status nos últimos
  15 min. Se achar exatamente uma, vincule ao paciente e publique a agenda. Se não achar,
  ou achar mais de uma, devolva um erro claro.
- DELETE /api/patients/{id}/device: desvincula e publica a agenda vazia.
- GET /api/patients/{id}/device: online, última conexão, versão da agenda enviada x
  aplicada (sincronizada / aguardando / rejeitada + erros), alertas pendentes, alarme
  ativo, modo reabastecer.
```

## Fase G6 — Comandos para a gaveta

```
Implemente o DeviceCommandService seguindo o CONTRATO-MQTT.md, seções 8 e 9:

- Comando = {commandId: "cmd-" + 8 caracteres, tipo, emitidoEm, validoAte = agora + 120 s,
  parametros}, publicado em remediot/v1/{deviceId}/comando com QoS 1 e NUNCA retained.
  Grave como PENDENTE. O ack atualiza o status. Um @Scheduled marca SEM_RESPOSTA depois
  de 30 s.
- Endpoints em /api/patients/{id}/device/commands/...:
  - reabastecer (iniciar, com minutos opcional, e encerrar). O ack pode voltar ERRO com
    "DOSE_EM_ANDAMENTO" (dose pendente a menos de 30 min do horário, ou alarme tocando):
    mostre "Não dá para reabastecer perto do horário de uma dose".
  - alerts/{eventId}/fui-eu: marca o DeviceEvent como CAREGIVER e acknowledged e envia
    RECONHECER_ALERTA com o eventId. O mesmo endpoint serve de "ciente" para os outros
    alertas (D10).
  - localizar, silenciar alarme, pedir status, ressincronizar agenda.
  - trocar Wi-Fi (RESETAR_WIFI) e reiniciar: só o CUIDADOR.

Testes: ack de cada status, SEM_RESPOSTA, permissão por perfil.
```

## Fase G7 — Telas (PWA)

```
Implemente no frontend, mantendo o padrão de acessibilidade (fontes grandes, alto
contraste, alvos de toque grandes):

1. Assistente "Conectar gaveta" (D7, D8), na tela do paciente, para o cuidador:
   - Passo 1: ligar a gaveta na tomada; o LED pisca em branco.
   - Passo 2: no celular, conectar na rede "RemedIoT-XXXX" com a senha do portal (vem de
     VITE_GAVETA_SENHA_PORTAL, a mesma PORTAL_SENHA do firmware).
   - Passo 3: a página da gaveta abre sozinha (se não abrir, acessar 192.168.4.1). Escolher
     o Wi-Fi da casa e digitar a senha.
   - Passo 4: voltar para a internet normal e digitar os 4 caracteres do nome da rede.
   - Passo 5: chamar o pair e consultar o GET device a cada 3 s até aparecer online e
     "agenda sincronizada". Explique que a senha do Wi-Fi é digitada na página da gaveta,
     não no app.
2. Card "Gaveta" na tela do paciente (cuidador e paciente):
   - online/offline e última conexão;
   - agenda sincronizada / aguardando / rejeitada (com o motivo em linguagem simples);
   - botões: Reabastecer (10 min), Localizar, Silenciar alarme (só com alarme ativo);
     para o cuidador também Trocar Wi-Fi e Desvincular, com confirmação.
3. No MedicationFormPage, nada muda nos horários (continuam livres). Só mostre a mensagem
   do limite de 40 horários quando o backend recusar.
4. Alertas da gaveta: lista com botão "Fui eu" (fora de horário) e "Ciente" (os demais).
5. Histórico do paciente com os eventos da gaveta, em linguagem sem "tomou".
6. "Simular abertura da gaveta" só para pacientes sem gaveta vinculada (D11).
```

## Fase G8 — Testes, documentação e demo

```
Rode todos os testes e corrija o que falhar. Crie um teste de contrato que lê os JSONs de
docs/gaveta/backend-exemplo/src/test/resources/firmware/ (eventos, status, acks) com os
records em modo estrito. Assim, se o contrato mudar de um lado, o teste quebra.

Atualize:
- README: variáveis MQTT_HOST, MQTT_USUARIO, MQTT_SENHA, remediot.mqtt.enabled e o passo a passo "como conectar a
  gaveta".
- CHECKLIST-FINALIZACAO: os tópicos do roadmap passam a ser os do contrato
  (remediot/v1/{deviceId}/...). Tire o Node-RED se a D6 for confirmada.
- Diagrama de arquitetura em Mermaid.

Me diga como testar sem a gaveta: com o Online Test do console do EMQX (ou o MQTTX), publicando em
remediot/v1/{deviceId}/eventos e /status os JSONs de exemplo do contrato.
```

---

## Regras do contrato que não podem ser quebradas

- O backend **não** fala com a gaveta por HTTP. É só MQTT, nos tópicos `remediot/v1/{deviceId}/...`.
- `eventId` é único: reenvio não pode gerar registro nem alerta duplicado.
- A confirmação sai **depois** de gravar, e também para os duplicados.
- `ts` é epoch **UTC** em segundos. Pode vir `null`.
- A agenda vai **retained**, com versão sempre crescente (horário da alteração em segundos) e no máximo 7.500 bytes. Comandos **nunca** retained.
- Os tópicos não levam nome nem id do paciente, e a agenda não leva nome de remédio (LGPD).
- As regras de horário (30 min antes, 30 min depois, 3 h) e o limite de 40 entradas são iguais no app e no firmware. Mudar um lado exige mudar o outro.

Dúvidas sobre o comportamento da gaveta: falar com o Cesar. Para testar sem hardware, o `ROTEIRO-DE-TESTES.md` e o simulador do firmware mostram o que a gaveta manda em cada situação.
