-- FILL FACTOR is used to help HOT updates to take place
CREATE TABLE v_container_item_counter (
    container_id integer PRIMARY KEY NOT NULL REFERENCES t_container(id) ON DELETE CASCADE,
    unseen_total bigint,
    unseen_visible bigint,
    total_visible bigint,
    total bigint
) WITH (fillfactor = 60);

-- NOTES:
-- & 1 = 1 << SEEN
-- & 2 = 2 << DELETED
CREATE OR REPLACE FUNCTION fct_container_item_counter_inserted() RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    current_count v_container_item_counter;
BEGIN
    SELECT INTO current_count * FROM v_container_item_counter WHERE container_id = NEW.container_id FOR NO KEY UPDATE;
    IF current_count IS NOT NULL THEN
        -- new item is not seen
        IF NEW.flags & 1 = 0 THEN
            current_count.unseen_total = current_count.unseen_total + 1;
        END IF;

        -- new item is not seen, not deleted
        IF NEW.flags & 1 = 0 AND NEW.flags & 2 = 0 THEN
            current_count.unseen_visible = current_count.unseen_visible + 1;
        END IF;
        
        -- new item not deleted
        IF NEW.flags & 2 = 0 THEN
            current_count.total_visible = current_count.total_visible + 1;
        END IF;

        current_count.total = current_count.total + 1;

        UPDATE v_container_item_counter
            SET
                unseen_total = current_count.unseen_total,
                unseen_visible = current_count.unseen_visible,
                total = current_count.total,
                total_visible = current_count.total_visible
        WHERE container_id = NEW.container_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION fct_container_item_counter_updated() RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    current_count v_container_item_counter;
    was_unseen bool;
    was_visible bool;
    is_unseen bool;
    is_visible bool;
BEGIN
    -- TRIGGER CONDITION: deleted or seen was changed
    SELECT INTO current_count * FROM v_container_item_counter WHERE container_id = NEW.container_id FOR NO KEY UPDATE;
    IF current_count IS NOT NULL THEN
        is_unseen := NEW.flags & 1 = 0;
        was_unseen := OLD.flags & 1 = 0;
        is_visible := NEW.flags & 2 = 0;
        was_visible := OLD.flags & 2 = 0;

        IF was_unseen != is_unseen THEN
            IF is_unseen THEN
                current_count.unseen_total = current_count.unseen_total + 1;
            ELSE
                current_count.unseen_total = current_count.unseen_total - 1;
            END IF;
        END IF;

        IF was_visible AND was_unseen AND NOT is_visible THEN
            current_count.unseen_visible = current_count.unseen_visible - 1;
        END IF;

        IF NOT was_visible AND is_visible AND is_unseen THEN
            current_count.unseen_visible = current_count.unseen_visible + 1;
        END IF;

        IF was_visible AND is_visible THEN
            IF was_unseen != is_unseen THEN
                IF is_unseen THEN
                    current_count.unseen_visible = current_count.unseen_visible + 1;
                ELSE
                    current_count.unseen_visible = current_count.unseen_visible - 1;
                END IF;
            END IF;
        END IF;


        IF was_visible != is_visible THEN
            IF is_visible THEN
                current_count.total_visible = current_count.total_visible + 1;
            ELSE
                current_count.total_visible = current_count.total_visible - 1;
            END IF;
        END IF;

        UPDATE v_container_item_counter
            SET
                unseen_total = current_count.unseen_total,
                unseen_visible = current_count.unseen_visible,
                total_visible = current_count.total_visible
        WHERE container_id = NEW.container_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION fct_container_item_counter_deleted() RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    current_count v_container_item_counter;
    was_unseen bool;
    was_visible bool;
BEGIN
    -- TRIGGER CONDITION: item deleted
    SELECT INTO current_count * FROM v_container_item_counter WHERE container_id = OLD.container_id FOR NO KEY UPDATE;
    IF current_count IS NOT NULL THEN
        was_unseen := OLD.flags & 1 = 0;
        was_visible := OLD.flags & 2 = 0;

        IF was_visible THEN
            IF was_unseen THEN
                current_count.unseen_visible = current_count.unseen_visible - 1;
            END IF;
            current_count.total_visible = current_count.total_visible - 1;
        END IF;

        current_count.total = current_count.total - 1;
        
        UPDATE v_container_item_counter
            SET
                unseen_total = current_count.unseen_total,
                unseen_visible = current_count.unseen_visible,
                total = current_count.total,
                total_visible = current_count.total_visible
        WHERE container_id = OLD.container_id;
    END IF;
    RETURN OLD;
END;
$$;

CREATE OR REPLACE FUNCTION fct_container_item_counter_truncated() RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    TRUNCATE v_container_item_counter;
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION fct_container_item_counter_container_created() RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO v_container_item_counter(container_id, unseen_total, unseen_visible, total_visible, total)
    VALUES (NEW.id, 0, 0, 0, 0) ON CONFLICT (container_id) DO NOTHING;
    RETURN NEW;
END;
$$;


-- new item created
CREATE OR REPLACE TRIGGER trigger_container_item_count_insert
    AFTER INSERT ON t_container_item
    FOR EACH ROW
    EXECUTE PROCEDURE fct_container_item_counter_inserted();

-- item is updated, and seen or deleted was changed
CREATE OR REPLACE TRIGGER trigger_container_item_count_updater
    AFTER UPDATE ON t_container_item
    FOR EACH ROW
    WHEN (
        (OLD.flags & 1 IS DISTINCT FROM NEW.flags & 1)
        OR (OLD.flags & 2 IS DISTINCT FROM NEW.flags & 2)
    )
    EXECUTE PROCEDURE fct_container_item_counter_updated();

-- item is deleted, but was not deleted and not seen
CREATE OR REPLACE TRIGGER trigger_container_item_counter_deleted
    AFTER DELETE ON t_container_item
    FOR EACH ROW
    EXECUTE PROCEDURE fct_container_item_counter_deleted();

-- item table was truncated
CREATE OR REPLACE TRIGGER trigger_container_item_counter_truncate
    AFTER TRUNCATE ON t_container_item FOR EACH STATEMENT
    EXECUTE PROCEDURE fct_container_item_counter_truncated();

-- container is created
CREATE OR REPLACE TRIGGER trigger_container_item_counter_container_created
    AFTER INSERT ON t_container FOR EACH ROW
    EXECUTE PROCEDURE fct_container_item_counter_container_created();

