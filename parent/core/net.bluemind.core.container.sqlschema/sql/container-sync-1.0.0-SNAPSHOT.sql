CREATE TABLE t_container_sync (
	sync_tokens hstore NOT NULL,
	container_id int4 references t_container(id),
    last_sync timestamp,
    next_sync timestamp,
    errors integer NOT NULL DEFAULT 0,
	UNIQUE(container_id)
);
