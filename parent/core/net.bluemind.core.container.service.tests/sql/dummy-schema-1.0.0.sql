create table t_dummy_value (
	seen boolean default false,
	deleted boolean default false,
	item_id bigint references t_container_item(id) on delete cascade
);

create index dummyidx on t_dummy_value(item_id);
