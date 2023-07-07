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

-- This index is used by MessageBodyTierChangeService (tier requeues)
CREATE INDEX IF NOT EXISTS t_message_body_created_guid_idx ON t_message_body (created, guid);

CREATE TYPE enum_q_tier AS ENUM (
    'NORMAL',
    'SLOW'
);

CREATE TABLE IF NOT EXISTS q_message_body_tier_change (
    message_body_guid bytea PRIMARY KEY REFERENCES t_message_body(guid) ON DELETE CASCADE,
    change_after timestamp not null,
    retries integer not null default 0,
    tier enum_q_tier not null
);
CREATE INDEX IF NOT EXISTS i_q_message_body_tier_change ON q_message_body_tier_change(change_after);

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


-- PLEASE BE CAREFUL OF ALIGNMENT ON THIS TABLE!
CREATE TABLE IF NOT EXISTS t_mailbox_record (
    item_id int8 NOT NULL REFERENCES t_container_item(id) ON DELETE CASCADE,
    subtree_id int4 NOT NULL REFERENCES t_container(id) ON DELETE CASCADE,
    container_id int4 NOT NULL REFERENCES t_container(id) ON DELETE CASCADE,
    imap_uid int8 NOT NULL,
    conversation_id int8,
    last_updated timestamp NOT NULL,
    internal_date timestamp NOT NULL,
    system_flags int4 NOT NULL,
    message_body_guid bytea NOT NULL,
    other_flags text[],
    PRIMARY KEY (subtree_id, item_id)
) PARTITION BY HASH(subtree_id);

CREATE INDEX IF NOT EXISTS t_mailbox_record_p_container_id_imap_uid_idx
    ON t_mailbox_record (container_id, imap_uid);
CREATE INDEX IF NOT EXISTS t_mailbox_record_p_message_body_guid_idx
    ON t_mailbox_record (message_body_guid);
CREATE INDEX IF NOT EXISTS t_mailbox_record_p_container_id_system_flags_imap_uid_item__idx
    ON t_mailbox_record (container_id, system_flags, imap_uid) INCLUDE (item_id);
CREATE INDEX IF NOT EXISTS t_mailbox_record_p_last_updated_message_body_guid_item_id_idx
    ON t_mailbox_record (last_updated)
    INCLUDE (message_body_guid, item_id)
    WHERE (((system_flags)::bit(32) & (1<<31)::bit(32)) = (1<<31)::bit(32));
CREATE INDEX IF NOT EXISTS t_mailbox_record_p_container_id_conversation_id_system_flag_idx
    ON t_mailbox_record (container_id, conversation_id, system_flags)
    WHERE (system_flags::bit(32) & 4::bit(32)) = 0::bit(32);

DO LANGUAGE plpgsql
$$
DECLARE
    partition TEXT;
    partition_count INTEGER;
BEGIN
    SELECT INTO partition_count COALESCE(current_setting('bm.mailbox_record_partitions', true)::integer, 256);
    FOR partition_key IN 0..(partition_count-1)
    LOOP
        partition := 't_mailbox_record_' || partition_key;
        RAISE NOTICE 'CREATING t_mailbox_record PARTITION %...', partition;
        EXECUTE 'CREATE TABLE ' || partition || ' PARTITION OF t_mailbox_record FOR VALUES WITH (MODULUS '|| partition_count || ', REMAINDER ' || partition_key || ');';
    END LOOP;
END;
$$;

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
            RETURN NEW;
        END IF;
    ELSE
        IF TG_OP = 'INSERT' THEN
            RETURN NEW;
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_message_record_purge AFTER DELETE OR INSERT ON t_mailbox_record
    FOR EACH ROW EXECUTE PROCEDURE trigger_message_record_purge();

CREATE TABLE IF NOT EXISTS v_conversation_by_folder (
    folder_id integer NOT NULL REFERENCES t_container ON DELETE CASCADE,
    size integer NULL,
    conversation_id bigint NOT NULL,
    date timestamp without time zone,
    first timestamp without time zone,
    unseen boolean NOT NULL default false, 
    flagged boolean NOT NULL default false,
    subject text NULL,
    sender text NULL,
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
        EXECUTE 'CREATE TABLE ' || partition || ' PARTITION OF s_mailbox_record FOR VALUES WITH (MODULUS '|| partition_count || ', REMAINDER ' || partition_key || ');';
    END LOOP;
    END;
$$;

CREATE OR REPLACE FUNCTION s_mailbox_record_add() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    body record;
    disable_sort_triggers boolean;
BEGIN
    SELECT INTO disable_sort_triggers COALESCE(current_setting('bm.disable_sort_triggers', true)::bool, false);
    IF disable_sort_triggers = true THEN
        RETURN NEW;
    END IF;
    SELECT
        regexp_replace(unaccent(subject), '^([\W]*|re\s*:)+', '', 'i') AS subject,
        jsonb_path_query_first(recipients, '$[*] ? (@.kind == "Originator" && @.address like_regex "[^.]+@[^.]+\.[^.]+")') ->> 'address' AS sender,
        size
    FROM t_message_body
    INTO body
    WHERE guid = NEW.message_body_guid;

    INSERT INTO s_mailbox_record
        (item_id, container_id, date, subject, size, sender, unseen, flagged)
    VALUES
        (NEW.item_id::bigint, NEW.container_id, NEW.internal_date, body.subject, body.size, body.sender, NEW.system_flags & 16 != 16,  NEW.system_flags & 2 = 2);
    RETURN NEW;
  END;
$$;

CREATE OR REPLACE FUNCTION s_mailbox_record_update() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    body record;
    disable_sort_triggers boolean;
BEGIN
    SELECT INTO disable_sort_triggers COALESCE(current_setting('bm.disable_sort_triggers', true)::bool, false);
    IF disable_sort_triggers = true THEN
        RETURN NEW;
    END IF;
    IF (OLD.message_body_guid != NEW.message_body_guid) THEN
        SELECT
            regexp_replace(unaccent(subject), '^([\W]*|re\s*:)+', '', 'i') AS subject,
            jsonb_path_query_first(recipients, '$[*] ? (@.kind == "Originator" && @.address like_regex "[^.]+@[^.]+\.[^.]+")') ->> 'address' AS sender,
            size
        FROM t_message_body
        INTO body
        WHERE guid = NEW.message_body_guid;

        UPDATE s_mailbox_record
        SET unseen = NEW.system_flags & 16 != 16,
            flagged = NEW.system_flags & 2 = 2,
            date = NEW.internal_date,
            subject = body.subject,
            size = body.size,
            sender = body.sender
        WHERE container_id = NEW.container_id
        AND item_id = NEW.item_id::bigint;
    ELSIF OLD.internal_date != NEW.internal_date THEN
        UPDATE s_mailbox_record
        SET unseen = NEW.system_flags & 16 != 16,
            flagged = NEW.system_flags & 2 = 2,
            date = NEW.internal_date
        WHERE container_id = NEW.container_id
        AND item_id = NEW.item_id::bigint;
    ELSE
        UPDATE s_mailbox_record
        SET unseen = NEW.system_flags & 16 != 16,
            flagged = NEW.system_flags & 2 = 2
        WHERE container_id = NEW.container_id
        AND item_id = NEW.item_id::bigint;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION s_mailbox_record_remove() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    disable_sort_triggers boolean;
BEGIN
    SELECT INTO disable_sort_triggers COALESCE(current_setting('bm.disable_sort_triggers', true)::bool, false);
    IF NOT disable_sort_triggers THEN
        DELETE FROM s_mailbox_record WHERE container_id = OLD.container_id AND item_id = OLD.item_id;
    END IF;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION s_mailbox_record_truncate() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    disable_sort_triggers boolean;
BEGIN
    SELECT INTO disable_sort_triggers COALESCE(current_setting('bm.disable_sort_triggers', true)::bool, false);
    IF NOT disable_sort_triggers THEN
        TRUNCATE s_mailbox_record;
    END IF;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE TRIGGER virtual_message_record_insert
    AFTER INSERT ON t_mailbox_record FOR EACH ROW
    -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
    WHEN (NEW.system_flags & 4 = 0)
    EXECUTE PROCEDURE s_mailbox_record_add();

CREATE OR REPLACE TRIGGER virtual_message_record_undelete
    AFTER UPDATE ON t_mailbox_record FOR EACH ROW
    -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
    WHEN (OLD.system_flags & 4 = 4 AND NEW.system_flags & 4 = 0)
    EXECUTE PROCEDURE s_mailbox_record_add();

CREATE OR REPLACE TRIGGER virtual_message_record_update
    AFTER UPDATE ON t_mailbox_record FOR EACH ROW
    -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
    WHEN (OLD.system_flags & 4 = 0 AND NEW.system_flags & 4 = 0)
    EXECUTE PROCEDURE s_mailbox_record_update();

CREATE OR REPLACE TRIGGER virtual_message_record_remove
    AFTER UPDATE ON t_mailbox_record FOR EACH ROW
    -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
    WHEN (OLD.system_flags & 4 = 0 AND NEW.system_flags & 4 = 4)
    EXECUTE PROCEDURE s_mailbox_record_remove();

CREATE OR REPLACE TRIGGER virtual_message_record_expunge
    AFTER DELETE ON t_mailbox_record FOR EACH ROW
    -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
    WHEN (OLD.system_flags & 4 = 0)
    EXECUTE PROCEDURE s_mailbox_record_remove();

CREATE OR REPLACE TRIGGER virtual_message_record_truncate
    AFTER TRUNCATE ON t_mailbox_record FOR EACH STATEMENT
    EXECUTE PROCEDURE s_mailbox_record_truncate();
    
-----------------------------------------------------------
-- t_mailbox_record expunged purge system
-- see MailboxRecordStore.getExpiredItems()
CREATE TABLE IF NOT EXISTS q_mailbox_record_expunged (
    container_id integer not null REFERENCES t_container(id) ON DELETE CASCADE,
    subtree_id integer not null REFERENCES t_container(id) ON DELETE CASCADE,
    item_id bigint not null REFERENCES t_container_item(id) ON DELETE CASCADE,
    imap_uid bigint not null,
    created TIMESTAMP NOT NULL,
    PRIMARY KEY (subtree_id, item_id)
);

CREATE INDEX IF NOT EXISTS q_mailbox_record_expunged_container_id_idx
    ON q_mailbox_record_expunged (container_id);
CREATE INDEX IF NOT EXISTS q_mailbox_record_expunged_subtree_id_idx
    ON q_mailbox_record_expunged (subtree_id);
CREATE INDEX IF NOT EXISTS q_mailbox_record_expunged_item_id_idx
    ON q_mailbox_record_expunged (item_id);
CREATE INDEX IF NOT EXISTS q_mailbox_record_expunged_imap_uid_idx
    ON q_mailbox_record_expunged (imap_uid);
CREATE INDEX IF NOT EXISTS q_mailbox_record_expunged_created_idx
    ON q_mailbox_record_expunged (created);


CREATE OR REPLACE FUNCTION fct_mailbox_record_expunged() RETURNS trigger AS
$$
DECLARE
    bypass boolean;
BEGIN
    SELECT INTO bypass coalesce(current_setting('bluemind.bypass_mailbox_record_expunged_queue', true)::boolean, false);
    IF bypass = false THEN
        IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
            -- we want to add to the queue expunged messages
            INSERT INTO q_mailbox_record_expunged (container_id, subtree_id, item_id, imap_uid, created) 
            VALUES (NEW.container_id, NEW.subtree_id, NEW.item_id, NEW.imap_uid, NEW.last_updated)
            -- do nothing on conflict because a message can be expunge only once
            ON CONFLICT(subtree_id, item_id) DO NOTHING;
            RETURN NEW;
        ELSIF TG_OP = 'DELETE' THEN
            -- we want to remove from the queue deleted messages
            DELETE FROM q_mailbox_record_expunged 
            WHERE subtree_id = OLD.subtree_id
            AND item_id = OLD.item_id;
        END IF;
    ELSE
        IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
            RETURN NEW;
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE TRIGGER trigger_mailbox_record_expunged_insert
    AFTER INSERT ON t_mailbox_record FOR EACH ROW
    -- WHEN new message is Expunged 
    WHEN (((NEW.system_flags)::bit(32) & (1<<31)::bit(32)) = (1<<31)::bit(32))
    EXECUTE PROCEDURE fct_mailbox_record_expunged();

CREATE OR REPLACE TRIGGER trigger_mailbox_record_expunged_update
    AFTER UPDATE ON t_mailbox_record FOR EACH ROW
    -- WHEN updated message became Expunged 
    WHEN ((((OLD.system_flags)::bit(32) & (1<<31)::bit(32)) = 0::bit(32)) 
    AND (((NEW.system_flags)::bit(32) & (1<<31)::bit(32)) = (1<<31)::bit(32)))
    EXECUTE PROCEDURE fct_mailbox_record_expunged();

CREATE OR REPLACE TRIGGER trigger_mailbox_record_expunged_delete
    AFTER DELETE ON t_mailbox_record FOR EACH ROW
    EXECUTE PROCEDURE fct_mailbox_record_expunged();
    

-- Optimization to store distinct user flags per folder
CREATE TABLE t_mailbox_folder_flags (
    id bigint GENERATED ALWAYS AS IDENTITY,
    container_id bigint not null references t_container(id),
    occurrence integer not null default 1,
    flag text not null,
    UNIQUE(container_id, flag)
    WITH (fillfactor = 80) -- Optimization to allow the maximum amount of HOT updates
);


CREATE OR REPLACE FUNCTION fct_mailbox_record_flag_update() RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    _flag text;
    _flags_removed text[] := '{}';
    _flags_added text[] := '{}';
BEGIN
    IF TG_OP = 'INSERT' THEN
        FOREACH _flag IN ARRAY NEW.other_flags
        LOOP
            IF NOT _flag = ANY(_flags_added) THEN
                _flags_added = array_append(_flags_added, _flag);
            END IF;
        END LOOP;
    ELSIF TG_OP = 'UPDATE' THEN
        -- record was expunged
        IF OLD.system_flags & (1 << 31) = 0 AND NEW.system_flags & (1 << 31) = (1 << 31) THEN
            _flags_removed = NEW.other_flags;
        ELSE
            -- flags where updated
            FOREACH _flag IN ARRAY (NEW.other_flags || OLD.other_flags)
            LOOP
                IF _flag = ANY(OLD.other_flags) THEN
                    IF NOT (_flag = ANY (NEW.other_flags)) THEN
                        IF NOT _flag = ANY(_flags_removed) THEN
                            _flags_removed = array_append(_flags_removed, _flag);
                        END IF;
                    END IF;
                ELSE
                    -- Flag added
                    IF NOT _flag = ANY(_flags_added) THEN
                        _flags_added = array_append(_flags_added, _flag);
                    END IF;
                END IF;
            END LOOP;
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE t_mailbox_folder_flags SET occurrence = t_mailbox_folder_flags.occurrence - 1
            WHERE container_id = OLD.container_id AND flag = ANY(OLD.other_flags);
        RETURN OLD;
    END IF;

    IF array_length(_flags_removed, 1) > 0 THEN
        UPDATE t_mailbox_folder_flags SET occurrence = t_mailbox_folder_flags.occurrence - 1
            WHERE container_id = NEW.container_id AND flag = ANY(NEW.other_flags);
    END IF;

    IF array_length(_flags_added, 1) > 0 THEN
        FOREACH _flag IN ARRAY _flags_added
        LOOP
            -- Flag added
            INSERT INTO t_mailbox_folder_flags(container_id, flag)
            VALUES (NEW.container_id, _flag)
            ON CONFLICT(container_id, flag) DO UPDATE SET occurrence = t_mailbox_folder_flags.occurrence + 1;
        END LOOP;
    END IF;

    RETURN NEW;
END;
$$;


CREATE OR REPLACE FUNCTION fct_mailbox_record_flag_truncate() RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    TRUNCATE t_mailbox_folder_flags;
    RETURN NEW;
END;
$$;

-- Auto-removal of flags with no occurences
CREATE OR REPLACE FUNCTION fct_mailbox_folder_flags_remove() RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM t_mailbox_folder_flags WHERE container_id = NEW.container_id AND flag = NEW.flag;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trigger_mailbox_folder_flags_zero AFTER UPDATE ON t_mailbox_folder_flags
    FOR EACH ROW
    WHEN (NEW.occurrence <= 0)
    EXECUTE PROCEDURE fct_mailbox_folder_flags_remove();

-- a non removed record was added
CREATE TRIGGER
    trigger_mailbox_record_flag_inserted AFTER INSERT ON t_mailbox_record
    FOR EACH ROW
    WHEN (
        NEW.system_flags & (1 << 31) = 0
        AND
        NEW.other_flags IS NOT NULL
        AND
        array_length(NEW.other_flags, 1) > 0
    )
    EXECUTE PROCEDURE fct_mailbox_record_flag_update();

-- a record was updated (record expunged, or flags updated)
CREATE TRIGGER
    trigger_mailbox_record_flag_updated AFTER UPDATE ON t_mailbox_record
    FOR EACH ROW
    WHEN (
        NEW.other_flags IS DISTINCT FROM OLD.other_flags
        OR
        ((OLD.system_flags & (1 << 31)) = 0 AND (NEW.system_flags & (1 << 31)) = (1 << 31))
    )
    EXECUTE PROCEDURE fct_mailbox_record_flag_update();

-- a record was updated (record removed, or flags updated)
CREATE TRIGGER
    trigger_mailbox_record_flag_deleted AFTER DELETE ON t_mailbox_record
    FOR EACH ROW
    WHEN (OLD.other_flags IS NOT NULL)
    EXECUTE PROCEDURE fct_mailbox_record_flag_update();

-- the record table was truncated
CREATE TRIGGER
    trigger_mailbox_record_flag_truncate AFTER TRUNCATE ON t_mailbox_record
    FOR EACH STATEMENT
    EXECUTE PROCEDURE fct_mailbox_record_flag_truncate();
