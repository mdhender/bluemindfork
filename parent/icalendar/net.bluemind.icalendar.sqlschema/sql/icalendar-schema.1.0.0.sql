create type t_icalendar_cutype as enum 
('Individual', 'Group', 'Resource', 'Room', 'Unknown');

create type t_icalendar_role as enum 
('Chair', 'RequiredParticipant', 'OptionalParticipant', 'NonParticipant');

create type t_icalendar_partstat as enum 
('NeedsAction', 'Accepted', 'Declined', 'Tentative', 'Delegated', 'Forbidden', 'Completed');

create type t_icalendar_class as enum
('Public', 'Private', 'Confidential');

create type t_icalendar_rrule_frequency as enum
('SECONDLY', 'MINUTELY', 'HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY');

create type t_icalendar_status as enum
('Tentative', 'Confirmed', 'Cancelled', 'NeedsAction', 'Completed', 'InProcess');

create type t_icalendar_valarm_action as enum
('Audio', 'Display', 'Email');