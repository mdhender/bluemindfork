CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA pg_catalog;

CREATE TABLE IF NOT EXISTS t_domain_deferredaction (
  item_id                int4 REFERENCES t_container_item(id) on delete cascade PRIMARY KEY,
  action_id              TEXT NOT NULL,
  reference              TEXT NOT NULL,
  execution_date         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  configuration   hstore
);
CREATE INDEX IF NOT EXISTS idx_domain_deferredaction_action_date ON t_domain_deferredaction(action_id, execution_date);
CREATE INDEX IF NOT EXISTS idx_domain_deferredaction_reference ON t_domain_deferredaction(reference);
