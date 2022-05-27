create table t_filehosting_file (
  uid varchar(39) NOT NULL primary key,
  owner text,
  path text NOT NULL,
  metadata hstore,
  download_limit integer default 0,
  expiration_date timestamp without time zone,
  access_count integer default 0,
  last_access timestamp without time zone
);

create index i_t_filehosting_file_uid on t_filehosting_file (uid);
create index i_t_filehosting_file_path on t_filehosting_file (path);

create table t_filehosting_file_info (
  id  SERIAL PRIMARY KEY,
  path text NOT NULL,
  created timestamp without time zone,
  owner text
);
  
create index i_t_filehosting_file_info_created on t_filehosting_file_info (created);
  