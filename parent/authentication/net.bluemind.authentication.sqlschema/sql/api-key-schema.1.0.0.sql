
create table t_api_key (
  sid text not null,
  displayname text not null,
  subject text not null,
  domain_uid text
);

create index t_api_key_sid_idx on t_api_key (sid);