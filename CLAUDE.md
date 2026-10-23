# CLAUDE.md — RemedIoT

> Este arquivo é lido automaticamente pelo Claude Code ao abrir o projeto.
> Ele define o contexto, o stack e as regras. Mantenha-o atualizado conforme o projeto evolui.

## O que é o RemedIoT

Sistema de **monitoramento de adesão medicamentosa** para uso doméstico (pessoa física).
Um dispositivo físico (gaveta inteligente com ESP32) detecta a abertura do compartimento
do remédio no horário certo, e um **aplicativo** permite que **cuidadores** acompanhem
**pacientes** (tipicamente idosos) remotamente.

**Rigor científico — repita isto na documentação e na UI quando fizer sentido:**
o sistema detecta a *interação com o compartimento* (gaveta aberta), **não** prova de
ingestão do medicamento. Nunca escreva textos que afirmem que o remédio foi "tomado" como
fato biológico — use "registrado", "dose registrada" ou "compartimento aberto".

Projeto de TCC, orientado pelo Prof. Wallace Rodrigues de Santana. Alinhado aos ODS 3
(Saúde e Bem-Estar) e ODS 9 (Inovação e Infraestrutura).

## Escopo desta fase

O **aplicativo completo** (frontend + backend + banco) já existe. A fase atual é a
**integração com a gaveta real (ESP32) por MQTT**, cujo firmware está pronto (parte do
Cesar). Ver "Gaveta (ESP32) por MQTT". O WhatsApp continua numa fase futura — só o gancho
(ver "Integrações futuras").

**Alexa (em teste desde 29/09/2026):** os lembretes antes da dose já falam de verdade no
Echo via Voice Monkey (API v3) — ver "Lembretes e Alexa".

Fora de escopo: Grafana e InfluxDB (eram para o cenário clínico/pessoa jurídica, que foi
descartado). Não usar esses componentes.

## Stack (obrigatório — a documentação do TCC especifica)

- **Frontend:** React + Vite, configurado como **PWA** (instalável no celular). Estilização
  com foco em **acessibilidade** (fontes grandes, alto contraste, alvos de toque grandes).
- **Backend:** **Java 17 + Spring Boot** (Spring Web, Spring Data JPA, Spring Security).
  Build com **Maven**.
- **Banco:** **PostgreSQL**. (Nada de InfluxDB.)
- **Auth:** login por e-mail e senha, com JWT. Dois perfis: `CUIDADOR` e `PACIENTE`.

Idioma: **textos de interface em português (pt-BR)**; identificadores de código
(variáveis, classes, endpoints) em inglês.

## Arquitetura — regra de ouro

O app **nunca** fala com o hardware diretamente. Tudo passa pela **API Spring Boot**, que é
a fonte única da verdade:

```
[App React] <--REST--> [API Spring Boot] <--JPA--> [PostgreSQL]
                              ^
                              | MQTT/TLS 8883 (Spring Integration MQTT)
                              v
        [ESP32 gaveta] <--MQTT/TLS--> [EMQX Cloud]

[API Spring Boot] --> alarmes: Voice Monkey (Alexa) / WhatsApp (futuro) / push (futuro)
```

Sem Node-RED (D6): o próprio Spring assina e publica no broker. A `ponte-app/` do Cesar
(Node.js) fazia esse papel provisoriamente e **precisa ser desligada** quando o backend
passar a falar MQTT — as duas juntas publicam agendas com `doseId` diferentes.

## Modelo de dados

- **Cuidador** 1—N **Paciente** (um cuidador tem uma "carteira" de vários pacientes).
- **Paciente** pertence a um cuidador (`caregiver_id`). Tem: foto, nome, idade, endereço
  (com latitude/longitude para o link do Waze).
- **Paciente** 1—N **Medicamento**. Medicamento tem: nome, dose, **peso** (1, 2 ou 3) e um
  ou mais **horários** (HH:MM).
- **Medicamento** 1—N **Dose** (ocorrência diária de um horário). A Dose tem status:
  `AGENDADA`, `TOMADA` (registrada dentro da janela), `ATRASADA` (registrada fora da janela)
  ou `NAO_RETIRADA` (janela passou sem registro).
- **Evento de abertura**: registro (timestamp) vindo do sensor (ou do simulador) que muda o
  status de uma Dose.

**Vínculo cuidador↔paciente é crítico:** cuidador e paciente autorizados enxergam **os
mesmos dados** via `patient_id`. Ambos podem **adicionar e editar medicamentos**. Quando o
cuidador cadastra um remédio, ele aparece imediatamente para o paciente, e vice-versa.

## Pesos = regra de escalonamento de alertas

O peso não é só um rótulo; ele define a agressividade do alerta (lógica no backend):

- **Peso 1** (tranquilo), atrasado → apenas aviso no app.
- **Peso 2** (sério), atrasado → aviso no app + marcar para alarme sonoro + anúncio na Alexa.
- **Peso 3** (não pode atrasar), atrasado → tudo acima + alerta ao cuidador via WhatsApp +
  destaque vermelho no dashboard do cuidador.

Cores na UI: peso 1 = verde, peso 2 = amarelo, peso 3 = vermelho.

## Funcionalidades

### Área do CUIDADOR
- Cadastro/login por e-mail e senha.
- Carteira de pacientes em cards: foto, nome, idade, endereço, status resumido.
- Adicionar/editar paciente (incl. medicamentos e pesos).
- Botão "Traçar rota" que abre o Waze: `https://waze.com/ul?ll=LAT,LON&navigate=yes`.
- **Dashboard de assertividade**: visão de todos os pacientes — quem toma no horário, quem
  toma com atraso, quem não está tomando. % de adesão por paciente; peso 3 atrasado em
  destaque vermelho.
- Tela do paciente: resumo do dia, gráfico tomados vs pendentes, histórico do dia por horário.

### Área do PACIENTE
- Agenda do dia: medicamentos com horário, dose e peso (cor por peso).
- Botão grande "Ligar para o cuidador/família" (link `tel:`).
- Adicionar medicamento.
- Lembretes 10 min / 5 min / 30 s antes de cada dose (ver "Lembretes e Alexa").

## Simulador de hardware (para demonstração)

Cada Dose tem um botão/endpoint de teste "Simular abertura da gaveta" que registra um
Evento e recalcula o status (`TOMADA`/`ATRASADA` conforme a janela; `NAO_RETIRADA` quando a
janela passa sem evento). Permite demonstrar o fluxo na banca sem hardware. Com a gaveta,
o botão só aparece para pacientes **sem gaveta vinculada** (D11).

## Gaveta (ESP32) por MQTT

Documentação em `docs/gaveta/`:
- `CONTRATO-MQTT.md` — formato de **todas** as mensagens. **Não mude sem falar com o Cesar:
  o firmware depende dele.**
- `PROMPT-INTEGRACAO-GAVETA.md` — plano em fases G0 a G8 (um commit por fase).
- `backend-exemplo/` — referência em Spring Boot 3.5 / Jackson 2. **Adaptar** para o nosso
  Boot 4 / Jackson 3 (`tools.jackson.databind`), não copiar.
- `ROTEIRO-DE-TESTES.md` — o que a gaveta manda em cada situação.

Credenciais do broker só em variáveis de ambiente (`MQTT_HOST`, `MQTT_USUARIO`,
`MQTT_SENHA`), com o usuário do **backend** (`remediot-backend`), nunca o dos dispositivos.
Nada de host/senha no Git. Os zips do pacote ficam fora do Git (`*.zip` no `.gitignore`).

Regras do contrato que não podem ser quebradas:
- Backend ↔ gaveta só por MQTT, tópicos `remediot/v1/{deviceId}/...`.
- `eventId` é a chave de idempotência: reenvio não gera registro nem alerta duplicado.
- Confirmar o evento **depois** de gravar (após o commit), e confirmar também duplicados.
- `ts` é epoch **UTC em segundos** e pode vir `null`. Horário local sempre com
  `ZoneId.of("America/Sao_Paulo")` (o servidor de deploy roda em UTC).
- Agenda: **retained**, versão sempre crescente (epoch em segundos), máx. 7.500 bytes e
  40 entradas. Comandos **nunca** retained.
- LGPD: tópicos e agenda sem nome de paciente nem de remédio.

### Decisões do grupo

| # | Tema | Decisão |
|---|---|---|
| D1 | Janela da dose | Igual ao app: 30 min antes até 30 min depois = `TOMADA`; até 3 h = `ATRASADA`; 3 h sem abrir = `NAO_RETIRADA`. A gaveta manda o status pronto no evento |
| D2 | Horários | Livres por remédio, sem dias da semana. Cada `MedicationSchedule` = uma entrada da agenda (`doseId` = `"s"` + id) |
| D3 | Limite | 40 horários por paciente com gaveta (limite de memória da gaveta) |
| D4 | Uma abertura | Registra **todas** as doses no prazo; um evento por dose |
| D5 | Buzzer e peso | No horário só LED (voz é da Alexa); no atraso, peso 2 e 3 apitam na própria gaveta |
| D6 | Ponte MQTT | *Provisória:* direto no Spring (Spring Integration MQTT), sem Node-RED |
| D7 | Vínculo gaveta ↔ paciente | *Provisória:* 4 caracteres do nome da rede da gaveta (`RemedIoT-C4D5`); uma gaveta por paciente |
| D8 | Wi-Fi | *Provisória:* portal da própria gaveta, guiado pelo app |
| D9 | Eventos que não são de dose | *Provisória:* fora de horário, dose dupla, gaveta esquecida e offline viram alerta ao cuidador + histórico |
| D10 | "Fui eu" e reabastecer | *Provisória:* cuidador e paciente; botões na tela do paciente e no alerta |
| D11 | Simulador | *Provisória:* só para pacientes sem gaveta vinculada |

D6 a D11 seguem a sugestão do pacote e ainda precisam ser confirmadas com o grupo.

Em aberto com o Cesar: abertura >30 min antes ou >3 h depois vira `ATRASADA` no
`DoseStatusService` (simulador) mas `ABERTURA_FORA_HORARIO` na gaveta; e o bloqueio do modo
reabastecer perto da dose (hoje 30 min antes/depois no firmware).

## Integrações futuras — deixe os ganchos, não implemente agora

- `AlertService` — hoje só grava/loga; amanhã chama WhatsApp e push.

## Lembretes e Alexa

- `DoseReminderScheduler` (`@Scheduled`, a cada 5 s) dispara lembretes **10 min, 5 min e
  30 s** antes de cada dose `AGENDADA` (configurável em `REMINDER_OFFSETS`). Vale só o
  lembrete mais próximo do horário; os enviados ficam em `dose_reminder` (sem repetição).
- Regra pura em `ReminderPolicy` (testada em `ReminderPolicyTest`).
- `VoiceAnnouncer` → `VoiceMonkeyAnnouncer` (API v3 `/announce`). Sem `VOICEMONKEY_TOKEN`
  e `VOICEMONKEY_DEVICE`, só loga.
- Só os pacientes em `ALEXA_PATIENT_IDS` recebem voz (plano gratuito: 200 chamadas/mês).
  No teste atual: Maria Silva (id 1).
- `POST /api/alexa/test` faz o Echo falar uma frase de teste.

## Convenções

- Endpoints REST em inglês, plural: `/api/patients`, `/api/patients/{id}/medications`, etc.
- Camadas: `controller` → `service` → `repository` → `entity`. DTOs nas bordas.
- Testes: JUnit no backend para a lógica de status/pesos (é o coração do sistema).
- Commits pequenos e descritivos. Rode os testes antes de dizer que algo está pronto.
- Acessibilidade não é opcional: valide contraste e tamanho de fonte a cada tela.

## O que NÃO fazer

- Não usar Grafana nem InfluxDB.
- Não implementar de verdade o WhatsApp nesta fase — só o gancho.
- Não mudar o `docs/gaveta/CONTRATO-MQTT.md` sem combinar com o Cesar.
- Não versionar credenciais do broker (nem o `secrets.h` do firmware).
- Não afirmar "ingestão" — o sistema mede interação com o compartimento.
- Não deixar cuidador ver pacientes que não estão na carteira dele (isolamento por dono).
