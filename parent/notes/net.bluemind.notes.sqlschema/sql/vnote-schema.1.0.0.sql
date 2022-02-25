create type t_notes_vnote_color as enum ('BLUE', 'GREEN', 'PINK', 'YELLOW', 'WHITE');

create table t_notes_vnote (
  item_id bigint not null references t_container_item(id) on delete cascade primary key,
  body text not null default '',
  subject text not null default '',
  width int not null default 150,
  height int not null default 150,
  posX int not null default 0,
  posY int not null default 0,
  color t_notes_vnote_color not null default 'YELLOW'
);
