CREATE TABLE schema_version (
    name TEXT NOT NULL,
    version TEXT NOT NULL,
     CONSTRAINT schema_uniqeness UNIQUE(name,version)
    );