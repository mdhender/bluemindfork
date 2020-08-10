create type enum_mailflow_execution_mode as enum 
  ('CONTINUE', 'STOP_AFTER_EXECUTION');
  
create type enum_mailflow_routing as enum 
  ('INCOMING', 'OUTGOING', 'BOTH');

create table t_mailflow_assignment (
	domain_uid text not null,
	uid 	  text not null,
	description text,
	position int,
	action_identifier varchar(64) not null,
	execution_mode enum_mailflow_execution_mode,
	routing enum_mailflow_routing not null default 'OUTGOING',
	action_config hstore,
	assignment_group text,
	is_active boolean not null default true
);

create index i_mailflow_assignment on t_mailflow_assignment (uid, domain_uid);

create table t_mailflow_rule (
	domain_uid text not null,
	uid 	  text not null,
	id int4 not null,
	parent_id int4 not null,
	rule_identifier varchar(64),
	rule_config hstore
);

create index i_mailflow_rule on t_mailflow_rule (uid, domain_uid);
