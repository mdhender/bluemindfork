CREATE TABLE t_container_hierarchy (
	name text not null,
	container_type text not null,
	container_uid text not null,
	item_id integer not null references t_container_item(id)
);
CREATE INDEX idx_tch_item_id ON t_container_hierarchy(item_id);
CREATE INDEX idx_tch_type  ON t_container_hierarchy USING gin(container_type);
