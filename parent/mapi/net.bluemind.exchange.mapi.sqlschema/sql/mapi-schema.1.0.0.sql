drop table if exists t_mapi_replica;

CREATE TABLE t_mapi_replica (
	local_replica_guid varchar(36),
	mailbox_guid varchar(36),
	logon_replica_guid varchar(36),
	message_objects_guid varchar(36),
	mailbox_uid text primary key
);
