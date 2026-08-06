# PROMPT-INICIAL.md — como iniciar o RemedIoT no Claude Code

Este é o plano de construção. Abra o Claude Code **na pasta do projeto** (onde está o
`CLAUDE.md`) e cole os blocos abaixo. Você pode colar **fase por fase** (recomendado — mais
fácil de revisar e corrigir) ou tudo de uma vez.

> Dica: antes de começar, garanta que o `CLAUDE.md` está na raiz — o Claude Code o lê
> automaticamente e vai seguir o stack e as regras dali. Rode `/init` se ele sugerir.

---

## Fase 0 — Estrutura do monorepo

```
Leia o CLAUDE.md deste projeto. Crie a estrutura de um monorepo com duas pastas:
`frontend/` (React + Vite, PWA) e `backend/` (Java 17 + Spring Boot + Maven).
Configure o backend com Spring Web, Spring Data JPA, Spring Security e o driver do
PostgreSQL. Configure o frontend com Vite, React Router e o plugin de PWA.
Crie um .gitignore adequado para Java/Maven e Node. Inicialize o git.
Ainda NÃO implemente funcionalidades — só o esqueleto que compila/roda.
Ao terminar, me diga os comandos para rodar cada lado.
```

## Fase 1 — Modelo de dados e autenticação (backend)

```
No backend, implemente as entidades JPA conforme o modelo do CLAUDE.md:
Caregiver, Patient, Medication (com campo weight 1/2/3), MedicationSchedule (horários),
Dose (com status AGENDADA/TOMADA/ATRASADA/NAO_RETIRADA) e OpeningEvent.
Respeite os relacionamentos (Caregiver 1—N Patient, Patient 1—N Medication, etc.).
Implemente autenticação por e-mail e senha com JWT e os perfis CUIDADOR e PACIENTE.
Garanta o isolamento: um cuidador só acessa pacientes da carteira dele.
Escreva testes JUnit para o cálculo de status da Dose (dentro/fora da janela).
```

## Fase 2 — Endpoints REST e simulador (backend)

```
Implemente os endpoints REST (inglês, plural): /api/auth, /api/patients,
/api/patients/{id}/medications, /api/patients/{id}/doses, /api/dashboard.
Implemente a lógica de pesos/escalonamento no AlertService (por enquanto só grava/loga).
Implemente o simulador: POST /api/doses/{id}/simulate-opening, que cria um OpeningEvent e
recalcula o status da Dose. Deixe reservado POST /api/devices/events (gancho do hardware).
Adicione um agendador (@Scheduled) que marca doses como NAO_RETIRADA quando a janela passa.
Escreva testes para as regras de peso.
```

## Fase 3 — Frontend: base, login e navegação por perfil

```
No frontend, crie o tema acessível (fontes grandes, alto contraste, azul suave; cores de
status verde/amarelo/vermelho). Implemente login/cadastro por e-mail e senha consumindo a
API. Após o login, direcione para a área do CUIDADOR ou do PACIENTE conforme o perfil.
Isole as chamadas de API numa camada de serviço (services/api.js) para facilitar trocas.
```

## Fase 4 — Área do Cuidador

```
Implemente a área do cuidador: carteira de pacientes em cards (foto, nome, idade, endereço,
status resumido); adicionar/editar paciente e medicamentos (com peso); botão "Traçar rota"
que abre o Waze (https://waze.com/ul?ll=LAT,LON&navigate=yes); e a tela de um paciente com
resumo do dia, gráfico tomados vs pendentes e histórico do dia por horário.
```

## Fase 5 — Área do Paciente

```
Implemente a área do paciente: agenda do dia (medicamentos com horário, dose e peso, com
cor por peso); botão grande "Ligar para o cuidador/família" (link tel:); adicionar
medicamento; e um placeholder visível para os alarmes 20/10/1 min antes.
Adicione o botão "Simular abertura da gaveta" em cada dose, chamando o endpoint do simulador.
```

## Fase 6 — Dashboard de assertividade

```
Implemente o dashboard do cuidador: % de adesão por paciente, separando quem toma no horário,
quem toma com atraso e quem não está tomando. Destaque em vermelho pacientes com remédio de
peso 3 atrasado. Use uma biblioteca de gráficos leve (ex: Recharts).
```

## Fase 7 — Dados de exemplo e polimento

```
Crie um seed com 3–4 pacientes já preenchidos (fotos placeholder, medicamentos com pesos
variados e horários ao longo do dia) para as telas não nascerem vazias. Faça um passe de
acessibilidade e responsividade. Confirme que o PWA instala no celular.
```

---

## Como pedir correções ao Claude Code

- Seja específico: "na tela X, o botão Y deveria fazer Z" funciona melhor que "arruma isso".
- Peça para **rodar os testes** depois de mudanças no backend.
- Se algo quebrar, cole a mensagem de erro inteira — ele lê stack traces bem.
- Use `git commit` ao fim de cada fase que funcionar, pra ter pontos de retorno seguros.
