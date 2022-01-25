create table t_domain_role (
  item_id   bigint references t_container_item(id) on delete cascade,
  role      varchar(64) not null
);
CREATE INDEX idx_domain_role_item_id ON t_domain_role(item_id);
