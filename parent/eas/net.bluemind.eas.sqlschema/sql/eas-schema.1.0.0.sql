CREATE TABLE t_eas_heartbeat (
    device_uid text NOT NULL UNIQUE, -- auto create index
    heartbeat integer NOT NULL
);

CREATE TABLE t_eas_pending_reset (
  account text NOT NULL, -- user uid
  device text NOT NULL -- device identifier
);

CREATE TABLE t_eas_client_id (
  client_id text NOT NULL
);

CREATE INDEX t_eas_client_id_client_id ON t_eas_client_id (client_id);

CREATE TABLE t_eas_folder_sync (
  account text NOT NULL, -- user uid
  device text NOT NULL, -- device identifier
  versions hstore NOT NULL, -- versions
  primary key (account, device)
);
