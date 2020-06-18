create type enum_upgrader_phase as enum 
  ('SCHEMA_UPGRADE', 'POST_SCHEMA_UPGRADE');
  
create table t_component_version (
    component TEXT primary key,
    version   TEXT not null
);