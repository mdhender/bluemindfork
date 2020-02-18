drop table if exists t_mapi_fai;

CREATE TABLE t_mapi_fai (
	folder_id text,
	fai jsonb,
	item_id int4 references t_container_item(id) on delete cascade
);

create index if not exists tmf_item_id_fkey on t_mapi_fai(item_id);

