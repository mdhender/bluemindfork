
create table t_dp_backup (
	id serial primary key,
	starttime timestamp not null,
	version varchar(32) not null
);

-- sequence already exists in 3.0
DO
$$
BEGIN
        CREATE SEQUENCE partgeneration_id_seq;
EXCEPTION WHEN duplicate_table THEN
        -- do nothing, it's already there
END
$$ LANGUAGE plpgsql;

create type t_generation_status as enum ('VALID', 'INVALID', 'UNKNOWN');

create table t_dp_partgeneration (
	id integer not null default nextval('partgeneration_id_seq'),
	backup_id integer not null references t_dp_backup(id),
	tag  character varying(255) not null,
	server_adr text not null,
 	starttime timestamp without time zone not null default now(),
 	endtime  timestamp without time zone,
 	size_mb  integer,
 	valid t_generation_status,
 	datatype  character varying(255)
);

create table t_dp_retentionpolicy (
	daily integer,
	weekly integer,
	monthly integer
);
