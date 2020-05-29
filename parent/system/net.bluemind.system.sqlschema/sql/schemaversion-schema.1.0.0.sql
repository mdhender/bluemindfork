create type enum_upgrader_phase as enum 
  ('SCHEMA_UPGRADE', 'POST_SCHEMA_UPGRADE');
  
create type enum_database_name as enum 
  ('DIRECTORY', 'SHARD', 'ALL');

create table t_bm_upgraders (
	server text,
	phase enum_upgrader_phase,
	database_name enum_database_name,
	upgrader_id text,
	success boolean,
	PRIMARY KEY (server, database_name, upgrader_id)
);

create table t_component_version (
    component TEXT primary key,
    version   TEXT not null
);