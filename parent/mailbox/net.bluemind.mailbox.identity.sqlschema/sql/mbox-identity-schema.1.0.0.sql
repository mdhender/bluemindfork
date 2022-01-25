
create type enum_identity_format as enum 
  ('HTML', 'PLAIN');

create table t_mailbox_identity (
	id		  varchar(64) not null,
	mbox_id 	  bigint references t_mailbox(item_id),
	name 		  varchar(64) not null,

	format		  enum_identity_format not null,
	signature	  TEXT not null,
	displayname	  TEXT not null,
	sent_folder	  TEXT null,
	email		  TEXT not null,
	is_default	  boolean default false
);

