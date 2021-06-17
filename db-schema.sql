create database if not exists htnu18app;

use htnu18app;

drop user if exists 'htnu18app'@'%';
create user 'htnu18app'@'%' identified by 'htnu18app';
grant all on htnu18app.* to 'htnu18app'@'%';

drop table if exists patient;
create table patient (
    id int not null auto_increment primary key,
    patIdHash char(64) unique not null
);

drop table if exists home_bp_reading;
create table home_bp_reading (
    id int not null auto_increment primary key,
    patId int not null,
    systolic int not null,
    diastolic int not null,
    pulse int not null,
    readingDate datetime not null,
    followedInstructions tinyint(1) not null,
    createdDate datetime not null
);

create index idxPatId on home_bp_reading (patId);

drop table if exists goal;
create table goal (
  id int not null auto_increment primary key,
  patId int not null,
  extGoalId varchar(100) not null,
  goalText varchar(255) not null,
  followUpDays int,
  createdDate datetime not null,
  completed tinyint(1) not null default 0,
  completedDate datetime
);

create unique index idxPatGoalId on goal (patId, extGoalId);

drop table if exists goal_history;
create table goal_history (
    id int not null auto_increment primary key,
    goalId int not null,
    lifecycleStatus varchar(20) not null,
    achievementStatus varchar(20) not null,
    createdDate datetime not null
);

create index idxGoalId on goal_history (goalId);
