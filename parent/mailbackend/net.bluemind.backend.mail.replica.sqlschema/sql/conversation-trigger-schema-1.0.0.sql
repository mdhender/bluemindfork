CREATE OR REPLACE VIEW view_conversation_get_message_body_data AS
    SELECT
        guid,
        regexp_replace(unaccent(subject), '^([\W]*|re\s*:)+', '', 'i') AS subject,
        jsonb_path_query(recipients, '$[*] ? (@.kind == "Originator")') ->> 'address' AS sender,
        size
    FROM t_message_body;

CREATE OR REPLACE VIEW view_conversation_by_mailbox_record AS
    SELECT
        container_id,
        conversation_id,
        bit_OR(system_flags) & 2 = 2 AS flagged,
        bit_OR(-(system_flags + 1)) & 16 = 16 AS unseen,
        max(internal_date) AS "date",
        min(internal_date) AS first
    FROM t_mailbox_record
    WHERE system_flags::bit(32) & 4::bit(32) = 0::bit(32)
    GROUP BY container_id, conversation_id;

CREATE OR REPLACE VIEW view_conversation_get_conversation AS
    SELECT folder_id, conversation_id, "date", first, size, unseen, flagged
    FROM v_conversation_by_folder;


CREATE OR REPLACE FUNCTION fct_conversation_get_first_mailrecord(p_containerid integer, p_conversationid bigint)
    RETURNS RECORD
    LANGUAGE plpgsql
AS $$
DECLARE
    FIRST record;
    BODY record;
    DATA record;
BEGIN
    SELECT message_body_guid, internal_date
    FROM t_mailbox_record
    INTO FIRST
    WHERE container_id = p_containerid
    AND conversation_id = p_conversationid
    AND system_flags::bit(32) & 4::bit(32) = 0::bit(32)
    ORDER BY internal_date ASC, item_id ASC
    LIMIT 1;

    SELECT subject, sender, size
    FROM view_conversation_get_message_body_data
    WHERE guid = FIRST.message_body_guid
    INTO BODY;
    SELECT
        BODY.subject AS subject,
        BODY.sender AS sender,
        BODY.size AS size,
        FIRST.internal_date AS "date"
    INTO DATA;
    RETURN DATA;
END;
$$;


CREATE OR REPLACE FUNCTION fct_conversation_get_last_mailrecord(p_containerid integer, p_conversationid bigint)
    RETURNS RECORD
    LANGUAGE plpgsql
AS $$
DECLARE
    LAST record;
    BODY record;
    DATA record;
BEGIN
    SELECT message_body_guid, internal_date
    FROM t_mailbox_record
    INTO LAST
    WHERE container_id = p_containerid
    AND conversation_id = p_conversationid
    AND system_flags::bit(32) & 4::bit(32) = 0::bit(32)
    ORDER BY internal_date DESC, item_id DESC
    LIMIT 1;

    SELECT subject, sender, size
    FROM view_conversation_get_message_body_data
    WHERE guid = LAST.message_body_guid
    INTO BODY;
    SELECT
        BODY.subject AS subject,
        BODY.sender AS sender,
        BODY.size AS size,
        LAST.internal_date AS "date"
    INTO DATA;
    RETURN DATA;
END;
$$;

CREATE OR REPLACE FUNCTION v_conversation_by_folder_add(new_record t_mailbox_record)
    RETURNS t_mailbox_record LANGUAGE plpgsql AS $$
DECLARE
    body record;
BEGIN
    SELECT subject, sender, size
    FROM view_conversation_get_message_body_data
    WHERE guid = new_record.message_body_guid
    INTO body;

    -- The logic explained, we want:
    -- - first timestamp of a message in a conversation
    -- - the last sender
    -- - the FIRST subject, without "Re:" prefix
    -- - the biggest message size
    -- - know if a message is unseen, or flagged
    --
    -- EXCLUDED is the row we just tried to insert (so it matches the message we try to add)
    -- We also check if the sender / subject is non empty, avoiding to overwrite
    -- a perfectly good subject/sender by a bad one

    INSERT INTO v_conversation_by_folder
        (
            folder_id,
            conversation_id,
            "date",
            first,
            size,
            subject,
            sender,
            unseen,
            flagged
        )
    VALUES
        (
            new_record.container_id,
            new_record.conversation_id,
            new_record.internal_date,
            new_record.internal_date,
            body.size,
            body.subject,
            body.sender,
            new_record.system_flags & 16 != 16,
            new_record.system_flags & 2 = 2
        )
    ON CONFLICT (folder_id, conversation_id) DO UPDATE SET
        "date" = (select greatest(v_conversation_by_folder.date, EXCLUDED.date)),
        first = (select LEAST(v_conversation_by_folder.first, EXCLUDED.date)),
        size = (select greatest(v_conversation_by_folder.size, EXCLUDED.size)),
        subject = (
            CASE WHEN (
                EXCLUDED.date < v_conversation_by_folder.date
                AND COALESCE(TRIM(body.subject), '') <> ''
            ) THEN body.subject
            ELSE v_conversation_by_folder.subject
            END
        ),
        sender = (
            CASE WHEN (
                EXCLUDED.date >= v_conversation_by_folder.date
                AND COALESCE(TRIM(body.sender), '') <> ''
            ) THEN body.sender
            ELSE v_conversation_by_folder.sender
            END
        ),
        unseen = (EXCLUDED.unseen OR v_conversation_by_folder.unseen),
        flagged = (EXCLUDED.flagged OR v_conversation_by_folder.flagged);
    return new_record;
END;
$$;


CREATE OR REPLACE FUNCTION v_conversation_by_folder_add_trigger() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    ret t_mailbox_record;
BEGIN
    SELECT v_conversation_by_folder_add(NEW) INTO ret;
    RETURN ret;
END;
$$;

CREATE OR REPLACE FUNCTION v_conversation_by_folder_remove(old_record t_mailbox_record, new_record t_mailbox_record)
    RETURNS t_mailbox_record LANGUAGE plpgsql AS $$
DECLARE
    NEW_CONVERSATION record;
    CURRENT_CONVERSATION record;
    removed_body_size integer := NULL;

    new_sender TEXT := NULL;
    new_subject TEXT := NULL;
    new_size INTEGER := NULL;

    new_first timestamp without time zone := NULL;
    new_date timestamp without time zone := NULL;

    new_unseen BOOLEAN := NULL;
    new_flagged BOOLEAN := NULL;
BEGIN
    -- The trigger is plugged on AFTER DELETE and AFTER UPDATE, so if we query
    -- t_mailbox_record we get the updated row
    SELECT "date", first, size, unseen, flagged
    FROM v_conversation_by_folder
    WHERE folder_id = old_record.container_id
    AND conversation_id = old_record.conversation_id
    INTO CURRENT_CONVERSATION;

    SELECT size
    INTO removed_body_size
    FROM t_message_body
    WHERE guid = new_record.message_body_guid;

    -- deleted record was unseen OR flagged OR is the last one
    IF (
        old_record.system_flags & 16 != 16
        OR old_record.system_flags & 2 = 2
        OR old_record.internal_date = CURRENT_CONVERSATION.date
    ) THEN
        -- The logic here is the following:
        -- If we rebuild the conversation, we don't have any rows not deleted
        -- then the conversation does not exists anymore.
        -- Otherwise, just update the unseen / flagged and date fields at this stage
        SELECT flagged, unseen, "date", first
        FROM view_conversation_by_mailbox_record
        WHERE container_id = old_record.container_id
        AND conversation_id = old_record.conversation_id
        INTO NEW_CONVERSATION;

        IF (NEW_CONVERSATION IS NULL) THEN
            DELETE FROM v_conversation_by_folder
            WHERE folder_id = old_record.container_id
            AND conversation_id = old_record.conversation_id;
            -- We removed the conversation, no need to continue further
            RETURN NULL;
        ELSE
            new_unseen = NEW_CONVERSATION.unseen;
            new_flagged = NEW_CONVERSATION.flagged;
            new_date = NEW_CONVERSATION.date;
        END IF;
    END IF;

    -- deleted record was the first, we need to find the new first record
    IF old_record.internal_date = CURRENT_CONVERSATION.first THEN
        SELECT subject, date
        INTO new_subject, new_first
        FROM fct_conversation_get_first_mailrecord(old_record.container_id, old_record.conversation_id)
                AS (subject text, sender text, size integer, date timestamp);
    END IF;

    -- deleted record was the latest one, so we want to update the sender then
    IF old_record.internal_date = CURRENT_CONVERSATION.date THEN
        SELECT sender
        INTO new_sender
        FROM fct_conversation_get_last_mailrecord(old_record.container_id, old_record.conversation_id)
                AS (subject text, sender text, size integer, date timestamp);
    END IF;

    -- deleted record had the biggest size of the conversation
    IF (removed_body_size IS NOT NULL AND removed_body_size > CURRENT_CONVERSATION.size) THEN
        SELECT max(size) AS size
        INTO new_size
        FROM t_mailbox_record
        JOIN t_message_body ON (guid = message_body_guid)
        WHERE system_flags::bit(32) & 4::bit(32) = 0::bit(32)
        AND container_id = old_record.container_id
        AND conversation_id = old_record.conversation_id
        GROUP BY container_id, conversation_id;
    END IF;

    IF (
        new_sender IS NOT NULL
        OR new_subject IS NOT NULL
        OR new_size IS NOT NULL
        OR new_first IS NOT NULL
        OR new_date IS NOT NULL
        OR new_unseen IS NOT NULL
        OR new_flagged IS NOT NULL
    ) THEN
        UPDATE v_conversation_by_folder
        SET
            date = COALESCE(new_date, v_conversation_by_folder.date),
            unseen = COALESCE(new_unseen, v_conversation_by_folder.unseen) ,
            flagged = COALESCE(new_flagged, v_conversation_by_folder.flagged),
            first = COALESCE(new_first, v_conversation_by_folder.first),
            size = COALESCE(new_size, v_conversation_by_folder.size),
            subject = COALESCE(new_subject, v_conversation_by_folder.subject),
            sender = COALESCE(new_sender, v_conversation_by_folder.sender)
        WHERE folder_id = old_record.container_id
        AND conversation_id  = old_record.conversation_id;
    END IF;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION v_conversation_by_folder_remove_trigger() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM v_conversation_by_folder_remove(OLD, NEW);
    RETURN NULL;
END;
$$;


CREATE OR REPLACE FUNCTION v_conversation_by_folder_update() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    EXISTING record;
    CURRENT_CONVERSATION record;
    NEW_RECORD_BODY record;

    new_sender TEXT := NULL;
    new_subject TEXT := NULL;
    new_size INTEGER := NULL;

    new_first timestamp without time zone := NULL;
    new_date timestamp without time zone := NULL;

    new_unseen BOOLEAN := NULL;
    new_flagged BOOLEAN := NULL;
BEGIN
    SELECT "date", first, size, unseen, flagged
    FROM v_conversation_by_folder
    WHERE folder_id = OLD.container_id
    AND conversation_id = OLD.conversation_id
    INTO CURRENT_CONVERSATION;

    -- body has changed, we may update the subject / size / sender depending on the case
    IF OLD.message_body_guid != NEW.message_body_guid THEN
        SELECT subject, sender, size
        FROM view_conversation_get_message_body_data
        WHERE guid = NEW.message_body_guid
        INTO NEW_RECORD_BODY;
        -- We are only interrested if the new body size is bigger than the conversation
        IF NEW_RECORD_BODY.size > CURRENT_CONVERSATION.size THEN
            new_size = NEW_RECORD_BODY.size;
        END IF;
        -- but, because the body changed, we also might have changed subject
        -- and sender
        IF OLD.internal_date = CURRENT_CONVERSATION.date THEN
            -- We may be the last message in the conversation, so we must update
            -- the sender
            new_sender = NEW_RECORD_BODY.sender;
        END IF;
        IF OLD.internal_date = CURRENT_CONVERSATION.first THEN
            -- We may be the first message in the conversation, so we must update
            -- the subject
            new_subject = NEW_RECORD_BODY.subject;
        END IF;
    END IF;

    -- the date changed, we may re-organize the conversation then
    IF OLD.internal_date != NEW.internal_date THEN
        -- We update the date to something "newer"
        IF NEW.internal_date > OLD.internal_date THEN
            -- we are the last message now
            IF NEW.internal_date > CURRENT_CONVERSATION.date THEN
                -- We need to update the last sender and date of the conversation
                SELECT NEW.internal_date, subject
                INTO new_date, new_subject
                FROM view_conversation_get_message_body_data
                WHERE guid = NEW.message_body_guid;
            END IF;

            -- We may not be the first message of the conversation anymore
            IF OLD.internal_date = CURRENT_CONVERSATION.first THEN
                SELECT subject, "date"
                INTO new_subject, new_first
                FROM
                    fct_conversation_get_first_mailrecord(OLD.container_id, OLD.conversation_id)
                    AS (subject text, sender text, size integer, "date" timestamp);
            END IF;

        -- we update the date to something "older"
        ELSIF NEW.internal_date < OLD.internal_date THEN
            -- We now are the first message in the conversation, update first date and subject
            IF NEW.internal_date < CURRENT_CONVERSATION.first THEN
                SELECT NEW.internal_date, subject
                INTO new_first, new_subject
                FROM view_conversation_get_message_body_data
                WHERE guid = NEW.message_body_guid;
            END IF;

            -- we were the last message in the conversation, update the sender and date
            IF OLD.internal_date = CURRENT_CONVERSATION.date THEN
                SELECT sender, "date"
                INTO new_sender, new_date
                FROM
                    fct_conversation_get_last_mailrecord(OLD.container_id, OLD.conversation_id)
                    AS (subject text, sender text, size integer, "date" timestamp);
            END IF;
        END IF;
    END IF;

    -- system_flags & 16 = 16 => message is seen
    -- system_flags & 2 = 2 => message is flagged
    -- If the old messages was not seen, but now is
    -- Or the messages was flagged, but is not anymore
    IF (
        (OLD.system_flags & 16 = 0 AND NEW.system_flags & 16 = 16)
        OR (OLD.system_flags & 2 = 2 AND NEW.system_flags & 2 = 0)
    ) THEN
        -- rebuild the bit_or flags on the whole conversation
        SELECT flagged, unseen, date, first
        INTO new_flagged, new_unseen, new_date
        FROM view_conversation_by_mailbox_record
        WHERE container_id = OLD.container_id
        AND conversation_id = OLD.conversation_id;

    -- Message is unseen or flagged and conversation is not
    ELSE
        -- seen -> unseen and current is seen ==> UNSEEN
        IF (NEW.system_flags & 16 != 16) AND NOT CURRENT_CONVERSATION.unseen THEN
            new_unseen = true;
        END IF;
        -- not flagged -> flagged and current is not flagged ==> FLAGGED
        IF (NEW.system_flags & 2 = 2) AND NOT CURRENT_CONVERSATION.flagged THEN
            new_flagged = true;
        END IF;
    END IF;

    IF (
        new_sender IS NOT NULL
        OR new_subject IS NOT NULL
        OR new_size IS NOT NULL
        OR new_first IS NOT NULL
        OR new_date IS NOT NULL
        OR new_unseen IS NOT NULL
        OR new_flagged IS NOT NULL
    ) THEN
        UPDATE v_conversation_by_folder
        SET
            "date" = COALESCE(new_date, v_conversation_by_folder.date),
            unseen = COALESCE(new_unseen, v_conversation_by_folder.unseen) ,
            flagged = COALESCE(new_flagged, v_conversation_by_folder.flagged),
            first = COALESCE(new_first, v_conversation_by_folder.first),
            size = COALESCE(new_size, v_conversation_by_folder.size),
            subject = COALESCE(new_subject, v_conversation_by_folder.subject),
            sender = COALESCE(new_sender, v_conversation_by_folder.sender)
        WHERE folder_id = NEW.container_id
        AND conversation_id  = NEW.conversation_id;
    END IF;
    RETURN NEW;
END;
$$;


CREATE OR REPLACE FUNCTION v_conversation_by_folder_conversation_id_changed() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    ret t_mailbox_record;
BEGIN
    PERFORM v_conversation_by_folder_remove(OLD, NEW);
    SELECT v_conversation_by_folder_add(NEW) INTO ret;
    RETURN ret;
END;
$$;

-------------------------------------------------
-- system_flags 4 => DELETED (MailboxItemFlag.java:System)
CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_insert
    AFTER INSERT ON t_mailbox_record FOR EACH ROW
    WHEN (
        -- conversation is set
        NEW.conversation_id IS NOT NULL
        -- message is not deleted
        AND NEW.system_flags & 4 = 0
    )
    EXECUTE PROCEDURE v_conversation_by_folder_add_trigger();

CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_undelete
    AFTER UPDATE ON t_mailbox_record FOR EACH ROW
    WHEN (
        -- conversation is set
        NEW.conversation_id IS NOT NULL
        -- previous version was deleted
        AND OLD.system_flags & 4 = 4
        -- new version is not deleted anymore
        AND NEW.system_flags & 4 = 0
    )
    EXECUTE PROCEDURE v_conversation_by_folder_add_trigger();

CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_update_create_conversation
    AFTER UPDATE ON t_mailbox_record FOR EACH ROW
    WHEN (
        OLD.conversation_id IS NOT NULL
        AND NEW.conversation_id IS NOT NULL
        AND NEW.system_flags & 4 = 0
    )
    EXECUTE PROCEDURE v_conversation_by_folder_add_trigger();

CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_update
    AFTER UPDATE ON t_mailbox_record FOR EACH ROW
    WHEN (
        -- we are interrested because we did something before
        OLD.conversation_id IS NOT NULL
        -- not a delete / undelete
        AND OLD.system_flags & 4 = 0
        AND NEW.system_flags & 4 = 0
        AND (
            -- Something interresting changed
            NEW.internal_date != OLD.internal_date
            OR NEW.system_flags != OLD.system_flags
            OR OLD.message_body_guid != NEW.message_body_guid
        )
    )
    EXECUTE PROCEDURE v_conversation_by_folder_update();

CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_remove
    AFTER UPDATE ON t_mailbox_record FOR EACH ROW
    WHEN (
        -- we are interrested because we did something before
        OLD.conversation_id IS NOT NULL
        -- message is now deleted
        AND OLD.system_flags & 4 = 0
        AND NEW.system_flags & 4 = 4
    )
    EXECUTE PROCEDURE v_conversation_by_folder_remove_trigger();

CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_expunge
    AFTER DELETE ON t_mailbox_record FOR EACH ROW
    WHEN (
        -- we are interrested because we did something before
        OLD.conversation_id IS NOT NULL
        -- and the message was not removed yet (direct remove from t_mailbox_record)
        AND OLD.system_flags & 4 = 0
    )
    EXECUTE PROCEDURE v_conversation_by_folder_remove_trigger();

CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_conversation_id_changed
    AFTER UPDATE ON t_mailbox_record FOR EACH ROW
    WHEN (
        -- we are interrested because we did something before
        OLD.conversation_id IS NOT NULL
        -- but the conversation id was changed
        AND OLD.conversation_id != NEW.conversation_id
    )
    EXECUTE PROCEDURE v_conversation_by_folder_conversation_id_changed();
