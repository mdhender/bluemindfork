CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;

create table t_resource_type (
  id   varchar(255) NOT NULL,
  resource_container_id   int4 references t_container(id),
  label text NOT NULL,
   PRIMARY KEY(id, resource_container_id)
);

create type enum_resource_type_prop_type as enum 
  ('Number', 'String', 'Boolean');

create table t_resource_type_prop (
	id   varchar(255) NOT NULL,
  type_id  varchar(255),
  resource_container_id int4 ,
  label text NOT NULL,
  type text NOT NULL,
   PRIMARY KEY(id, type_id, resource_container_id),
  
   foreign key (type_id,resource_container_id) references t_resource_type(id, resource_container_id)
);

create type resource_reservation_mode_type as enum 
  ('OWNER_MANAGED', 'AUTO_ACCEPT', 'AUTO_ACCEPT_REFUSE');

create table t_resource (
  item_id   int4 references t_container_item(id) primary key,
  label text NOT NULL,
  type_id  varchar(255),
  description text NULL,
  mailbox_location varchar(64) NOT NULL,
  values  hstore,
  reservation_mode resource_reservation_mode_type
);