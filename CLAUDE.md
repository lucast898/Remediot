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

Construir **o aplicativo completo** (frontend + backend + banco), com **dados de sensor
simulados**. O hardware (ESP32), a integração com Alexa e o WhatsApp entram numa fase
futura — mas o código de agora deve deixar os **ganchos** prontos para eles (ver
"Integrações futuras").

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
                              | (fase futura)
        [ESP32 gaveta] --MQTT--> [Node-RED] --HTTP--> API
                              |
                              +--> alarmes: Voice Monkey (Alexa) / WhatsApp / push
```

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
- Placeholder para os alarmes sonoros 20/10/1 minutos antes.

## Simulador de hardware (para demonstração)

Como o ESP32 ainda não existe, cada Dose deve ter um botão/endpoint de teste
"Simular abertura da gaveta" que registra um Evento e recalcula o status
(`TOMADA`/`ATRASADA` conforme a janela; `NAO_RETIRADA` quando a janela passa sem evento).
Isso permite demonstrar o fluxo completo na banca sem hardware.

## Integrações futuras — deixe os ganchos, não implemente agora

Isole a lógica de eventos e alarmes atrás de **interfaces/serviços**, para plugar o real
depois sem reescrever telas:

- `DoseEventService` — hoje recebe eventos do simulador; amanhã recebe do endpoint que o
  Node-RED chama a partir do MQTT do ESP32.
- `AlertService` — hoje só grava/loga; amanhã chama Voice Monkey (Alexa), WhatsApp e push.
- Endpoint REST reservado, ex.: `POST /api/devices/events` (autenticado por chave do
  dispositivo) para o Node-RED entregar os eventos.
- Agendador dos alarmes 20/10/1 min: usar `@Scheduled` (ou Quartz) no backend, lendo os
  horários de cada paciente.

## Convenções

- Endpoints REST em inglês, plural: `/api/patients`, `/api/patients/{id}/medications`, etc.
- Camadas: `controller` → `service` → `repository` → `entity`. DTOs nas bordas.
- Testes: JUnit no backend para a lógica de status/pesos (é o coração do sistema).
- Commits pequenos e descritivos. Rode os testes antes de dizer que algo está pronto.
- Acessibilidade não é opcional: valide contraste e tamanho de fonte a cada tela.

## O que NÃO fazer

- Não usar Grafana nem InfluxDB.
- Não implementar de verdade Alexa/WhatsApp/MQTT nesta fase — só os ganchos.
- Não afirmar "ingestão" — o sistema mede interação com o compartimento.
- Não deixar cuidador ver pacientes que não estão na carteira dele (isolamento por dono).
