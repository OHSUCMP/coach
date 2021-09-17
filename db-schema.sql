create database if not exists htnu18app;

use htnu18app;

-- drop user if exists 'htnu18app'@'%';
-- create user 'htnu18app'@'%' identified by 'htnu18app';
-- grant all on htnu18app.* to 'htnu18app'@'%';

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

-- drop goal_history first as it depends on goal
drop table if exists goal_history;

drop table if exists goal;
create table goal (
  id int not null auto_increment primary key,
  patId int not null,
  extGoalId varchar(100) not null,
  referenceSystem varchar(100) not null,
  referenceCode varchar(100) not null,
  goalText varchar(255) not null,
  systolicTarget int,
  diastolicTarget int,
  targetDate date,
  createdDate datetime not null
);

create unique index idxPatGoalId on goal (patId, extGoalId);

create table goal_history (
    id int not null auto_increment primary key,
    goalId int not null,
    achievementStatus varchar(20) not null,
    createdDate datetime not null,
    foreign key (goalId)
        references goal (id)
        on delete cascade
);

create index idxGoalId on goal_history (goalId);

drop table if exists counseling;
create table counseling (
    id int not null auto_increment primary key,
    patId int not null,
    extCounselingId varchar(100) not null,
    referenceSystem varchar(100) not null,
    referenceCode varchar(100) not null,
    counselingText varchar(255) not null,
    createdDate datetime not null
);

create index idxPatCounselingId on counseling (patId, extCounselingId);

drop table if exists adverse_event;
create table adverse_event (
    id int not null auto_increment primary key,
    description varchar(255) not null,
    conceptCode varchar(50) not null,
    conceptSystem varchar(100) not null
);

-- FHIR terminology systems: https://www.hl7.org/fhir/terminologies-systems.html
-- also see: https://www.hl7.org/fhir/icd.html
-- ICD9CM: http://hl7.org/fhir/sid/icd-9-cm
-- ICD10CM: http://hl7.org/fhir/sid/icd-10-cm
-- SNOMEDCT: http://snomed.info/sct
-- CPT: http://www.ama-assn.org/go/cpt

insert into adverse_event(description, conceptCode, conceptSystem) values("Acute kidney problem", "N17", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Acute kidney problem", "N17.0", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Acute kidney problem", "N17.1", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Acute kidney problem", "N17.2", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Acute kidney problem", "N17.8", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Acute kidney problem", "N17.9", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "I49.5", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "I49.8", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "R00.1", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "427.81", "http://hl7.org/fhir/sid/icd-9-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "427.89", "http://hl7.org/fhir/sid/icd-9-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "251162005", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "29894000", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "397841007", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "44602002", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "49044005", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Bradycardia", "49710005", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Electrolyte problems / metabolic panel", "80048", "http://www.ama-assn.org/go/cpt");
insert into adverse_event(description, conceptCode, conceptSystem) values("Electrolyte problems / metabolic panel", "80053", "http://www.ama-assn.org/go/cpt");
insert into adverse_event(description, conceptCode, conceptSystem) values("Electrolyte problems / metabolic panel", "80069", "http://www.ama-assn.org/go/cpt");
insert into adverse_event(description, conceptCode, conceptSystem) values("Electrolyte problems / metabolic panel", "82310", "http://www.ama-assn.org/go/cpt");
insert into adverse_event(description, conceptCode, conceptSystem) values("Fall", "W19", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Fall", "W11", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Fall", "W13", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Fall", "W12", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Fall", "W14", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Fall", "W15", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Fall", "W06", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Fall", "W07", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Fall", "W09", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "I95.0", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "I95.1", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "I95.2", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "I95.3", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "I95.81", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "I95.89", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "I95.9", "http://hl7.org/fhir/sid/icd-10-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "458", "http://hl7.org/fhir/sid/icd-9-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "458.1", "http://hl7.org/fhir/sid/icd-9-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "458.21", "http://hl7.org/fhir/sid/icd-9-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "458.29", "http://hl7.org/fhir/sid/icd-9-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "458.8", "http://hl7.org/fhir/sid/icd-9-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "458.9", "http://hl7.org/fhir/sid/icd-9-cm");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "195506001", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "200113008", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "200114002", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "230664009", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "234171009", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "271870002", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "286963007", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "371073003", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "408667000", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "408668005", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "429561008", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "45007003", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "61933008", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "70247006", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "75181005", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "77545000", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Hypotension", "88887003", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Syncope / Loss of consciousness", "32834005", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Syncope / Loss of consciousness", "40863000", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Syncope / Loss of consciousness", "419045004", "http://snomed.info/sct");
insert into adverse_event(description, conceptCode, conceptSystem) values("Syncope / Loss of consciousness", "7862002", "http://snomed.info/sct");

drop table if exists adverse_event_outcome;
create table adverse_event_outcome (
    id int not null auto_increment primary key,
    adverseEventIdHash char(64) unique not null,
    outcome varchar(30) not null,
    createdDate datetime not null
);

