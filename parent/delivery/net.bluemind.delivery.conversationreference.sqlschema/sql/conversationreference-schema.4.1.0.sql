CREATE TABLE t_conversationreference (
  id bigint GENERATED ALWAYS AS IDENTITY,
  message_id_hash bigint NOT NULL,
  mailbox_id integer NOT NULL,
  conversation_id bigint NOT NULL,
  created timestamp without time zone default now(),
  UNIQUE(mailbox_id, message_id_hash)
) PARTITION BY HASH (mailbox_id);

CREATE INDEX
  idx_t_conversationreference_mailbox_id_message_id_hash
  ON t_conversationreference(mailbox_id, message_id_hash)
  INCLUDE(conversation_id);
CREATE INDEX
  idx_t_conversationreference_date
  ON t_conversationreference(created);

DO LANGUAGE plpgsql
$$
DECLARE
  partition TEXT;
  idx TEXT;
  partition_count INTEGER;
BEGIN
  SELECT INTO partition_count COALESCE(current_setting('bm.conversationreference_partitions',true)::integer,256);
  FOR partition_key IN 0..(partition_count-1) LOOP
    partition := 't_conversationreference_' || partition_key;
    idx := partition || '_mailbox_id_message_id_hash';
    RAISE NOTICE 'CREATING PARTITION %...', partition;
    EXECUTE 'CREATE TABLE ' || partition || ' PARTITION OF t_conversationreference FOR VALUES WITH (MODULUS ' || partition_count || ', REMAINDER ' || partition_key || ');';
    END LOOP;
END;
$$;

