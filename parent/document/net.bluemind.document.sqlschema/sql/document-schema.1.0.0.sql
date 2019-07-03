
create table t_document (
  uid varchar(256),
  filename varchar(256),
  name varchar(256),
  description text,
  mime varchar(64),
  item_id int4 references t_container_item(id)
);


