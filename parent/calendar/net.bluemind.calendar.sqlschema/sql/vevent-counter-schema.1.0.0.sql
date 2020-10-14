CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;

create table t_calendar_vevent_counter (
  
  originator_cn text,
  originator_email text,
  vevent t_calendar_vevent
  
);

