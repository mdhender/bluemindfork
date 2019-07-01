CREATE TABLE t_owner_subscription (
	container_type text not null,
	container_uid text not null,
	offline_sync boolean not null,
	owner text not null,
	default_container boolean not null,
	name text not null,
	item_id integer not null references t_container_item(id)
);
CREATE INDEX idx_tos_item_id ON t_owner_subscription(item_id);
