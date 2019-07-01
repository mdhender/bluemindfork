

create table t_directory_ou (
	item_id int4 references t_container_item(id) UNIQUE,
	parent_item_id int4 references t_container_item(id),
	name text NOT NULL
);

create index idx_t_directory_ou_parent_item_id ON t_directory_ou(parent_item_id);

create table t_directory_ou_administrator (
	ou_id int4 references t_directory_ou(item_id),
	administrator_item_id int4 references t_container_item(id),
	roles text[],
	UNIQUE (ou_id,administrator_item_id)
);
create index i_t_directory_ou_administrator_ou_id on t_directory_ou_administrator (ou_id);


