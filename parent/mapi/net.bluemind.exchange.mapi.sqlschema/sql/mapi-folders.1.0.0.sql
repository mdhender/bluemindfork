CREATE TABLE if not exists t_mapi_folders (
	replica_guid varchar(36) not null,
	container_uid text not null,
	parent_container_uid text,
	display_name text,
	container_class text,
	primary key (replica_guid, container_uid)
);

CREATE INDEX IF NOT EXISTS t_mapi_folders_container_uid ON t_mapi_folders(container_uid);

CREATE TABLE if not exists t_mapi_raw_message (
	content jsonb,
	item_id bigint references t_container_item(id) on delete cascade
);
create index if not exists tmrm_item_id_fkey on t_mapi_raw_message(item_id);

