
CREATE TYPE t_job_exit_status AS ENUM (
  'IN_PROGRESS', 'SUCCESS', 'COMPLETED_WITH_WARNINGS', 'FAILURE', 'INTERRUPTED'
);

CREATE TYPE t_entry_log_level AS ENUM (
  'PROGRESS', 'INFO', 'WARNING', 'ERROR'
);

CREATE TYPE t_job_plan_kind AS ENUM (
  'OPPORTUNISTIC', 'SCHEDULED', 'DISABLED'
);



CREATE TABLE t_job_execution (
  id  SERIAL,
  exec_group   VARCHAR(128) NOT NULL,
  domain_name  TEXT NOT NULL REFERENCES t_domain(name) ON DELETE CASCADE,
  job_id       VARCHAR(255) NOT NULL,
  exec_start   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  exec_end     TIMESTAMP WITHOUT TIME ZONE,
  status       t_job_exit_status NOT NULL,
  PRIMARY KEY (exec_group, domain_name, job_id),
  UNIQUE (id)
);

CREATE TABLE t_job_log_entry (
  execution_id  INTEGER NOT NULL REFERENCES t_job_execution(id) ON DELETE CASCADE,
  severity      t_entry_log_level NOT NULL DEFAULT 'INFO'::t_entry_log_level,
  stamp         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  locale        VARCHAR(2),
  content       TEXT NOT NULL
);

CREATE TABLE t_job_plan (
  domain_name       TEXT NOT NULL REFERENCES t_domain(name) ON DELETE CASCADE,
  job_id            VARCHAR(255) NOT NULL,
  kind              t_job_plan_kind NOT NULL DEFAULT 'OPPORTUNISTIC'::t_job_plan_kind,
  cron              VARCHAR(128),
  last_run          INTEGER REFERENCES t_job_execution(id) ON DELETE SET NULL,
  send_report       BOOLEAN DEFAULT FALSE,
  report_recipients TEXT DEFAULT '',
  report_level      t_entry_log_level NOT NULL DEFAULT 'INFO'::t_entry_log_level,
  PRIMARY KEY  (domain_name, job_id)
);


create index jle35_exec_id_idx on t_job_log_entry(execution_id);

