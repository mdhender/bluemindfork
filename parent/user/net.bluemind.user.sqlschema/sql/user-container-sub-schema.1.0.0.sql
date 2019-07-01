CREATE TABLE t_container_sub (
	container_uid text,
	container_type text,
	user_id int4 references t_domain_user(item_id),
	offline_sync boolean default true,
	PRIMARY KEY(container_uid, user_id)
);
CREATE INDEX idx_container_sub_container_uid ON t_container_sub(container_uid);
CREATE INDEX idx_container_sub_user_id ON t_container_sub(user_id);
