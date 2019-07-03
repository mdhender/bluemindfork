create table t_domain_role (
  item_id   int4 references t_container_item(id),
  role      varchar(64) not null
);
CREATE INDEX idx_domain_role_item_id ON t_domain_role(item_id);
