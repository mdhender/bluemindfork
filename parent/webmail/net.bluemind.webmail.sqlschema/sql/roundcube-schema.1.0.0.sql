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

ALTER TABLE ONLY public.rc_cache_index ADD CONSTRAINT rc_cache_index_pkey PRIMARY KEY (user_id, mailbox);
ALTER TABLE ONLY public.rc_cache_messages ADD CONSTRAINT rc_cache_messages_pkey PRIMARY KEY (user_id, mailbox, uid);
ALTER TABLE ONLY public.rc_cache_thread ADD CONSTRAINT rc_cache_thread_pkey PRIMARY KEY (user_id, mailbox);
ALTER TABLE ONLY public.rc_contactgroupmembers ADD CONSTRAINT rc_contactgroupmembers_pkey PRIMARY KEY (contactgroup_id, contact_id);
ALTER TABLE ONLY public.rc_contactgroups ADD CONSTRAINT rc_contactgroups_pkey PRIMARY KEY (contactgroup_id);
ALTER TABLE ONLY public.rc_contacts ADD CONSTRAINT rc_contacts_pkey PRIMARY KEY (contact_id);
ALTER TABLE ONLY public.rc_dictionary ADD CONSTRAINT rc_dictionary_user_id_language_key UNIQUE (user_id, language);
ALTER TABLE ONLY public.rc_identities ADD CONSTRAINT rc_identities_pkey PRIMARY KEY (identity_id);
ALTER TABLE ONLY public.rc_searches ADD CONSTRAINT rc_searches_pkey PRIMARY KEY (search_id);
ALTER TABLE ONLY public.rc_searches ADD CONSTRAINT rc_searches_user_id_key UNIQUE (user_id, type, name);
ALTER TABLE ONLY public.rc_session ADD CONSTRAINT rc_session_pkey PRIMARY KEY (sess_id);
ALTER TABLE ONLY public.rc_users ADD CONSTRAINT rc_users_pkey PRIMARY KEY (user_id);
ALTER TABLE ONLY public.rc_users ADD CONSTRAINT users_username_key UNIQUE (username, mail_host);

CREATE INDEX IF NOT EXISTS "cache_created_idx" ON public.rc_cache USING btree (created);
CREATE INDEX IF NOT EXISTS "cache_user_id_idx" ON public.rc_cache USING btree (user_id, cache_key);
CREATE INDEX IF NOT EXISTS "contactgroupmembers_contact_id_idx" ON public.rc_contactgroupmembers USING btree (contact_id);
CREATE INDEX IF NOT EXISTS "contactgroups_user_id_idx" ON public.rc_contactgroups USING btree (user_id, del);
CREATE INDEX IF NOT EXISTS "identities_user_id_idx" ON public.rc_identities USING btree (user_id, del);
CREATE INDEX IF NOT EXISTS "rc_cache_index_changed_idx" ON public.rc_cache_index USING btree (changed);
CREATE INDEX IF NOT EXISTS "rc_cache_index_user_id_idx" ON public.rc_cache_index USING btree (user_id);
CREATE INDEX IF NOT EXISTS "rc_cache_messages_changed_idx" ON public.rc_cache_messages USING btree (changed);
CREATE INDEX IF NOT EXISTS "rc_cache_messages_user_id_idx" ON public.rc_cache_messages USING btree (user_id);
CREATE INDEX IF NOT EXISTS "rc_cache_thread_changed_idx" ON public.rc_cache_thread USING btree (changed);
CREATE INDEX IF NOT EXISTS "rc_cache_thread_user_id_idx" ON public.rc_cache_thread USING btree (user_id);
CREATE INDEX IF NOT EXISTS "rc_cache_user_id_idx" ON public.rc_cache USING btree (user_id);
CREATE INDEX IF NOT EXISTS "rc_contactgroupmembers_contactgroup_id_idx" ON public.rc_contactgroupmembers USING btree (contactgroup_id);
CREATE INDEX IF NOT EXISTS "rc_contactgroups_user_id_idx" ON public.rc_contactgroups USING btree (user_id);
CREATE INDEX IF NOT EXISTS "rc_contacts_user_id_idx" ON public.rc_contacts USING btree (user_id, del);
CREATE INDEX IF NOT EXISTS "rc_dictionary_user_id_idx" ON public.rc_dictionary USING btree (user_id);
CREATE INDEX IF NOT EXISTS "rc_identities_user_id_idx" ON public.rc_identities USING btree (user_id);
CREATE INDEX IF NOT EXISTS "rc_searches_user_id_idx" ON public.rc_searches USING btree (user_id);
CREATE INDEX IF NOT EXISTS "rc_searches_user_id_name_type_idx" ON public.rc_searches USING btree (user_id, name, type);
CREATE INDEX IF NOT EXISTS "session_changed_idx" ON public.rc_session USING btree (changed);
CREATE INDEX IF NOT EXISTS "users_alias_id_idx" ON public.rc_users USING btree (alias);

ALTER TABLE ONLY public.rc_cache_index ADD CONSTRAINT rc_cache_index_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.rc_users(user_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY public.rc_cache_messages ADD CONSTRAINT rc_cache_messages_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.rc_users(user_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY public.rc_cache_thread ADD CONSTRAINT rc_cache_thread_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.rc_users(user_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY public.rc_cache ADD CONSTRAINT rc_cache_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.rc_users(user_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY public.rc_contactgroupmembers ADD CONSTRAINT rc_contactgroupmembers_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.rc_contacts(contact_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY public.rc_contactgroupmembers ADD CONSTRAINT rc_contactgroupmembers_contactgroup_id_fkey FOREIGN KEY (contactgroup_id) REFERENCES public.rc_contactgroups(contactgroup_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY public.rc_contactgroups ADD CONSTRAINT rc_contactgroups_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.rc_users(user_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY public.rc_contacts ADD CONSTRAINT rc_contacts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.rc_users(user_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY public.rc_dictionary ADD CONSTRAINT rc_dictionary_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.rc_users(user_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY public.rc_identities ADD CONSTRAINT rc_identities_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.rc_users(user_id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE ONLY public.rc_searches ADD CONSTRAINT rc_searches_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.rc_users(user_id) ON UPDATE CASCADE ON DELETE CASCADE;

