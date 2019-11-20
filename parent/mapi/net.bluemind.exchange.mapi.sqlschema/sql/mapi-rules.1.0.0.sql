drop table if exists t_mapi_rule;

CREATE TABLE t_mapi_rule (
	folder_id text,
	rule_id bigint,
	blob text,
	UNIQUE (folder_id, rule_id)
);
