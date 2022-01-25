drop table if exists t_mapi_fai;

CREATE TABLE t_mapi_fai (
	folder_id text,
	fai jsonb,
	item_id bigint references t_container_item(id) on delete cascade
);
CREATE INDEX IF NOT EXISTS t_mapi_fai_folder_id_idx ON t_mapi_fai(folder_id);
create index if not exists tmf_item_id_fkey on t_mapi_fai(item_id);
