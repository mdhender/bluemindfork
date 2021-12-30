create type t_notes_vnote_color as enum ('BLUE', 'GREEN', 'PINK', 'YELLOW', 'WHITE');

create table t_notes_vnote (
  body text,
  subject text,
  width int,
  height int,
  posX int,
  posY int,
  color t_notes_vnote_color default 'YELLOW',
  item_id int4 references t_container_item(id) on delete cascade
);
