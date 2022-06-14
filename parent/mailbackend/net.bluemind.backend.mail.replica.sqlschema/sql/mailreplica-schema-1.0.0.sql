create table IF NOT EXISTS t_message_body (
	guid bytea primary key,
	subject text,
	structure jsonb,
	headers jsonb,
	recipients jsonb,
	message_id text,
	references_header text[],
	date_header timestamp not null,
	size integer not null,
	preview varchar(160),
	body_version integer not null default 0,
	created TIMESTAMP not null default now()
);

create table IF NOT EXISTS t_mailbox_replica (
	short_name text not null,
	parent_uid text,
	name text not null,
	last_uid int8 not null,
	highest_mod_seq int8 not null,
	recent_uid int8 not null,
	recent_time timestamp not null,
	last_append_date timestamp not null,
	pop3_last_login timestamp not null,
	uid_validity int8 not null,
	acls jsonb not null,
	options varchar(32) not null,
	sync_crc int8 not null,
	quotaroot text,
	xconv_mod_seq int8,
	unique_id text not null,
	container_id int4 not null references t_container(id) ON UPDATE CASCADE  on delete cascade,
	item_id bigint references t_container_item(id) on delete cascade UNIQUE
);

create index IF NOT EXISTS i_mailbox_replica on t_mailbox_replica (item_id);
create index IF NOT EXISTS i_mailbox_replica_names on t_mailbox_replica (container_id, name);


create table IF NOT EXISTS t_mailbox_record (
	message_body_guid bytea not null,
	imap_uid int8 not null,
	mod_seq int8 not null,
	last_updated timestamp not null,
	internal_date timestamp not null,
	system_flags int4 not null,
	other_flags text[],
	container_id int4 not null references t_container(id) ON UPDATE CASCADE  on delete cascade,
	conversation_id bigint,
	item_id bigint references t_container_item(id) ON UPDATE CASCADE  on delete cascade
);
create index IF NOT EXISTS t_mailbox_record_imap_uid ON t_mailbox_record (imap_uid);
create index IF NOT EXISTS i_mailbox_record_cid_imap_uid ON t_mailbox_record (container_id, imap_uid);
create index IF NOT EXISTS t_mailbox_record_body_guid ON t_mailbox_record (message_body_guid);
create index IF NOT EXISTS i_mailbox_record on t_mailbox_record (item_id);

create index if not exists i_mailbox_record_imap_idset on t_mailbox_record (container_id, system_flags, imap_uid) include (item_id);

-- Expunged flag specialized index
-- used in MailboxRecordStore.getExpiredItems()
CREATE INDEX ON t_mailbox_record (last_updated)
	INCLUDE (message_body_guid, item_id)
	WHERE (((system_flags)::bit(32) & (1<<31)::bit(32)) = (1<<31)::bit(32));


-- t_message_body orphan purge system
-- see MessageBodyStore.deleteOrphanBodies()
CREATE TABLE IF NOT EXISTS t_message_body_purge_queue (
	message_body_guid bytea UNIQUE PRIMARY KEY not null,
	created TIMESTAMP NOT NULL default now(),
	removed DATE
);
CREATE INDEX ON t_message_body_purge_queue (created);
CREATE INDEX ON t_message_body_purge_queue (removed);

CREATE OR REPLACE FUNCTION trigger_message_record_purge() RETURNS trigger AS
$$
DECLARE
	bypass boolean;
BEGIN
	SELECT INTO bypass coalesce(current_setting('bluemind.bypass_message_body_purge_queue', true)::boolean, false);
	IF bypass = false THEN
		IF TG_OP = 'DELETE' THEN
			-- Find references to other t_mailbox_record
			-- we want to add to the purge body queue unreferenced messages
			PERFORM 1 FROM t_mailbox_record WHERE message_body_guid = OLD.message_body_guid;
			IF NOT FOUND THEN
				INSERT INTO t_message_body_purge_queue (message_body_guid) VALUES (OLD.message_body_guid)
					ON CONFLICT(message_body_guid) DO NOTHING;
			END IF;
		ELSIF TG_OP = 'INSERT' THEN
			-- delete from the purge queue if present
			DELETE FROM t_message_body_purge_queue WHERE message_body_guid = NEW.message_body_guid;
		END IF;
	END IF;
	RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_message_record_purge AFTER DELETE OR INSERT ON t_mailbox_record
	FOR EACH ROW EXECUTE PROCEDURE trigger_message_record_purge();

create table IF NOT EXISTS t_seen_overlay (
	user_id text not null,
	unique_id text not null,
	last_read int8 not null,
	last_uid int8 not null,
	last_change int8 not null,
	seen_uids text not null,
	unique (user_id, unique_id)
);
create index IF NOT EXISTS t_seenoverlay_by_user_id ON t_seen_overlay (user_id);
create index IF NOT EXISTS t_seenoverlay_by_both ON t_seen_overlay (user_id, unique_id);

create table IF NOT EXISTS t_mailbox_sub (
	user_id text not null,
	mbox text not null
);
create index IF NOT EXISTS t_mailboxsub_by_user_id ON t_mailbox_sub (user_id);
alter table t_mailbox_sub drop constraint if exists ct_unique_mbox_subs;
alter table t_mailbox_sub add constraint ct_unique_mbox_subs unique (user_id, mbox);

create table IF NOT EXISTS t_quota_root (
	root text not null primary key,
	limit_kb int not null
);

create table IF NOT EXISTS t_mailbox_annotation (
	mbox text not null,
	user_id text not null, -- empty string when global
	entry text not null,
	value text,
	unique (mbox, user_id, entry)
);

create table IF NOT EXISTS t_sieve_script (
	user_id text not null,
	filename text not null,
	last_update int8 not null,
	active boolean not null,
	primary key (user_id, filename)
);
create index IF NOT EXISTS t_sieve_by_user_id ON t_sieve_script (user_id);

create table IF NOT EXISTS t_subtree_uid (
	domain_uid	text not null,
	mailbox_uid text not null,
	mailbox_name text not null,
	namespace text not null,
	unique (domain_uid, mailbox_uid)
);

create index IF NOT EXISTS subtree_uid_idx ON t_subtree_uid (domain_uid, mailbox_name);

CREATE TABLE t_conversation (
	messages JSONB not null,
	container_id int8 not null,
	item_id int8 not null REFERENCES t_container_item(id) ON DELETE CASCADE,
	unique(container_id, item_id)
) PARTITION BY HASH (container_id);
CREATE INDEX i_conversation_item_id ON t_conversation (item_id);

DO LANGUAGE plpgsql
$$
DECLARE
  partition TEXT;
  partition_count INTEGER;
BEGIN
  SELECT INTO partition_count COALESCE(current_setting('bm.conversation_partitions', true)::integer, 25);

  FOR partition_key IN 0..(partition_count-1)
  LOOP
    partition := 't_conversation_' || partition_key;
    RAISE NOTICE 'CREATING CHANGESET PARTITION %...', partition;
    EXECUTE 'CREATE TABLE ' || partition || ' PARTITION OF t_conversation FOR VALUES WITH (MODULUS '|| partition_count || ', REMAINDER ' || partition_key || ');';
  END LOOP;
END;
$$;


CREATE INDEX IF NOT EXISTS i_t_mailbox_record_conversation_date
  ON t_mailbox_record (conversation_id, internal_date)
  WHERE (system_flags::bit(32) & 4::bit(32)) = 0::bit(32);

CREATE INDEX IF NOT EXISTS i_t_mailbox_record_conversation_flags
  ON t_mailbox_record (container_id, conversation_id, system_flags)
  WHERE (system_flags::bit(32) & 4::bit(32)) = 0::bit(32);

CREATE TABLE IF NOT EXISTS v_conversation_by_folder (
    folder_id integer NOT NULL REFERENCES t_container ON DELETE CASCADE,
    conversation_id bigint NOT NULL,
    date timestamp without time zone,
    first timestamp without time zone,
    subject text NULL, 
    size integer NULL, 
    sender text NULL,
    unseen boolean NOT NULL default false, 
    flagged boolean NOT NULL default false,
    UNIQUE(folder_id, conversation_id)
) PARTITION BY HASH (folder_id);

CREATE INDEX IF NOT EXISTS v_conversation_by_folder_subject 
ON v_conversation_by_folder (folder_id, subject DESC) INCLUDE (conversation_id);
CREATE INDEX IF NOT EXISTS v_conversation_by_folder_date 
ON v_conversation_by_folder (folder_id, date DESC) INCLUDE (conversation_id);
CREATE INDEX IF NOT EXISTS v_conversation_by_folder_size 
ON v_conversation_by_folder (folder_id, size DESC) INCLUDE (conversation_id);
CREATE INDEX IF NOT EXISTS v_conversation_by_folder_sender 
ON v_conversation_by_folder (folder_id, sender DESC) INCLUDE (conversation_id);

CREATE INDEX IF NOT EXISTS v_conversation_by_folder_subject_unseen 
ON v_conversation_by_folder (folder_id, subject DESC) INCLUDE (conversation_id) 
WHERE (unseen is true);
CREATE INDEX IF NOT EXISTS v_conversation_by_folder_date_unseen 
ON v_conversation_by_folder (folder_id, date DESC) INCLUDE (conversation_id)
WHERE (unseen is true);
CREATE INDEX IF NOT EXISTS v_conversation_by_folder_size_unseen 
ON v_conversation_by_folder (folder_id, size DESC) INCLUDE (conversation_id) 
WHERE (unseen is true);
CREATE INDEX IF NOT EXISTS v_conversation_by_folder_sender_unseen 
ON v_conversation_by_folder (folder_id, sender DESC) INCLUDE (conversation_id)
WHERE (unseen is true);

CREATE INDEX IF NOT EXISTS v_conversation_by_folder_subject_flagged 
ON v_conversation_by_folder (folder_id, subject DESC) INCLUDE (conversation_id) 
WHERE (flagged is true);
CREATE INDEX IF NOT EXISTS v_conversation_by_folder_date_flagged 
ON v_conversation_by_folder (folder_id, date DESC) INCLUDE (conversation_id)
WHERE (flagged is true);
CREATE INDEX IF NOT EXISTS v_conversation_by_folder_size_flagged 
ON v_conversation_by_folder (folder_id, size DESC) INCLUDE (conversation_id) 
WHERE (flagged is true);
CREATE INDEX IF NOT EXISTS v_conversation_by_folder_sender_flagged 
ON v_conversation_by_folder (folder_id, sender DESC) INCLUDE (conversation_id)
WHERE (flagged is true);

DO LANGUAGE plpgsql
$$
DECLARE
  partition TEXT;
  partition_count INTEGER;
BEGIN
  SELECT INTO partition_count COALESCE(current_setting('bm.conversation_partitions', true)::integer, 25);

  FOR partition_key IN 0..(partition_count-1)
  LOOP
    partition := 'v_conversation_by_folder_' || partition_key;
    RAISE NOTICE 'CREATING v_conversation_by_folder PARTITION %...', partition;
    EXECUTE 'CREATE TABLE ' || partition || ' PARTITION OF v_conversation_by_folder FOR VALUES WITH (MODULUS '|| partition_count || ', REMAINDER ' || partition_key || ');';
  END LOOP;
END;
$$;


CREATE OR REPLACE FUNCTION v_conversation_by_folder_add(new_record t_mailbox_record)
  RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
  -- Update flags and not set flags (mask) respectively in every folder
    INSERT INTO
      v_conversation_by_folder (folder_id, conversation_id, flags, mask, date)
    VALUES
      (new_record.container_id, new_record.conversation_id, new_record.system_flags, -(new_record.system_flags + 1), new_record.internal_date)
    ON CONFLICT (folder_id, conversation_id) DO UPDATE
      SET
        flags = EXCLUDED.flags | v_conversation_by_folder.flags,
        mask = EXCLUDED.mask | (-(v_conversation_by_folder.flags + 1)),
        date = (select greatest(new_record.internal_date, EXCLUDED.date));
END;
$$;

CREATE OR REPLACE FUNCTION v_trig_conversation_by_folder_add() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM v_conversation_by_folder_add(NEW);
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION v_conversation_by_folder_update() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
DECLARE
BEGIN
  PERFORM v_conversation_by_folder_update_row(NEW.conversation_id, NEW.container_id);
  RETURN NEW;
END;
$$;


CREATE OR REPLACE FUNCTION v_conversation_by_folder_remove() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
DECLARE
BEGIN
    PERFORM v_conversation_by_folder_update_row(OLD.conversation_id, OLD.container_id);
    RETURN NULL;
END;
$$;


CREATE OR REPLACE FUNCTION v_conversation_by_folder_update_row(bigint, integer) RETURNS VOID
  LANGUAGE plpgsql
AS $$
DECLARE
  existing record;
BEGIN
    SELECT
      bit_or(system_flags) as flags,
      bit_or(-(system_flags + 1)) as mask,
      max(internal_date) as date
    FROM
      t_mailbox_record INTO existing
    WHERE
      container_id = $2
    AND
      system_flags::bit(32) & 4::bit(32) = 0::bit(32)
    AND conversation_id = $1
    GROUP BY container_id, conversation_id;

    IF (existing IS NULL) THEN
        DELETE FROM v_conversation_by_folder WHERE folder_id = $2 AND conversation_id = $1;
    ELSE
        -- Update flags and not set flags (mask) respectively in every folder
        UPDATE v_conversation_by_folder
        SET flags = existing.flags, mask = existing.mask, date = existing.date
        WHERE folder_id = $2
        AND conversation_id = $1;
    END IF;
END;
$$;

-- Handle the v_converstaion_by_folder data (we are changing all conversation_ids)
CREATE OR REPLACE FUNCTION v_conversation_by_folder_update_conversation_row()
  RETURNS TRIGGER LANGUAGE plpgsql
AS $$
DECLARE
  existing record;
BEGIN
    -- Remove the old conversation, what ever the deleted flag is
    PERFORM v_conversation_by_folder_update_row(OLD.conversation_id, OLD.container_id);
    -- Update the new conversation
    -- Filtering OUT deleted mailbox_records
    IF NEW.system_flags & 4 = 0 THEN
      PERFORM v_conversation_by_folder_add(NEW);
    END IF;
    RETURN NEW;
END;
$$;

-------------------------------------------------
-- Message inserted or marked not deleted
CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_insert
  AFTER INSERT
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (NEW.conversation_id IS NOT NULL AND NEW.system_flags & 4 = 0)
  EXECUTE PROCEDURE v_trig_conversation_by_folder_add();

-- Message marked as deleted, or removed
CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_undelete
  AFTER UPDATE
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (NEW.conversation_id IS NOT NULL AND OLD.system_flags & 4 = 4 AND NEW.system_flags & 4 = 0)
  EXECUTE PROCEDURE v_trig_conversation_by_folder_add();

-- Conversation is replaced
CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_update_conversation
  AFTER UPDATE
  ON t_mailbox_record
  FOR EACH ROW
  WHEN (
    OLD.conversation_id IS DISTINCT FROM NEW.conversation_id
  )
  EXECUTE PROCEDURE v_conversation_by_folder_update_conversation_row();

CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_update
  AFTER UPDATE
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (
    OLD.conversation_id IS NOT NULL AND OLD.system_flags & 4 = 0 AND NEW.system_flags & 4 = 0 
    AND (NEW.internal_date != OLD.internal_date OR NEW.system_flags != OLD.system_flags)
  )
  EXECUTE PROCEDURE v_conversation_by_folder_update();


CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_remove
  AFTER UPDATE
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (OLD.conversation_id IS NOT NULL AND OLD.system_flags & 4 = 0 AND NEW.system_flags & 4 = 4)
  EXECUTE PROCEDURE v_conversation_by_folder_remove();

CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_expunge
  AFTER DELETE
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (OLD.conversation_id IS NOT NULL AND OLD.system_flags & 4 = 0)
  EXECUTE PROCEDURE v_conversation_by_folder_remove();


---------------------------------------

CREATE TABLE IF NOT EXISTS s_mailbox_record (
	   item_id bigint NOT NULL,  
     container_id integer NOT NULL,
     unseen boolean NOT NULL default false, 
     flagged boolean NOT NULL default false, 
     date timestamp without time zone NOT NULL,
     subject text NULL, 
     size integer NULL, 
     sender text NULL, 
     PRIMARY KEY(item_id, container_id)
) PARTITION BY HASH (container_id);

CREATE INDEX IF NOT EXISTS s_mailbox_record_subject 
ON s_mailbox_record (container_id, subject DESC) INCLUDE (item_id);
CREATE INDEX IF NOT EXISTS s_mailbox_record_date 
ON s_mailbox_record (container_id, date DESC) INCLUDE (item_id);
CREATE INDEX IF NOT EXISTS s_mailbox_record_size 
ON s_mailbox_record (container_id, size DESC) INCLUDE (item_id);
CREATE INDEX IF NOT EXISTS s_mailbox_record_sender 
ON s_mailbox_record (container_id, sender DESC) INCLUDE (item_id);

CREATE INDEX IF NOT EXISTS s_mailbox_record_subject_unseen 
ON s_mailbox_record (container_id, subject DESC) INCLUDE (item_id) 
WHERE (unseen is true);
CREATE INDEX IF NOT EXISTS s_mailbox_record_date_unseen 
ON s_mailbox_record (container_id, date DESC) INCLUDE (item_id)
WHERE (unseen is true);
CREATE INDEX IF NOT EXISTS s_mailbox_record_size_unseen 
ON s_mailbox_record (container_id, size DESC) INCLUDE (item_id) 
WHERE (unseen is true);
CREATE INDEX IF NOT EXISTS s_mailbox_record_sender_unseen 
ON s_mailbox_record (container_id, sender DESC) INCLUDE (item_id)
WHERE (unseen is true);

CREATE INDEX IF NOT EXISTS s_mailbox_record_subject_flagged 
ON s_mailbox_record (container_id, subject DESC) INCLUDE (item_id) 
WHERE (flagged is true);
CREATE INDEX IF NOT EXISTS s_mailbox_record_date_flagged 
ON s_mailbox_record (container_id, date DESC) INCLUDE (item_id)
WHERE (flagged is true);
CREATE INDEX IF NOT EXISTS s_mailbox_record_size_flagged 
ON s_mailbox_record (container_id, size DESC) INCLUDE (item_id) 
WHERE (flagged is true);
CREATE INDEX IF NOT EXISTS s_mailbox_record_sender_flagged 
ON s_mailbox_record (container_id, sender DESC) INCLUDE (item_id)
WHERE (flagged is true);

DO LANGUAGE plpgsql                                                                                          
$$
DECLARE
  partition TEXT;
  partition_count INTEGER;                                                                                                                     
  BEGIN                                  
  SELECT INTO partition_count COALESCE(current_setting('bm.s_mailbox_record_partitions', true)::integer, 256);
    
  FOR partition_key IN 0..(partition_count-1)
  LOOP
    partition := 's_mailbox_record_' || partition_key;
    EXECUTE 'CREATE TABLE ' || partition || ' PARTITION OF s_mailbox_record  FOR VALUES WITH (MODULUS '|| partition_count || ', REMAINDER ' || partition_key || ');';
  END LOOP;
  END;
$$;

CREATE OR REPLACE FUNCTION s_mailbox_record_add() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
DECLARE
  body record;
BEGIN
    SELECT REGEXP_REPLACE(subject, '^([\W_]*|re\s*:)+', '', 'i') as subject, size, address  
    FROM t_message_body, jsonb_to_recordset(recipients) AS rec(kind TEXT, address TEXT) INTO body
    WHERE guid = NEW.message_body_guid
    AND kind = 'Originator';

    INSERT INTO s_mailbox_record (item_id, container_id, date, subject, size, sender, unseen, flagged) 
    VALUES (NEW.item_id, NEW.container_id, NEW.internal_date, body.subject, body.size, body.address, NEW.system_flags & 16 != 16,  NEW.system_flags & 2 = 2);
        
    return NEW;
  end;
$$;

CREATE OR REPLACE FUNCTION s_mailbox_record_update() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
DECLARE
  body record;
BEGIN
    if (OLD.message_body_guid != NEW.message_body_guid and OLD.internal_date != NEW.internal_date) then
      SELECT REGEXP_REPLACE(subject, '^([\W_]*|re\s*:)+', '', 'i') as subject, size, address FROM t_message_body, jsonb_to_recordset(recipients) AS rec(kind TEXT, address TEXT) INTO body WHERE guid = NEW.message_body_guid AND kind = 'Originator';

      UPDATE s_mailbox_record 
      SET unseen = NEW.system_flags & 16 != 16, 
          flagged = NEW.system_flags & 2 = 2,  
          date = NEW.internal_date,  
          subject = body.subject, 
          size = body.size, 
          sender = body.address
      WHERE container_id = NEW.container_id 
      AND item_id = NEW.item_id;
     
    elsif OLD.internal_date != NEW.internal_date then  
      UPDATE s_mailbox_record 
      SET unseen = NEW.system_flags & 16 != 16, 
          flagged = NEW.system_flags & 2 = 2,  
          date = NEW.internal_date  
      WHERE container_id = NEW.container_id 
      AND item_id = NEW.item_id;
    
    else 
      UPDATE s_mailbox_record 
      SET unseen = NEW.system_flags & 16 != 16, 
          flagged = NEW.system_flags & 2 = 2  
      WHERE container_id = NEW.container_id 
      AND item_id = NEW.item_id;

    end if;

  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION s_mailbox_record_remove() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
DECLARE
BEGIN
    DELETE FROM s_mailbox_record WHERE container_id = OLD.container_id AND item_id = OLD.item_id;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION s_mailbox_record_truncate() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
DECLARE
BEGIN
    TRUNCATE s_mailbox_record;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE TRIGGER virtual_message_record_insert
  AFTER INSERT
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (NEW.system_flags & 4 = 0)
  EXECUTE PROCEDURE s_mailbox_record_add();

CREATE OR REPLACE TRIGGER virtual_message_record_undelete
  AFTER UPDATE
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (OLD.system_flags & 4 = 4 AND NEW.system_flags & 4 = 0)
  EXECUTE PROCEDURE s_mailbox_record_add();

CREATE OR REPLACE TRIGGER virtual_message_record_update
  AFTER UPDATE
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (OLD.system_flags & 4 = 0 AND NEW.system_flags & 4 = 0)
  EXECUTE PROCEDURE s_mailbox_record_update();

CREATE OR REPLACE TRIGGER virtual_message_record_remove
  AFTER UPDATE
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (OLD.system_flags & 4 = 0 AND NEW.system_flags & 4 = 4)
  EXECUTE PROCEDURE s_mailbox_record_remove();

CREATE OR REPLACE TRIGGER virtual_message_record_expunge
  AFTER DELETE
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (OLD.system_flags & 4 = 0)
  EXECUTE PROCEDURE s_mailbox_record_remove();

CREATE OR REPLACE TRIGGER virtual_message_record_truncate
  AFTER TRUNCATE
  ON t_mailbox_record
  FOR EACH STATEMENT
  EXECUTE PROCEDURE s_mailbox_record_truncate();

---------------------------------------
