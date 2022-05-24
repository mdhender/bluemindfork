create table IF NOT EXISTS t_webappdata (
  item_id bigint not null references t_container_item(id) on delete cascade primary key,
  key text not null,
  value text default null
);
