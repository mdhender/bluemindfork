
create table t_user_refreshtoken (
  system_identifier text not null,
  token text not null,
  expiry_time timestamp without time zone,
  user_uid text,
  PRIMARY KEY (system_identifier, user_uid)
);

create index t_refresh_token_system_identifier_idx on t_user_refreshtoken (system_identifier);

 