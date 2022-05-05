create table t_freebusy (
	container_id int4 references t_container(id),
    calendars  text[]
);

create index t_freebusy_container_id_idx on t_freebusy(container_id);