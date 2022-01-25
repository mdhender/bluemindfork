create table t_tagvalue (
	label	text,
	color	varchar(6),
	item_id bigint references t_container_item(id) on delete cascade
);
CREATE INDEX idx_tagvalue_item_id_fkey ON t_tagvalue(item_id);

create table t_tagref (
	tagged_item_uid text NOT NULL,
	tagged_container_uid text NOT NULL,
	tagged_container_type text NOT NULL,
	ref_container_uid text NOT NULL,
	ref_item_uid      text NOT NULL
);
create index idx_tagref_tagged_item_uid on t_tagref (tagged_item_uid);
create index idx_tagref_tagged_container_uid_tagged_item_uid on t_tagref (tagged_container_uid, tagged_item_uid);