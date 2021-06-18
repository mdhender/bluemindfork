CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;

create type t_calendar_transparency as enum ('Opaque', 'Transparent');

create table t_calendar_vevent (

  dtstart_timestamp timestamp without time zone NOT NULL,
  dtstart_timezone timezone,
  dtstart_precision e_datetime_precision NOT NULL,

  dtend_timestamp timestamp without time zone,
  dtend_timezone timezone,
  dtend_precision e_datetime_precision,

  summary text,
  location text,
  description text,

  /*
   * 4.8.1.3 Classification
   */
  class t_icalendar_class,

  /*
   * 4.8.2.7 Time Transparency
   */
  transparency t_calendar_transparency,

  /*
   * 4.8.1.11 Status
   */
  status t_icalendar_status,

  /*
   * 4.8.1.9 Priority
   */
  priority int,

  /*
   * link to another entity uid
   */
  organizer_uri text,
  organizer_cn text,
  organizer_mailto text,
  organizer_dir text,
  
  /*
   * 3.8.4.6 URL
   */
  url text,

  /*
   * 4.8.5.1 Exception Date/Times
   */
  exdate_timestamp timestamp without time zone[],
  exdate_timezone timezone,
  exdate_precision e_datetime_precision,
  
  /*
   * 3.8.5.2.  Recurrence Date-Times
   */
  rdate_timestamp timestamp without time zone[],
  rdate_timezone timezone,
  rdate_precision e_datetime_precision,

  /*
   * Attendee
   */
  cuType t_icalendar_cutype[],
  member text[],
  att_role t_icalendar_role[],
  partStat t_icalendar_partstat[],
  rsvp boolean[],
  delTo text[],
  delFrom text[],
  sentBy text[],
  cn text[],
  dir text[],
  language text[],
  att_uid text[], /* links to others entities uids */
  att_mailto text[], /* mailto for external attendee*/
  att_resp_comment text[],

  /*
   * 4.8.5.4 Recurrence Rule
   */
  rrule_frequency t_icalendar_rrule_frequency,
  rrule_interval int DEFAULT NULL,
  rrule_count int DEFAULT NULL,
  rrule_until_timestamp timestamp without time zone,
  rrule_until_timezone timezone,
  rrule_until_precision e_datetime_precision, 
  rrule_bySecond int[],
  rrule_byMinute int[],
  rrule_byHour int[],
  rrule_byDay text[],
  rrule_byMonthDay int[],
  rrule_byYearDay int[],
  rrule_byWeekNo int[],
  rrule_byMonth int[],

  /*
   * 3.8.4.4.  Recurrence ID
   */
  recurid_timestamp timestamp without time zone,
  recurid_timezone timezone,
  recurid_precision e_datetime_precision,

  /*
   * ?
   */
  role text,
  
  /*
   * VALARM 
   */
  valarm_action t_icalendar_valarm_action[],
  valarm_trigger int[],
  valarm_description text[],
  valarm_duration int[],
  valarm_repeat int[],
  valarm_summary text[],
  
   /*
    * 3.8.1.1.  Attachment
    */
   attach_uri text[],
   attach_name text[],

  /*
   * 4.8.7.4.  Sequence Number
   */
  sequence int,

  draft boolean,
  
  conference text,
  
  conference_id text,

  item_id int4 references t_container_item(id) on delete cascade
);

CREATE INDEX tcv_item_id_fkey ON t_calendar_vevent(item_id);
CREATE INDEX idx_calendar_vevent_valarm_trigger ON t_calendar_vevent (valarm_trigger);
CREATE INDEX idx_calendar_vevent_conference_id ON t_calendar_vevent(conference_id) WHERE conference_id IS NOT NULL;

CREATE TABLE t_calendar_series (
  ics_uid text NOT NULL,
  properties hstore,
  accept_counters boolean,
  item_id int4 REFERENCES t_container_item(id) ON UPDATE CASCADE ON DELETE CASCADE
);
CREATE INDEX t_calendar_series_item_id ON t_calendar_series(item_id);
CREATE INDEX idx_calendar_series_lowercase_icsuid ON t_calendar_series (lower(ics_uid));
