CREATE OR REPLACE FUNCTION is_timezone( tz TEXT ) RETURNS BOOLEAN as $$
DECLARE 
	date TIMESTAMPTZ;
BEGIN
	date := now() AT TIME ZONE tz;
	RETURN TRUE;
EXCEPTION WHEN OTHERS THEN
	RETURN FALSE; END;
$$ language plpgsql STABLE;
 
CREATE DOMAIN timezone AS TEXT CHECK ( is_timezone( value ) );

CREATE TYPE e_datetime_precision AS ENUM ('Date', 'DateTime');
