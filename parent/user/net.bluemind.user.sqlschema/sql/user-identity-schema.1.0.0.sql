
create table t_user_mailidentity (
	id		  varchar(64) not null,
	mbox_uid 	  varchar(64),
	user_id 	  bigint references t_domain_user(item_id),
	name 		  varchar(64) not null,
	format		  enum_identity_format not null,
	signature	  TEXT not null,
	displayname	  TEXT not null,
	sent_folder	  TEXT not null,
	email		  TEXT not null,
	is_default	  boolean not null default false
);

CREATE INDEX idx_user_mailidentity_item_id ON t_user_mailidentity(user_id);
