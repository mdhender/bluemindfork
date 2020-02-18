CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;

CREATE TABLE t_group (
  item_id       int4 references t_container_item(id) PRIMARY KEY,
  container_id  int4 references t_container(id),

  name          text not null,
  description   text,

  hidden        boolean default false,
  hiddenMembers boolean default true,

  mailArchived  boolean,
  server_id	text, -- uid
  properties hstore,
  
  UNIQUE(container_id, name)
);

CREATE TABLE t_group_groupmember (
  group_parent_id int4 references t_group(item_id),
  group_child_id int4 references t_group(item_id),

  PRIMARY KEY(group_parent_id, group_child_id)
);

CREATE TABLE t_group_usermember  (
  group_id     int4 references t_group(item_id),
  user_id     int4 references t_container_item(id) on delete cascade,
  PRIMARY KEY(group_id, user_id)
);

CREATE TABLE t_group_externalusermember  (
  group_id     int4 references t_group(item_id),
  external_user_id     int4 references t_container_item(id) on delete cascade,
  PRIMARY KEY(group_id, external_user_id)
);

CREATE TABLE t_group_flat_members (
  group_id        int4 references t_group(item_id),
  user_id     int4 references t_container_item(id) on delete cascade,
  PRIMARY KEY(group_id, user_id)
);

CREATE INDEX t_group_flat_members_user_id_fkey ON t_group_flat_members (user_id);
CREATE INDEX t_group_flat_members_group_id_fkey ON t_group_flat_members (group_id);
