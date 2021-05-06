create database if not exists htnu18app;

use htnu18app;

drop user if exists 'htnu18app'@'%';
create user 'htnu18app'@'%' identified by 'htnu18app';
grant all on htnu18app.* to 'htnu18app'@'%';

drop table if exists patient;
create table patient (
    id int not null auto_increment primary key,
    pat_id_hash char(64) unique not null
);

drop table if exists home_bp_reading;
create table home_bp_reading (
    id int not null auto_increment primary key,
    pat_id int not null,
    systolic int not null,
    diastolic int not null,
    pulse int not null,
    reading_date datetime not null,
    followed_instructions tinyint(1) not null,
    created_date datetime not null
);

create index patId on home_bp_reading (pat_id);

drop table if exists goal;
create table goal (
  id int not null auto_increment primary key,
  goal_id varchar(100) not null,
  pat_id int not null,
  goal_text varchar(255) not null,
  follow_up_days int,
  created_date datetime not null,
  completed tinyint(1) not null default 0,
  completed_date datetime
);

create unique index goalPatId on goal (goal_id, pat_id);
