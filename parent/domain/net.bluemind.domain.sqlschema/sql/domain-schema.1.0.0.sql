CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;
create table t_settings_domain (
  item_id   bigint references t_container_item(id) on delete cascade primary key,
  settings  hstore
);

CREATE TABLE t_domain (
	item_id   bigint references t_container_item(id) on delete cascade primary key,
	label	text NOT NULL,
	name	text NOT NULL,
	description text     ,
	global	boolean NOT NULL,
	aliases	text[] NOT NULL,
	default_alias text NOT NULL CHECK (default_alias = name OR default_alias = ANY(aliases)),
	properties hstore,
	UNIQUE(name)
);

CREATE INDEX ON t_domain(default_alias);
