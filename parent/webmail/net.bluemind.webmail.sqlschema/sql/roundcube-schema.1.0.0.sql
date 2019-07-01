CREATE TABLE rc_cache (
    cache_id integer DEFAULT nextval(('rc_cache_ids'::text)::regclass) NOT NULL,
    user_id integer NOT NULL,
    cache_key character varying(128) DEFAULT ''::character varying NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    data text NOT NULL
);

CREATE SEQUENCE rc_cache_ids
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;
    
CREATE TABLE rc_cache_index (
    user_id integer NOT NULL,
    mailbox character varying(255) NOT NULL,
    changed timestamp with time zone DEFAULT now() NOT NULL,
    valid smallint DEFAULT 0 NOT NULL,
    data text NOT NULL
);

CREATE TABLE rc_cache_messages (
    user_id integer NOT NULL,
    mailbox character varying(255) NOT NULL,
    uid integer NOT NULL,
    changed timestamp with time zone DEFAULT now() NOT NULL,
    data text NOT NULL,
    flags integer DEFAULT 0 NOT NULL
);

CREATE TABLE rc_cache_thread (
    user_id integer NOT NULL,
    mailbox character varying(255) NOT NULL,
    changed timestamp with time zone DEFAULT now() NOT NULL,
    data text NOT NULL
);

CREATE SEQUENCE rc_contact_ids
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE rc_contactgroupmembers (
    contactgroup_id integer NOT NULL,
    contact_id integer NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE rc_contactgroups (
    contactgroup_id integer DEFAULT nextval(('rc_contactgroups_ids'::text)::regclass) NOT NULL,
    user_id integer NOT NULL,
    changed timestamp with time zone DEFAULT now() NOT NULL,
    del smallint DEFAULT 0 NOT NULL,
    name character varying(128) DEFAULT ''::character varying NOT NULL
);

CREATE SEQUENCE rc_contactgroups_ids
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;
    
CREATE TABLE rc_contacts (
    contact_id integer DEFAULT nextval(('rc_contact_ids'::text)::regclass) NOT NULL,
    user_id integer NOT NULL,
    changed timestamp with time zone DEFAULT now() NOT NULL,
    del smallint DEFAULT 0 NOT NULL,
    name character varying(128) DEFAULT ''::character varying NOT NULL,
    email text DEFAULT ''::character varying NOT NULL,
    firstname character varying(128) DEFAULT ''::character varying NOT NULL,
    surname character varying(128) DEFAULT ''::character varying NOT NULL,
    vcard text,
    words text
);

CREATE TABLE rc_dictionary (
    user_id integer,
    language character varying(5) NOT NULL,
    data text NOT NULL
);

CREATE TABLE rc_identities (
    identity_id integer DEFAULT nextval(('rc_identity_ids'::text)::regclass) NOT NULL,
    user_id integer NOT NULL,
    changed timestamp with time zone DEFAULT now() NOT NULL,
    del smallint DEFAULT 0 NOT NULL,
    standard smallint DEFAULT 0 NOT NULL,
    name character varying(128) NOT NULL,
    organization character varying(128),
    email character varying(128) NOT NULL,
    "reply-to" character varying(128),
    bcc character varying(128),
    signature text,
    html_signature integer DEFAULT 0 NOT NULL
);

CREATE SEQUENCE rc_identity_ids
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;
    
CREATE SEQUENCE rc_search_ids
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

CREATE TABLE rc_searches (
    search_id integer DEFAULT nextval(('search_ids'::text)::regclass) NOT NULL,
    user_id integer NOT NULL,
    type smallint DEFAULT 0 NOT NULL,
    name character varying(128) NOT NULL,
    data text NOT NULL
);

CREATE TABLE rc_session (
    sess_id character varying(128) DEFAULT ''::character varying NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    changed timestamp with time zone DEFAULT now() NOT NULL,
    ip character varying(41) NOT NULL,
    vars text NOT NULL
);

CREATE SEQUENCE rc_user_ids
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;
    
CREATE TABLE rc_users (
    user_id integer DEFAULT nextval(('rc_user_ids'::text)::regclass) NOT NULL,
    username character varying(128) DEFAULT ''::character varying NOT NULL,
    mail_host character varying(128) DEFAULT ''::character varying NOT NULL,
    alias character varying(128) DEFAULT ''::character varying NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    last_login timestamp with time zone,
    language character varying(5),
    preferences text DEFAULT ''::text NOT NULL
);