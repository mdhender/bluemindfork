
create table t_todolist_vtodo (
  /*
  * 3.8.4.7.  Unique Identifier
  */
  uid text NOT NULL,

  /*
   * 3.8.7.2.  Date-Time Stamp
   */

  dtstart_timestamp timestamp without time zone,
  dtstart_timezone timezone,
  dtstart_precision e_datetime_precision,
  
  /*
   * 3.8.2.3.  Date-Time Due
   */
  due_timestamp timestamp without time zone,
  due_timezone timezone,
  due_precision e_datetime_precision,

  /*
   * 3.8.1.12 Summary
   */
  summary text,

  /*
   * 4.8.1.3 Classification
   */
  class t_icalendar_class,

  /*
   * 3.8.1.7 location
   */        
  location text,

  /*
   * 3.8.1.5 Description
   */

  description text,

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
  /*
   * Organizer CN
   */
  organizer_cn text,
  /*
   * Organizer mailto
   */
  organizer_mailto text,
  organizer_dir text,

  /*
   * 3.8.4.6 URL
   */
  url text,  
  
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
  att_mailto text[],
  att_uid text[], /* links to others entities uids */
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
   * 3.8.4.4.  Recurrence ID
   */
  recurid_timestamp timestamp without time zone,
  recurid_timezone timezone,
  recurid_precision e_datetime_precision, 

  /*
   * 3.8.1.8.  Percent Complete
   */
  percent int,

  /*
   * 3.8.2.1.  Date-Time Completed
   */
  completed_timestamp timestamp without time zone,
  completed_timezone timezone,
  completed_precision e_datetime_precision,
    
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

  item_id int4 references t_container_item(id) primary key
);

create index idx_todolist_uid on t_todolist_vtodo (uid);
