DO LANGUAGE plpgsql 
$$
  DECLARE
    partition TEXT;
    idx TEXT;
    iter RECORD;
  BEGIN
    FOR iter IN SELECT id FROM t_container ORDER BY id
    LOOP
      partition := 't_container_changeset_' || iter.id;
      idx := partition || '_item_id_version_idx';
      RAISE NOTICE 'CLUSTERING CHANGESET PARTITION %...', partition;
      EXECUTE 'CLUSTER  ' || partition || ' USING ' || idx ||';';
    END LOOP;
  END;
$$;
    
$$
  DECLARE
    partition TEXT;
    iter RECORD;
  BEGIN
    CREATE TEMPORARY TABLE tmp_deprecated_changeset (item_id integer, container_id integer);
    INSERT INTO tmp_deprecated_changeset
      SELECT item_id, container_id FROM t_container_changeset 
      WHERE type = 2 AND date < NOW() - INTERVAL '2 MONTHS';

    FOR iter IN SELECT container_id FROM tmp_deprecated_changeset GROUP BY container_id
    LOOP
      partition := 't_container_changeset_' || iter.container_id;
      RAISE NOTICE 'VACUUM CHANGESET PARTITION %...', partition;      
      EXECUTE 'DELETE FROM ' || partition || ' WHERE item_id IN 
        (SELECT item_id FROM tmp_deprecated_changeset WHERE container_id = ' || iter.container_id || ');';
    END LOOP;
    DROP TABLE tmp_deprecated_changeset;
  END;
$$;

