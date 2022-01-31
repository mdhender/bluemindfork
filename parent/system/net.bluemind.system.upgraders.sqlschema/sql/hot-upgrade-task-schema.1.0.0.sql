CREATE TYPE enum_hot_upgrade_task_status AS ENUM (
    'SUCCESS',
    'FAILURE',
    'PLANNED'
);

CREATE TYPE enum_hot_upgrade_execution_mode AS ENUM (
    'DIRECT',
    'JOB'
);

CREATE TABLE IF NOT EXISTS t_hot_upgrade_task (
    id BIGSERIAL PRIMARY KEY,
    operation TEXT NOT NULL,
    parameters jsonb,
    status enum_hot_upgrade_task_status NOT NULL DEFAULT 'PLANNED'::enum_hot_upgrade_task_status,
    failure SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    execution_mode enum_hot_upgrade_execution_mode NOT NULL DEFAULT 'DIRECT'::enum_hot_upgrade_execution_mode,
    retry_count INTEGER NOT NULL DEFAULT 3,
    retry_delay INTEGER NOT NULL DEFAULT 0,
    report_failure BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX ON t_hot_upgrade_task (status, failure);