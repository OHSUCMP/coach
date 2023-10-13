drop table if exists hypotension_adverse_event;
create table hypotension_adverse_event (
    id int not null auto_increment primary key,
    patId int not null,
    bp1Id int unique not null,
    bp2Id int unique not null,
    createdDate datetime not null
);

create index idxPatId on hypotension_adverse_event (patId);

-- add source column since we're storing Omron readings here now too to facilitate AE referencing
alter table home_bp_reading add source varchar(10);
update home_bp_reading set source='COACH_UI' where source is null;
alter table home_bp_reading modify source varchar(10) not null;

alter table home_pulse_reading add source varchar(10);
update home_pulse_reading set source='COACH_UI' where source is null;
alter table home_pulse_reading modify source varchar(10) not null;

-- as we're now going to also be storing Omron readings in the home_bp_readings and home_pulse_readings tables,
-- followedInstructions must allow null
alter table home_bp_reading modify followedInstructions tinyint(1);
alter table home_pulse_reading modify followedInstructions tinyint(1);
