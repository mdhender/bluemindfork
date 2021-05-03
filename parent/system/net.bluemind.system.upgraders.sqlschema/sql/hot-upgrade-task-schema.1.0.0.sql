CREATE TYPE enum_hot_upgrade_task_status AS ENUM (
    'SUCCESS',
    'FAILURE',
    'PLANNED'
);

CREATE TABLE IF NOT EXISTS t_hot_upgrade_task (
    id SERIAL PRIMARY KEY,
    operation TEXT NOT NULL,
    parameters jsonb,
    status enum_hot_upgrade_task_status NOT NULL DEFAULT 'PLANNED'::enum_hot_upgrade_task_status,
    failure SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX ON t_hot_upgrade_task (status, failure);