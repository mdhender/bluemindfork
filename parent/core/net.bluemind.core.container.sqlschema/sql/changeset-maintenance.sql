DO LANGUAGE plpgsql 
$$
DECLARE
  partition TEXT;
  partition_count INTEGER;
BEGIN
  SELECT INTO partition_count COALESCE(current_setting('bm.changeset_partitions', true)::integer, 256);

  FOR partition_key IN 0..(partition_count-1)
  LOOP
    partition := 't_container_changeset_' || partition_key;
    RAISE NOTICE 'VACUUM CHANGESET PARTITION %...', partition;      
    EXECUTE 'DELETE FROM ' || partition || ' a USING ' || partition || ' b WHERE a.item_id = b.item_id AND b.type = 2 AND b.date < now() - ''2month''::interval' ;
  END LOOP;
END;
$$;

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
    RAISE NOTICE 'CLUSTERING CHANGESET PARTITION %...', partition;
    EXECUTE 'CLUSTER  ' || partition || ' USING ' || idx ||';';
  END LOOP;
END;
$$;
    
