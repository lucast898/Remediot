# CHECKLIST-FINALIZACAO.md — fechando o RemedIoT

Guia para transformar o app funcional num TCC entregável e bem defendido. Trabalhe de cima
para baixo; cada bloco pode virar um pedido ao Claude Code.

## 1. Qualidade do código

- [ ] Testes passando: `cd backend && ./mvnw test`. Priorize testes da lógica de status da
      Dose e das regras de peso (é o coração do sistema — a banca pode perguntar).
- [ ] Sem segredos no repositório (senha do banco, chaves) — use variáveis de ambiente e um
      `.env.example`. Confirme que o `.gitignore` cobre `.env`, `target/`, `node_modules/`.
- [ ] Tratamento de erros amigável na UI (falha de rede, credenciais inválidas).
- [ ] Código organizado em camadas (controller/service/repository) e componentes reutilizáveis
      no front.

Peça ao Claude Code: *"rode todos os testes, corrija o que falhar e me mostre a cobertura da
lógica de status e pesos."*

## 2. Acessibilidade (diferencial forte para este público)

- [ ] Contraste adequado (idosos): teste com uma ferramenta de contraste (WCAG AA).
- [ ] Fontes grandes e botões com área de toque generosa.
- [ ] Navegação por teclado e leitores de tela (labels/aria) nas ações principais.
- [ ] PWA instalável no celular (manifest + service worker) e ícone na tela inicial.

## 3. Documentação do TCC

- [ ] **README técnico** (já incluso) atualizado com prints das telas.
- [ ] **Diagrama de arquitetura** (app ↔ API ↔ banco; e a fase futura com ESP32/MQTT/Node-RED).
- [ ] **Modelo de dados** (diagrama de entidades) — o Claude Code pode gerar em Mermaid.
- [ ] **Documento de limitações**: registre explicitamente que "abertura ≠ ingestão" (isso
      demonstra rigor científico e antecipa a pergunta da banca).
- [ ] **Manual de uso** curto (cuidador e paciente), com a jornada principal.

Peça ao Claude Code: *"gere um diagrama Mermaid das entidades e um da arquitetura, e um
README técnico com seção de limitações."*

## 4. Deploy (opcional, mas impressiona)

Ter um link ao vivo evita depender do seu notebook na apresentação.

- [ ] **Frontend**: Vercel ou Netlify (deploy direto do repositório, gratuito).
- [ ] **Backend**: Render ou Railway (sobem Spring Boot + Postgres gerenciado).
- [ ] Configurar CORS e as URLs de API por ambiente (dev/prod).
- [ ] Testar o fluxo completo no ambiente publicado.

Alternativa simples: rodar tudo local e ter o **simulador** pronto para a demo.

## 5. Roteiro de demonstração na banca (usando o simulador)

Sequência sugerida (~3 min):

1. Login como **cuidador** → mostrar a carteira de pacientes e o dashboard de assertividade.
2. Abrir um paciente → mostrar a agenda do dia com os **pesos coloridos**.
3. Login (outra aba) como **paciente** → mostrar a agenda e o botão "Ligar para o cuidador".
4. Clicar **"Simular abertura da gaveta"** numa dose no horário → status vira **TOMADA** ✅.
5. Deixar uma dose de **peso 3** vencer sem abrir → mostrar o **destaque vermelho** e o alerta
   escalonado no dashboard do cuidador.
6. Fechar reforçando: "a abertura é indicador de interação, não prova de ingestão".

Ensaie com cronômetro. Tenha o seed de dados carregado antes de começar.

## 6. Roadmap de integração futura (para a seção "próximos passos")

Descreva no TCC, mesmo sem implementar:

- **Hardware**: ESP32 + sensor magnético (reed switch) + buzzer, gaveta impressa em 3D.
- **Comunicação**: ESP32 publica evento via **MQTT** (ex.: `remediot/paciente/17/comp/3`).
- **Ponte**: **Node-RED** assina o MQTT e faz `POST /api/devices/events` na API.
- **Alarmes 20/10/1 min**: agendador no backend dispara **Voice Monkey** (Alexa anuncia por
  voz na Echo do paciente via webhook) + notificação push + WhatsApp para peso 3.
- **Buzzer**: a API publica MQTT de volta (`.../buzzer: ligar`) e o ESP32 apita.
- **Evolução**: sensor de peso para aumentar a precisão do monitoramento.

## 7. Antes de entregar

- [ ] `git log` limpo, com commits por fase.
- [ ] Tag da versão de entrega: `git tag v1.0-tcc`.
- [ ] Backup do repositório (GitHub privado) e do documento escrito.
- [ ] Vídeo curto de demonstração (plano B caso a internet falhe na apresentação).
