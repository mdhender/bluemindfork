CREATE TYPE enum_wipe_mode AS ENUM (
    'AccountOnlyRemoteWipe',
    'RemoteWipe'
);

CREATE TABLE t_eas_device (
    identifier character varying(255) NOT NULL,
    owner text,
    type character varying(64) NOT NULL,
    
    wipe_date timestamp without time zone,
    wipe_user text,
    unwipe_date timestamp without time zone,
    
    unwipe_user text,
    wipe boolean DEFAULT false,
    
    partnership boolean DEFAULT false,
    policy integer,
    
    last_sync timestamp without time zone,
    
    protocol_version numeric(3, 1) default 00.0,
    
    wipe_mode enum_wipe_mode,
    
    item_id bigint references t_container_item(id) on delete cascade,
    
    unique(identifier, owner)
);

CREATE INDEX idx_eas_device_item_id ON t_eas_device(item_id);