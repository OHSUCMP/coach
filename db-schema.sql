create database if not exists htnu18app;

use htnu18app;

drop table if exists patient;
create table patient (
    id int not null auto_increment primary key,
    pat_id_hash char(64) unique not null
);

drop table if exists home_bp_reading;
create table home_bp_reading (
    id int not null auto_increment primary key,
    pat_id int not null,
    systolic1 int not null,
    diastolic1 int not null,
    pulse1 int not null,
    systolic2 int not null,
    diastolic2 int not null,
    pulse2 int not null,
    reading_date datetime not null,
    followed_instructions tinyint(1) not null,
    created_date datetime not null
);

drop table if exists goal;
create table goal (
  id int not null auto_increment primary key,
  goal_id varchar(100) not null,
  pat_id int not null,
  follow_up_days int,
  value varchar(255) not null,
  created_date datetime not null
);

create unique index goalPatId on goal (goal_id, pat_id);
