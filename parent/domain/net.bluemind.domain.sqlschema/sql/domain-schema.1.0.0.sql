CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;
create table t_settings_domain (
  item_id   int4 references t_container_item(id) on delete cascade primary key,
  settings  hstore
);

CREATE TABLE t_domain (
	item_id   int4 references t_container_item(id) on delete cascade primary key,
	label	text NOT NULL,
	name	text NOT NULL,
	description text     ,
	global	boolean NOT NULL,
	aliases	text[] NOT NULL,
	properties hstore,
	UNIQUE(name)
);

