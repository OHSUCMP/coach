drop table if exists summary;
create table summary (
    id int not null auto_increment primary key,
    patId int not null,
    bpGoal varchar(7),
    calculatedBP varchar(7),
    bpAtOrBelowGoal tinyint(1),
    notes text,
    createdDate datetime not null default current_timestamp,
    constraint sum_fk1 foreign key (patId) references patient (id) on delete cascade
);

create index idxPatId on summary (patId);

drop table if exists summary_recommendation;
create table summary_recommendation (
    id int not null auto_increment primary key,
    summaryId int not null,
    recommendation varchar(255) not null,
    severity varchar(20) not null,
    card varchar(255) not null,
    constraint sum_rec_fk1 foreign key (summaryId) references summary (id) on delete cascade
);

create index idxSummaryId on summary_recommendation (summaryId);