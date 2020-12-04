create table IF NOT EXISTS t_message_body (
	guid bytea primary key,
	subject text,
	structure jsonb,
	headers jsonb,
	recipients jsonb,
	message_id text,
	references_header text[],
	date_header timestamp not null,
	size integer not null,
	preview varchar(160),
	body_version integer not null default 0,
	created TIMESTAMP not null default now()
);

create table IF NOT EXISTS t_mailbox_replica (
	short_name text not null,
	parent_uid text,
	name text not null,
	last_uid int8 not null,
	highest_mod_seq int8 not null,
	recent_uid int8 not null,
	recent_time timestamp not null,
	last_append_date timestamp not null,
	pop3_last_login timestamp not null,
	uid_validity int8 not null,
	acls jsonb not null,
	options varchar(32) not null,
	sync_crc int8 not null,
	quotaroot text,
	unique_id text not null,
	container_id int4 not null references t_container(id) ON UPDATE CASCADE  on delete cascade,
	item_id int4 references t_container_item(id) on delete cascade UNIQUE
);

create index IF NOT EXISTS i_mailbox_replica on t_mailbox_replica (item_id);
create index IF NOT EXISTS i_mailbox_replica_names on t_mailbox_replica (container_id, name);

create table IF NOT EXISTS t_mailbox_record (
	message_body_guid bytea not null, 
	imap_uid int8 not null,
	mod_seq int8 not null,
	last_updated timestamp not null,
	internal_date timestamp not null,
	system_flags int4 not null,
	other_flags text[],
	container_id int4 not null references t_container(id) ON UPDATE CASCADE  on delete cascade,
	item_id int4 references t_container_item(id) ON UPDATE CASCADE  on delete cascade
);
create index IF NOT EXISTS t_mailbox_record_imap_uid ON t_mailbox_record (imap_uid);
create index IF NOT EXISTS i_mailbox_record_cid_imap_uid ON t_mailbox_record (container_id, imap_uid);
create index IF NOT EXISTS t_mailbox_record_body_guid ON t_mailbox_record (message_body_guid);
create index IF NOT EXISTS i_mailbox_record on t_mailbox_record (item_id);

create table IF NOT EXISTS t_seen_overlay (
	user_id text not null,
	unique_id text not null,
	last_read int8 not null,
	last_uid int8 not null,
	last_change int8 not null,
	seen_uids text not null,
	unique (user_id, unique_id)
);
create index IF NOT EXISTS t_seenoverlay_by_user_id ON t_seen_overlay (user_id);
create index IF NOT EXISTS t_seenoverlay_by_both ON t_seen_overlay (user_id, unique_id);

create table IF NOT EXISTS t_mailbox_sub (
	user_id text not null,
	mbox text not null
);
create index IF NOT EXISTS t_mailboxsub_by_user_id ON t_mailbox_sub (user_id);
alter table t_mailbox_sub drop constraint if exists ct_unique_mbox_subs;
alter table t_mailbox_sub add constraint ct_unique_mbox_subs unique (user_id, mbox);

create table IF NOT EXISTS t_quota_root (
	root text not null primary key,
	limit_kb int not null
);

create table IF NOT EXISTS t_mailbox_annotation (
	mbox text not null,
	user_id text not null, -- empty string when global
	entry text not null,
	value text,
	unique (mbox, user_id, entry)
);

create table IF NOT EXISTS t_sieve_script (
	user_id text not null,
	filename text not null,
	last_update int8 not null,
	active boolean not null,
	primary key (user_id, filename)
);
create index IF NOT EXISTS t_sieve_by_user_id ON t_sieve_script (user_id);

create table IF NOT EXISTS t_subtree_uid (
	domain_uid	text not null,
	mailbox_uid text not null,
	mailbox_name text not null,
	namespace text not null,
	unique (domain_uid, mailbox_uid)
);

create index IF NOT EXISTS subtree_uid_idx ON t_subtree_uid (domain_uid, mailbox_name);
