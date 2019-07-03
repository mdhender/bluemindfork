CREATE TABLE t_container_sync (
	sync_token  TEXT NOT NULL,
	container_id int4 references t_container(id),
    last_sync timestamp,
    next_sync timestamp,
	UNIQUE(container_id)
);
