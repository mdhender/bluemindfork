
create table t_freebusy (
	container_id int4 references t_container(id),
    calendars  text[]
);