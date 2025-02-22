create type t_calendarview_type as enum 
  ('DAY', 'WEEK', 'MONTH', 'LIST');
  
create table t_calendarview (
  item_id bigint references t_container_item(id) on delete cascade,
  id  serial primary key,
  label text,
  type  t_calendarview_type,
  calendars text[],
  is_default boolean default false
);

create index idx_t_calendarview_item_id on t_calendarview (item_id);