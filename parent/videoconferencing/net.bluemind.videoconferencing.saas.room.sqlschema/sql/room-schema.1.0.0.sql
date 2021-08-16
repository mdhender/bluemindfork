CREATE TABLE t_videoconferencing_room (
    id BIGSERIAL PRIMARY KEY,
    identifier CHARACTER VARYING(255) UNIQUE NOT NULL,
    owner TEXT,
    title TEXT,
    created timestamp not null default now(),
    item_id bigint UNIQUE REFERENCES t_container_item(id) ON DELETE CASCADE
);
