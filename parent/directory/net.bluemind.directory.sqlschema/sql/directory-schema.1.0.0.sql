CREATE EXTENSION IF NOT EXISTS ltree WITH schema pg_catalog;

CREATE EXTENSION IF NOT EXISTS unaccent WITH schema pg_catalog;

CREATE EXTENSION IF NOT EXISTS btree_gin WITH schema pg_catalog;

create type t_directory_entry_account_type as enum ('FULL', 'SIMPLE');

create table t_directory_entry (
	kind text NOT NULL,
	account_type t_directory_entry_account_type default NULL,
	entry_uid text NOT NULL,
	displayname text NOT NULL,
	email		text,
	flag_hidden	boolean default false,
	flag_system	boolean default false,
	flag_archived	boolean default false,
	orgunit_item_id int4 references t_container_item(id) on delete cascade,
	datalocation text,
	item_id int4 references t_container_item(id) on delete cascade UNIQUE
);

create index idx_t_directory_entry_uid on t_directory_entry (entry_uid);

create index idx_t_directory_entry_orgunit_item_id_idx on t_directory_entry (orgunit_item_id);

create index idx_t_directory_entry_displayname on t_directory_entry (displayname);

create index idx_t_directory_entry_kind on t_directory_entry using gin(kind);
