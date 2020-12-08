CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;

CREATE EXTENSION IF NOT EXISTS btree_gin WITH schema pg_catalog;

CREATE TABLE t_container (
	id serial PRIMARY KEY,
	uid TEXT NOT NULL unique,
	container_type VARCHAR(50) NOT NULL,
	name text	NOT NULL,
	owner text NOT NULL,
	createdby text NOT NULL,
	updatedby text NOT NULL,
	created timestamp NOT NULL,
	updated timestamp NOT NULL,
	defaultContainer boolean DEFAULT FALSE,
    domain_uid text NULL,
    readonly boolean DEFAULT FALSE    
);

CREATE INDEX idx_container_type  ON t_container USING gin(container_type);
CREATE INDEX idx_container_owner  ON t_container(owner);
CREATE INDEX idx_container_acl_subject_verb ON t_container_acl (subject, verb);

CREATE TABLE t_container_sequence (
	container_id int4 PRIMARY KEY references t_container(id),
	seq bigint default 0
);

CREATE TABLE t_container_item (
	id serial PRIMARY KEY,
	container_id int4 references t_container(id),
	uid TEXT NOT NULL,
	version int4 NOT NULL,
	external_id text,
	displayname text,
	createdby text NOT NULL,
	updatedby text NOT NULL,
	created timestamp NOT NULL,
	updated timestamp NOT NULL,
	flags	int4	NOT NULL,
    UNIQUE(container_id, uid) /* l'uid est unique au seins du container */
);

CREATE INDEX idx_container_item_uid ON t_container_item(uid);


CREATE TABLE t_container_changelog (
	version int4 NOT NULL,
	container_id int4 references t_container(id),
	item_uid TEXT NOT NULL, /* soft reference t_container_item(uid) */
	item_external_id TEXT, /* soft reference t_container_id(external_id) */
	type smallint NOT NULL, /* 0 = create, 1 = update, 3 = delete */
	author TEXT NOT NULL,
	date timestamp NOT NULL,
	origin TEXT,
	item_id int4, /* soft reference t_container_item(id) */
	weight_seed int8 default 0,
	PRIMARY KEY(version, container_id, item_uid)
);

CREATE INDEX tcc_container_id_fkey ON t_container_changelog(container_id);
CREATE INDEX t_container_changelog_container_id_item_id_version_idx ON t_container_changelog(container_id, item_id, version);

CREATE TABLE t_container_acl (
	container_id int4 references t_container(id),
	subject TEXT NOT NULL,
	verb TEXT NOT NULL,
	position int2 NOT NULL
);
CREATE INDEX idx_container_acl_container_id ON t_container_acl(container_id);
CREATE INDEX idx_container_acl_subject ON t_container_acl(subject);
CREATE INDEX idx_container_acl_verb on t_container_acl using gin(verb);

CREATE TABLE t_container_settings (
	container_id int4 references t_container(id),
    settings  hstore NOT NULL
);
CREATE INDEX idx_container_settings_container_id ON t_container_settings(container_id);

CREATE TABLE t_container_personal_settings (
	container_id int4 references t_container(id),
	subject TEXT NOT NULL,
    settings  hstore NOT NULL
);
CREATE INDEX idx_container_personal_settings_container_id ON t_container_personal_settings(container_id);

CREATE TABLE t_container_location (
  container_uid text NOT NULL,
  location text,
  PRIMARY KEY(container_uid)
);

/** Changeset */

CREATE TABLE t_container_changeset (LIKE t_container_changelog) PARTITION  BY HASH (container_id);

CREATE INDEX idx_container_changeset_container_id ON t_container_changeset(container_id);

DO LANGUAGE plpgsql 
$$
DECLARE
  partition TEXT;
  idx TEXT;
  partition_count INTEGER;
BEGIN
  SELECT INTO partition_count COALESCE(current_setting('bm.changeset_partitions', true)::integer, 256);

  FOR partition_key IN 0..(partition_count-1)
  LOOP
    partition := 't_container_changeset_' || partition_key;
    idx := partition || '_item_id_version_idx';
    RAISE NOTICE 'CREATING CHANGESET PARTITION %...', partition;    
    EXECUTE 'CREATE TABLE ' || partition || ' PARTITION OF t_container_changeset FOR VALUES WITH (MODULUS '|| partition_count || ', REMAINDER ' || partition_key || ');';
    EXECUTE 'CREATE INDEX ' || idx || ' ON ' || partition || '(item_id, version);';
  END LOOP;
END;
$$;

/** Changeset: data trigger */

CREATE OR REPLACE FUNCTION changeset_insert() RETURNS TRIGGER
  LANGUAGE plpgsql                                    
AS $$
BEGIN
    DELETE FROM t_container_changeset WHERE item_id = NEW.item_id AND container_id = NEW.container_id;
    INSERT INTO t_container_changeset 
    (SELECT * FROM t_container_changelog where item_id = NEW.item_id AND container_id = NEW.container_id ORDER BY version DESC limit 1) 
    UNION 
    (SELECT * FROM t_container_changelog where item_id = NEW.item_id AND container_id = NEW.container_id ORDER BY version  limit 1);
    return NEW;
  end;
$$;

CREATE TRIGGER changelog_insert AFTER INSERT  ON t_container_changelog FOR EACH ROW EXECUTE PROCEDURE changeset_insert();
CREATE TRIGGER changelog_update AFTER UPDATE ON t_container_changelog FOR EACH ROW WHEN (OLD.item_id IS DISTINCT FROM NEW.item_id) EXECUTE PROCEDURE changeset_insert();

/* Method to book a range of id */

CREATE OR REPLACE FUNCTION multi_nextval(
   use_seqname regclass,
   use_increment integer
) RETURNS bigint AS $$
DECLARE
   reply bigint;
   lock_id bigint := (use_seqname::bigint - 2147483648)::integer;
BEGIN
   PERFORM pg_advisory_lock(lock_id);
   reply := nextval(use_seqname);
   PERFORM setval(use_seqname, reply + use_increment - 1, TRUE);
   PERFORM pg_advisory_unlock(lock_id);
   RETURN reply;
END;
$$ LANGUAGE plpgsql;
