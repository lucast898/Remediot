# RemedIoT

Sistema de monitoramento de adesão medicamentosa (TCC). Aplicativo web (PWA) para
cuidadores acompanharem o uso de medicamentos por pacientes, com dispositivo IoT (gaveta
inteligente ESP32) na fase futura.

> **Nota científica:** o sistema detecta a *abertura do compartimento* (interação), não a
> ingestão do medicamento.

## Stack

| Camada     | Tecnologia                                   |
|------------|----------------------------------------------|
| Frontend   | React + Vite (PWA)                           |
| Backend    | Java 17 + Spring Boot (Maven)               |
| Banco      | PostgreSQL                                    |
| Auth       | E-mail e senha (JWT), perfis Cuidador/Paciente |

## Estrutura do projeto

```
remediot/
├── CLAUDE.md                  # contexto e regras (lido pelo Claude Code)
├── README.md                  # este arquivo
├── PROMPT-INICIAL.md          # plano de construção em fases
├── CHECKLIST-FINALIZACAO.md   # guia para fechar o TCC
├── frontend/                  # React + Vite (PWA)
└── backend/                   # Spring Boot (Java 17 + Maven)
```

## Pré-requisitos

Instale (uma vez):

- **Node.js 18+** — https://nodejs.org (você já tem)
- **JDK 17+** — Temurin/Adoptium: https://adoptium.net
- **Maven 3.9+** — https://maven.apache.org (ou use o wrapper `./mvnw` que o Spring gera)
- **PostgreSQL 15+** — https://www.postgresql.org/download/
  - Alternativa mais simples: rodar via Docker (comando abaixo).

Verifique as versões:

```bash
node -v
java -version
mvn -v      # ou ./backend/mvnw -v
```

## Banco de dados (Docker — opção mais fácil)

```bash
docker run --name remediot-db -e POSTGRES_PASSWORD=remediot \
  -e POSTGRES_DB=remediot -p 5432:5432 -d postgres:16
```

No `backend/src/main/resources/application.properties` aponte para:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/remediot
spring.datasource.username=postgres
spring.datasource.password=remediot
spring.jpa.hibernate.ddl-auto=update
```

## Rodando o projeto

**Backend** (porta 8080):

```bash
cd backend
./mvnw spring-boot:run
```

**Frontend** (porta 5173):

```bash
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173.

## Como construir com o Claude Code

1. Crie a pasta `remediot/` e coloque dentro os quatro `.md` deste pacote.
2. Abra o VS Code nessa pasta e inicie o Claude Code (`claude` no terminal integrado).
3. Ele lê o `CLAUDE.md` sozinho. Se sugerir, rode `/init`.
4. Abra o `PROMPT-INICIAL.md` e cole os blocos, **fase por fase**.
5. Ao fim de cada fase que funcionar, faça `git commit` para ter pontos de retorno.

## Roadmap

- [x] Definição de escopo e arquitetura
- [ ] App completo com simulador de sensor (fase atual)
- [ ] Impressão 3D e montagem da gaveta (ESP32 + sensor magnético + buzzer)
- [ ] Integração ESP32 → MQTT → Node-RED → API
- [ ] Alarmes via Alexa (Voice Monkey) e WhatsApp
- [ ] Evolução: sensor de peso para maior precisão
