
create table t_document (
  uid varchar(256),
  filename varchar(256),
  name varchar(256),
  description text,
  mime varchar(64),
  item_id bigint references t_container_item(id) on delete cascade
);


