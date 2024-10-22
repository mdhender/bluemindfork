CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;

create type t_domain_routing as enum 
  ('internal', 'external', 'none');


create table t_domain_user (
	item_id 	  	bigint references t_container_item(id) on delete cascade primary key,
	login 		  	varchar(64) not null,
	password 	  	varchar(255),
	password_lastchange	timestamp,
	password_mustchange	boolean default false,
	password_neverexpires	boolean default false,

	archived	  	boolean default false,
	system		  	boolean default false,
	hidden		  	boolean default false,

	routing		  	t_domain_routing,
	server_id	  	text, -- uid
	properties hstore,
	password_algorithm 	varchar(64),
	mailbox_copy_guid text
);
CREATE INDEX idx_domain_user_login ON t_domain_user(login);

CREATE EXTENSION IF NOT EXISTS hstore;

create table t_settings_user (
  item_id   bigint references t_container_item(id) on delete cascade primary key,
  settings  hstore
);
CREATE INDEX idx_settings_user_item_id ON t_settings_user(item_id);
