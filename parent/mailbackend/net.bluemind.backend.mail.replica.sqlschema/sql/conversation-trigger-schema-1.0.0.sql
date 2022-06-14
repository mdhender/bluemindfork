
CREATE OR REPLACE VIEW view_conversation_get_message_body_data AS 
    SELECT 
        REGEXP_REPLACE(subject, '^([\W_]*|re\s*:)+', '', 'i') as subject, address as sender, size, guid
    FROM 
        t_message_body 
    CROSS JOIN LATERAL 
        jsonb_to_recordset(recipients) AS rcpt(kind TEXT, address TEXT)
    WHERE 
        kind = 'Originator';

CREATE OR REPLACE FUNCTION fct_conversation_get_first_mailrecord(p_containerid integer, p_conversationid bigint) RETURNS RECORD
  LANGUAGE plpgsql
AS $$
DECLARE
    FIRST record;
    BODY record;
    DATA record;
BEGIN
        SELECT 
            message_body_guid, internal_date 
        FROM 
            t_mailbox_record 
        INTO 
            FIRST
        WHERE 
            container_id = p_containerid
        AND 
            conversation_id = p_conversationid
        AND
            system_flags::bit(32) & 4::bit(32) = 0::bit(32)
        ORDER BY 
            internal_date
        LIMIT 1;

        SELECT 
            subject, sender, size 
        FROM
            view_conversation_get_message_body_data
        WHERE 
            guid = FIRST.message_body_guid
        INTO BODY;

        SELECT 
            BODY.subject as subject, BODY.sender as sender, BODY.size as size, FIRST.internal_date as date
        INTO DATA;

        RETURN DATA ;
END;
$$;

CREATE OR REPLACE VIEW view_conversation_get_mailrecord_data AS 
    SELECT 
      container_id, 
      conversation_id, 
      bit_OR(system_flags) & 2 = 2 as flagged,
      bit_OR(-(system_flags + 1)) & 16 = 16 as unseen,
      max(internal_date) as date,
      min(internal_date) as first
    FROM
      t_mailbox_record 
    WHERE
      system_flags::bit(32) & 4::bit(32) = 0::bit(32)
    GROUP BY container_id, conversation_id;

CREATE OR REPLACE VIEW view_conversation_get_conversation AS 
     SELECT 
        folder_id, conversation_id, date, first, size, unseen, flagged 
    FROM 
        v_conversation_by_folder;

CREATE OR REPLACE VIEW view_conversation_get_biggest_body_size AS 
    SELECT 
        container_id, conversation_id, size 
    FROM 
        t_mailbox_record 
    JOIN 
        t_message_body on guid = message_body_guid
    WHERE
        system_flags::bit(32) & 4::bit(32) = 0::bit(32)
    ORDER BY 
        size DESC
    LIMIT 1;


CREATE OR REPLACE FUNCTION v_conversation_by_folder_add() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
DECLARE
  body record;
BEGIN
    SELECT 
        subject, sender, size
    FROM 
        view_conversation_get_message_body_data
    WHERE
        guid = NEW.message_body_guid
    INTO body;
    
    INSERT INTO
      v_conversation_by_folder (folder_id, conversation_id, date, first, size, subject, sender, unseen, flagged)
    VALUES
      (NEW.container_id, NEW.conversation_id, NEW.internal_date, NEW.internal_date, body.size, body.subject, body.sender, NEW.system_flags & 16 != 16,  NEW.system_flags & 2 = 2)
    ON CONFLICT (folder_id, conversation_id) DO UPDATE
      SET
        date = (select greatest(v_conversation_by_folder.date, EXCLUDED.date)),
        first = (select LEAST(v_conversation_by_folder.first, EXCLUDED.first)),
        size = (select greatest(v_conversation_by_folder.size, EXCLUDED.size)),
        subject = (CASE WHEN EXCLUDED.first < v_conversation_by_folder.first THEN body.subject ELSE v_conversation_by_folder.subject END), 
        sender = (CASE WHEN EXCLUDED.first < v_conversation_by_folder.first THEN body.sender ELSE v_conversation_by_folder.sender END),
        unseen = (EXCLUDED.unseen OR v_conversation_by_folder.unseen),
        flagged = (EXCLUDED.flagged OR v_conversation_by_folder.flagged);

    return NEW;
END;
$$;

CREATE OR REPLACE FUNCTION v_conversation_by_folder_update() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
DECLARE
    EXISTING record;
    CURRENT record;
    BODY record;
    OLD_BODY_SIZE integer;

    new_sender TEXT := NULL;
    new_subject TEXT := NULL;
    new_size INTEGER := NULL;
 
    new_first timestamp without time zone := NULL;
    new_date timestamp without time zone := NULL;
    
    new_unseen BOOLEAN := NULL;
    new_flagged BOOLEAN := NULL;
BEGIN
    SELECT 
        date, first, size, unseen, flagged 
    FROM 
        view_conversation_get_conversation
    WHERE 
        folder_id = OLD.container_id
    AND 
        conversation_id = OLD.conversation_id
    INTO CURRENT;

    -- BODY has changed (SIZE migth have become bigger) OR MESSAGE date might be the new first MESSAGE
    IF (OLD.message_body_guid != NEW.message_body_guid OR (OLD.internal_date != CURRENT.first AND NEW.internal_date < CURRENT.first)) THEN
        SELECT 
            subject, sender, size
        FROM 
            view_conversation_get_message_body_data
        WHERE
            guid = NEW.message_body_guid
        INTO BODY;

        IF NEW.internal_date <= CURRENT.first THEN
            new_sender = BODY.sender;
            new_subject = BODY.subject;
        END IF;
      
        IF BODY.size > CURRENT.size THEN
            new_size = BODY.size;
        
        ELSIF OLD.message_body_guid != NEW.message_body_guid AND BODY.size < CURRENT.size THEN
            SELECT 
                size
            FROM 
                view_conversation_get_message_body_data
            WHERE
                guid = OLD.message_body_guid
            INTO OLD_BODY_SIZE;

            IF OLD_BODY_SIZE IS NULL OR OLD_BODY_SIZE = CURRENT.size THEN
                SELECT size 
                FROM view_conversation_get_biggest_body_size 
                WHERE container_id = OLD.container_id
                AND conversation_id = OLD.conversation_id
                INTO new_size;
            END IF;
        
        END IF;

    -- Message might not be the first one anymore => must find new first message
    ELSIF (OLD.internal_date = CURRENT.first AND NEW.internal_date > OLD.internal_date) THEN

        SELECT 
            subject, sender, size, date 
        FROM 
            fct_conversation_get_first_mailrecord(OLD.container_id, OLD.conversation_id)
            as (subject text, sender text, size integer, date timestamp)
        INTO BODY;

        new_sender = BODY.sender;
        new_subject = BODY.subject;
        new_first = BODY.date;

        IF BODY.size > CURRENT.size THEN
            new_size = BODY.size;
        END IF;

    END IF;

    -- Message might not be the last one anymore => must find the last message of conversation
    -- OR 
    -- Message is not unseen or flagged anymore => must if there is an unseed or a flagged message in conversation.
    IF (OLD.system_flags & 16 = 0 AND NEW.system_flags & 16 = 16) 
    OR (OLD.system_flags & 2 = 2 AND NEW.system_flags & 2 = 0)
    OR (OLD.internal_date = CURRENT.date AND NEW.internal_date < OLD.internal_date) THEN
        
        SELECT  
            flagged, unseen, date, first 
        FROM 
            view_conversation_get_mailrecord_data
        WHERE
            container_id = OLD.container_id
        AND
            conversation_id = OLD.conversation_id 
        INTO EXISTING;

        new_unseen = EXISTING.unseen;
        new_flagged = EXISTING.flagged;
        new_date = EXISTING.date;
    
    -- Message was not the last message of the conversation but is now
    -- OR
    -- Message is unseen or flagged and conversation is not
    ELSE
        -- seen -> unseen and current is seen ==> UNSEEN
        IF (NEW.system_flags & 16 != 16) AND NOT CURRENT.unseen THEN
            new_unseen = true;
        END IF;
        -- not flagged -> flagged and current is not flagged ==> FLAGGED
        IF (NEW.system_flags & 2 = 2) AND NOT CURRENT.flagged THEN
            new_flagged = true;
        END IF;
        IF (NEW.internal_date > CURRENT.date) THEN
            new_date = NEW.internal_date;
        END IF;
    END IF;

    IF  new_sender IS NOT NULL 
        OR new_subject IS NOT NULL
        OR new_size IS NOT NULL
        OR new_first IS NOT NULL
        OR new_date IS NOT NULL
        OR new_unseen IS NOT NULL
        OR new_flagged IS NOT NULL THEN

        UPDATE v_conversation_by_folder
        SET 
            date = COALESCE(new_date, v_conversation_by_folder.date),
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



CREATE OR REPLACE FUNCTION v_conversation_by_folder_remove() RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
DECLARE
  EXISTING record;
  CURRENT record;
  BODY record;
  OLD_BODY_SIZE integer;

  new_sender TEXT := NULL;
  new_subject TEXT := NULL;
  new_size INTEGER := NULL;
  
  new_first timestamp without time zone := NULL;
  new_date timestamp without time zone := NULL;
  
  new_unseen BOOLEAN := NULL;
  new_flagged BOOLEAN := NULL;
BEGIN
    SELECT 
        date, first, size, unseen, flagged 
    FROM 
        view_conversation_get_conversation
    WHERE 
        folder_id = OLD.container_id
    AND 
        conversation_id = OLD.conversation_id
    INTO CURRENT;

    -- deleted record was unseen OR flagged
    IF (OLD.system_flags & 16 != 16) OR (OLD.system_flags & 2 = 2) OR OLD.internal_date = CURRENT.date THEN
        SELECT  
            flagged, unseen, date, first 
        FROM 
            view_conversation_get_mailrecord_data
        WHERE
            container_id = OLD.container_id
        AND
            conversation_id = OLD.conversation_id 
        INTO EXISTING;

        IF (EXISTING IS NULL) THEN
            DELETE FROM v_conversation_by_folder WHERE folder_id = OLD.container_id AND conversation_id = OLD.conversation_id;
            RETURN NULL;
        ELSE
            new_unseen = EXISTING.unseen;
            new_flagged = EXISTING.flagged;
            new_date = EXISTING.date;
        END IF;
    END IF;

    -- deleted record was the first
    IF OLD.internal_date = CURRENT.first THEN
        SELECT 
            subject, sender, size, date 
        FROM 
            fct_conversation_get_first_mailrecord(OLD.container_id, OLD.conversation_id)
            as (subject text, sender text, size integer, date timestamp)
        INTO BODY;

        new_sender = BODY.sender;
        new_subject = BODY.subject;
        new_first = BODY.date;

        IF BODY.size > CURRENT.size THEN
            new_size = BODY.size;
        END IF;
    
    END IF;

    -- deleted record has the bigger body size
    SELECT 
        size
    FROM 
        view_conversation_get_message_body_data
    WHERE
        guid = OLD.message_body_guid
    INTO OLD_BODY_SIZE;

    IF OLD_BODY_SIZE IS NULL OR OLD_BODY_SIZE = CURRENT.size THEN
        SELECT size 
        FROM view_conversation_get_biggest_body_size 
        WHERE container_id = OLD.container_id
        AND conversation_id = OLD.conversation_id
        INTO new_size;
    END IF;
    

    IF  new_sender IS NOT NULL 
        OR new_subject IS NOT NULL
        OR new_size IS NOT NULL
        OR new_first IS NOT NULL
        OR new_date IS NOT NULL
        OR new_unseen IS NOT NULL
        OR new_flagged IS NOT NULL THEN

        UPDATE v_conversation_by_folder
        SET 
            date = COALESCE(new_date, v_conversation_by_folder.date),
            unseen = COALESCE(new_unseen, v_conversation_by_folder.unseen) ,
            flagged = COALESCE(new_flagged, v_conversation_by_folder.flagged), 
            first = COALESCE(new_first, v_conversation_by_folder.first),
            size = COALESCE(new_size, v_conversation_by_folder.size),
            subject = COALESCE(new_subject, v_conversation_by_folder.subject), 
            sender = COALESCE(new_sender, v_conversation_by_folder.sender)
        WHERE folder_id = OLD.container_id
        AND conversation_id  = OLD.conversation_id; 

    END IF;

    RETURN NULL;
END;
$$;

-------------------------------------------------
CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_insert
  AFTER INSERT
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (NEW.conversation_id IS NOT NULL AND NEW.system_flags & 4 = 0)
  EXECUTE PROCEDURE v_conversation_by_folder_add();


CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_undelete
  AFTER UPDATE
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (NEW.conversation_id IS NOT NULL  AND OLD.system_flags & 4 = 4 AND NEW.system_flags & 4 = 0)
  EXECUTE PROCEDURE v_conversation_by_folder_add();



CREATE OR REPLACE TRIGGER virtual_conversation_by_folder_update
  AFTER UPDATE
  ON t_mailbox_record
  FOR EACH ROW
  -- system_flags 4 => DELETED (MailboxItemFlag.java:System)
  WHEN (
    OLD.conversation_id IS NOT NULL AND OLD.system_flags & 4 = 0 AND NEW.system_flags & 4 = 0 
    AND (NEW.internal_date != OLD.internal_date OR NEW.system_flags != OLD.system_flags OR OLD.message_body_guid != NEW.message_body_guid)
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