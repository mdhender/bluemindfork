CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;

create table t_user_account (
	item_id 	  int4 references t_container_item(id) on delete cascade,
	login 		  varchar(64) not null,
	credentials 	  text,
	system		  varchar(255),
	properties hstore,
	PRIMARY KEY (item_id, system)
);
CREATE INDEX idx_accounts_user_item_id ON t_user_account(item_id);
CREATE INDEX idx_accounts_user_item_id_system ON t_user_account(item_id, system);
