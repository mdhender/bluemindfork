CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;

CREATE EXTENSION IF NOT EXISTS btree_gin WITH schema pg_catalog;

CREATE TABLE t_container (
  id serial PRIMARY KEY,
  uid TEXT NOT NULL unique,
  container_type VARCHAR(50) NOT NULL,
  name text NOT NULL,
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

CREATE TABLE t_container_sequence (
  container_id int4 PRIMARY KEY references t_container(id),
  seq bigint default 0
);

CREATE TABLE t_container_item (
  id bigserial PRIMARY KEY,
  container_id int4 references t_container(id),
  uid TEXT NOT NULL,
  version int4 NOT NULL,
  external_id text,
  displayname text,
  createdby text NOT NULL,
  updatedby text NOT NULL,
  created timestamp NOT NULL,
  updated timestamp NOT NULL,
  flags int4  NOT NULL,
  UNIQUE(container_id, uid) /* l'uid est unique au seins du container */
);

CREATE INDEX idx_container_item_uid ON t_container_item(uid);
CREATE INDEX IF NOT EXISTS t_container_item_unseen_notdeleted_idx on t_container_item(container_id) WHERE (((flags)::bit(32) & '00000000000000000000000000000011'::bit(32)) = '00000000000000000000000000000000'::bit(32));
CREATE INDEX IF NOT EXISTS t_container_item_container_id_id_idx ON t_container_item(container_id, id);

CREATE TABLE t_container_acl (
  container_id int4 references t_container(id),
  subject TEXT NOT NULL,
  verb TEXT NOT NULL,
  position int2 NOT NULL
);
CREATE INDEX idx_container_acl_container_id ON t_container_acl(container_id);
CREATE INDEX idx_container_acl_subject ON t_container_acl(subject);
CREATE INDEX idx_container_acl_verb on t_container_acl using gin(verb);
CREATE INDEX idx_container_acl_subject_verb ON t_container_acl (subject, verb);

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

/** Changeset ng */

CREATE TABLE t_changeset (
  type smallint NOT NULL, /* 0 = create, 1 = update, 2 = delete */
  version int4 NOT NULL,
  container_id int4 references t_container(id) ON DELETE CASCADE,
  item_id bigint NOT NULL, /* soft reference t_container_item(id) */
  weight_seed int8 default 0,
  date timestamp NOT NULL,
  item_uid TEXT NOT NULL, /* soft reference t_container_item(uid) */
  PRIMARY KEY(container_id, version, item_uid)
) PARTITION  BY HASH (container_id);

CREATE INDEX IF NOT EXISTS t_changeset_container_id_idx ON t_changeset(container_id);
CREATE UNIQUE INDEX t_changeset_container_id_item_id_version_idx ON  t_changeset(container_id, item_id, version);
CREATE UNIQUE INDEX t_changeset_container_id_item_id_type_idx ON t_changeset (container_id, item_id, type);

DO LANGUAGE plpgsql
$$
DECLARE
  partition TEXT;
  partition_count INTEGER;
BEGIN
  SELECT INTO partition_count COALESCE(current_setting('bm.changeset_partitions', true)::integer, 256);

  FOR partition_key IN 0..(partition_count-1)
  LOOP
    partition := 't_changeset_' || partition_key;
    RAISE NOTICE 'CREATING CHANGESET NG PARTITION %...', partition;
    EXECUTE 'CREATE TABLE ' || partition || ' PARTITION OF t_changeset FOR VALUES WITH (MODULUS '|| partition_count || ', REMAINDER ' || partition_key || ');';
  END LOOP;
END;
$$;

CREATE TABLE IF NOT EXISTS q_changeset_cleanup(
  container_id int4 references t_container(id) ON DELETE CASCADE,
  item_id bigint NOT NULL, /* soft reference t_container_item(id) */
  date timestamp NOT NULL,
  PRIMARY KEY(container_id, item_id)
);

CREATE INDEX IF NOT EXISTS q_changeset_cleanup_container_date_idx ON q_changeset_cleanup (date);

CREATE OR REPLACE FUNCTION changeset_delete() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
	BEGIN
		DELETE FROM t_changeset WHERE container_id=NEW.container_id AND item_id=NEW.item_id AND type =1;
		INSERT INTO q_changeset_cleanup(container_id, item_id, date) VALUES(NEW.container_id, NEW.item_id, NEW.date);
		return NEW;
	END;
$$;

CREATE OR REPLACE FUNCTION q_changeset_cleanup_delete() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
	BEGIN
		DELETE FROM t_changeset WHERE container_id=OLD.container_id AND item_id=OLD.item_id ;
		return NEW;
	END;
$$;

CREATE OR REPLACE TRIGGER changeset_insert AFTER INSERT ON t_changeset FOR EACH ROW WHEN (NEW.type = 2) EXECUTE PROCEDURE changeset_delete();
CREATE OR REPLACE TRIGGER changeset_cleanup AFTER DELETE ON q_changeset_cleanup FOR EACH ROW EXECUTE PROCEDURE q_changeset_cleanup_delete();

CREATE OR REPLACE PROCEDURE proc_insert_t_changeset(
     IN p_version bigint,
     IN p_container_id bigint,
     IN p_item_uid TEXT,
     IN p_type smallint,
     IN p_item_id bigint,
     IN p_weight_seed bigint
)
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO t_changeset(version, container_id, item_uid, type, date, item_id, weight_seed)
    VALUES(p_version, p_container_id, p_item_uid, p_type,  now(), p_item_id, p_weight_seed)
    ON CONFLICT(container_id, item_id, type) DO UPDATE SET version = EXCLUDED.version, date = EXCLUDED.date;
EXCEPTION
-- same container_id, item_id, type, version -> do nothing
    WHEN unique_violation THEN
        NULL;
END;
$$;

/* Method to book a range of id */
CREATE OR REPLACE FUNCTION locked_multi_nextval(
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


CREATE OR REPLACE FUNCTION locked_nextval(
   use_seqname regclass
) RETURNS bigint AS $$
DECLARE
   reply bigint;
   lock_id bigint := (use_seqname::bigint - 2147483648)::integer;
BEGIN
   PERFORM pg_advisory_lock(lock_id);
   reply := nextval(use_seqname);
   PERFORM pg_advisory_unlock(lock_id);
   RETURN reply;
END;
$$ LANGUAGE plpgsql;
