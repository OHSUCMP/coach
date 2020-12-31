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
    systolic int,
    diastolic int,
    reading_date datetime not null,
    created_date datetime not null default current_timestamp
);