-- PostgreSQL: execute depois do backup, em banco que ja tenha as tabelas originais.
-- Para banco vazio, use baseline.sql em vez deste arquivo.
BEGIN;
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS revisado boolean NOT NULL DEFAULT false;
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS revisao bigint NOT NULL DEFAULT 0;
ALTER TABLE resultados_ocr ADD COLUMN IF NOT EXISTS revisao bigint NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_sessoes_expira_em ON sessoes(expira_em);
COMMIT;
