create table t_server (
	item_id 	  bigint references t_container_item(id) on delete cascade primary key,
	ip			  varchar(32),	
	fqdn		  text,
	name		  varchar(255),
	tags		  text[]
);

create table t_server_assignment (
	server_uid text not null,
	domain_uid text not null,
	tag text not null
);
