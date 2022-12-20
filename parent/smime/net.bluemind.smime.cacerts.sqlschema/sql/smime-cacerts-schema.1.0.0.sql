create table IF NOT EXISTS t_smime_cacerts (
  item_id bigint not null references t_container_item(id) on delete cascade primary key,
  cert text not null
);
