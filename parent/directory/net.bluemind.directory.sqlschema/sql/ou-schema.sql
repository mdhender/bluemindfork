

create table t_directory_ou (
	item_id bigint references t_container_item(id) on delete cascade UNIQUE,
	parent_item_id bigint references t_container_item(id) on delete cascade,
	name text NOT NULL
);

create index idx_t_directory_ou_parent_item_id ON t_directory_ou(parent_item_id);

create table t_directory_ou_administrator (
	ou_id bigint references t_directory_ou(item_id),
	administrator_item_id bigint references t_container_item(id) on delete cascade,
	roles text[],
	UNIQUE (ou_id,administrator_item_id)
);
create index i_t_directory_ou_administrator_ou_id on t_directory_ou_administrator (ou_id);


