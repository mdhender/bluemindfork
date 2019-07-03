CREATE TABLE t_mapi_folders (
	replica_guid varchar(36) not null,
	container_uid text not null,
	parent_container_uid text,
	display_name text,
	container_class text,
	primary key (replica_guid, container_uid)
);

CREATE TABLE t_mapi_raw_message (
	content jsonb,
	item_id int4 references t_container_item(id)
);
create index tmrm_item_id_fkey on t_mapi_raw_message(item_id);

