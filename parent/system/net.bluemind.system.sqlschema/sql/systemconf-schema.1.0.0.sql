CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;

create table t_systemconf (
	configuration hstore
);


INSERT INTO t_systemconf VALUES (  '"version"=>"test"');
