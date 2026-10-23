-- Esquema mínimo do exemplo (H2 em modo PostgreSQL; roda igual no PostgreSQL)

-- Eventos do dispositivo. event_id como chave primária = idempotência:
-- o mesmo evento chegando duas vezes esbarra na chave e é ignorado.
CREATE TABLE IF NOT EXISTS evento (
  event_id        VARCHAR(40) PRIMARY KEY,
  device_id       VARCHAR(12) NOT NULL,
  tipo            VARCHAR(40) NOT NULL,
  ocorrido_em     TIMESTAMP WITH TIME ZONE NOT NULL,
  hora_confiavel  BOOLEAN NOT NULL,          -- false: o ESP não tinha hora (usamos a chegada)
  recebido_em     TIMESTAMP WITH TIME ZONE NOT NULL,
  classificacao   VARCHAR(30),               -- ex.: CUIDADOR ("fui eu")
  payload         TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_evento_device_tempo ON evento (device_id, ocorrido_em);

-- Comandos enviados e o ack de cada um
CREATE TABLE IF NOT EXISTS comando (
  command_id      VARCHAR(40) PRIMARY KEY,
  device_id       VARCHAR(12) NOT NULL,
  tipo            VARCHAR(40) NOT NULL,
  status          VARCHAR(20) NOT NULL,      -- PENDENTE, OK, ERRO, ..., SEM_RESPOSTA
  mensagem        VARCHAR(200),
  enviado_em      TIMESTAMP WITH TIME ZONE NOT NULL,
  respondido_em   TIMESTAMP WITH TIME ZONE
);

-- Última agenda de cada dispositivo (a versão só cresce)
CREATE TABLE IF NOT EXISTS agenda (
  device_id       VARCHAR(12) PRIMARY KEY,
  versao          BIGINT NOT NULL,          -- epoch em segundos da alteração
  config_id       VARCHAR(40) NOT NULL,
  json            TEXT NOT NULL,
  status          VARCHAR(20) NOT NULL       -- ENVIADA, APLICADA, REJEITADA
);

-- Último status/heartbeat (ou LWT) de cada dispositivo
CREATE TABLE IF NOT EXISTS dispositivo_status (
  device_id       VARCHAR(12) PRIMARY KEY,
  online          BOOLEAN NOT NULL,
  payload         TEXT NOT NULL,
  atualizado_em   TIMESTAMP WITH TIME ZONE NOT NULL
);
