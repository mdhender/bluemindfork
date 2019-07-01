create table t_dummy_value (
	seen boolean default false,
	deleted boolean default false,
	item_id int4 references t_container_item(id)
);

create index dummyidx on t_dummy_value(item_id);
