CREATE EXTENSION IF NOT EXISTS hstore WITH schema pg_catalog;

create table t_settings_global (
	settings hstore
);

insert into t_settings_global values (
  '"date_upd"=>"m/d/Y",
  "timezone"=>"Europe/Paris",
  "mail"=>"true",
  "mail_participation"=>"true",
  "day_weekstart"=>"monday",
  "public_fb"=>"0",
  "lang"=>"fr",
  "date"=>"yyyy-MM-dd",
  "timeformat"=>"HH:mm",
  "defaultview"=>"week",
  "showweekends"=>"true",
  "work_hours_start"=>"8",
  "work_hours_end"=>"18",
  "working_days"=>"mon,tue,wed,thu,fri",
  "show_declined_events"=>"false",
  "default_app"=>"/webmail/",
  "sync_sequence"=>"0",
  "cal_set_im_presence"=>"false",
  "cal_set_phone_presence"=>"false",
  "im_set_phone_presence"=>"false",
  "default_event_alert"=>"900",
  "default_allday_event_alert"=>"25200"'
);
