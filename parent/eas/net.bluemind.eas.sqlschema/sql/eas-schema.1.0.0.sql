CREATE TABLE t_eas_heartbeat (
    device_uid text NOT NULL UNIQUE, -- auto create index
    heartbeat integer NOT NULL
);

CREATE TABLE t_eas_pending_reset (
  account text NOT NULL, -- user uid
  device text NOT NULL -- device identifier
);

CREATE TABLE t_eas_sent_item (
  device text NOT NULL, -- device identifier
  folder int4 NOT NULL,
  item text NOT NULL -- text because of calendar/todo/contact uid
);

CREATE TABLE t_eas_client_id (
  client_id text NOT NULL
);

CREATE TABLE t_eas_folder_sync (
  account text NOT NULL, -- user uid
  device text NOT NULL, -- device identifier
  versions hstore NOT NULL -- versions
);
