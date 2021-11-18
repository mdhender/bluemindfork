create type enum_mailbox_type as enum 
  ('user', 'mailshare', 'resource', 'group');

create type enum_mailbox_routing as enum 
  ('internal', 'external', 'none');


create table t_mailbox (
	item_id 	  int4 references t_container_item(id) on delete cascade primary key,
	name 		  text not null,

	type		  enum_mailbox_type not null,
	system		  boolean default false,
	hidden		  boolean default false,
	archived	  boolean default false,
	routing		  enum_mailbox_routing not null,
	quota		  int4 null
);
CREATE INDEX idx_mailbox_name ON t_mailbox(name);

create table t_mailbox_email (
  item_id     int4 references t_container_item(id) on delete cascade NOT NULL,
  left_address     text,
  right_address     text,
  all_aliases boolean default false,
  is_default  boolean default false
);
CREATE INDEX idx_mailbox_email_item_id ON t_mailbox_email(item_id);
CREATE INDEX idx_mailbox_email_left_address ON t_mailbox_email(left_address);
CREATE INDEX idx_mailbox_email_right_address ON t_mailbox_email(right_address);
CREATE INDEX idx_mailbox_email_all_aliases ON t_mailbox_email(all_aliases);
CREATE INDEX idx_mailbox_email_full_address ON t_mailbox_email ((left_address||'@'||right_address));

CREATE TABLE t_domainmailfilter_rule (
    container_id		int4 references t_container(id),
    name text,
    criteria 	text NOT NULL,
    star 		boolean NOT NULL,
    mark_read 	boolean NOT NULL,
    delete_it 	boolean NOT NULL,
    forward 	character varying(255),
    forward_with_copy boolean DEFAULT false,
    deliver 	character varying(255),
    discard 	boolean DEFAULT false NOT NULL,
    row_idx 	integer DEFAULT 0 NOT NULL,
    active		boolean DEFAULT true,
    stop      boolean DEFAULT true
);
CREATE INDEX idx_domainfilter_rule_item_id ON t_domainmailfilter_rule(container_id);

CREATE TABLE t_mailfilter_rule (
    item_id		int4 references t_container_item(id) on delete cascade,
    name text,
    criteria 	text NOT NULL,
    star 		boolean NOT NULL,
    mark_read 	boolean NOT NULL,
    delete_it 	boolean NOT NULL,
    forward 	character varying(255),
    forward_with_copy boolean DEFAULT false,
    deliver 	character varying(255),
    discard 	boolean DEFAULT false NOT NULL,
    row_idx 	integer DEFAULT 0 NOT NULL,
    active		boolean DEFAULT true,
    stop      boolean DEFAULT true
);
CREATE INDEX idx_mailfilter_rule_item_id ON t_mailfilter_rule(item_id);

CREATE TABLE t_mailfilter_vacation (
    item_id		int4 references t_container_item(id) on delete cascade primary key,
    active  	boolean NOT NULL,
    start_date  timestamp,
    end_date    timestamp,
    subject		text,
    body_plain	text,
    body_html	text,
    vacation_marker	boolean default false
);

CREATE TABLE t_mailfilter_forwarding (
    item_id		int4 references t_container_item(id) on delete cascade primary key,
    active	 	boolean NOT NULL,
    local_copy  boolean NOT NULL,
    email		text
);
