drop table if exists audit;
create table audit (
    id int not null identity(1,1) primary key,
    patId int not null,
    level varchar(10) not null,
    event varchar(100) not null,
    details varchar(1000),
    created datetime not null constraint c_audit_created default current_timestamp
);
go

create index idxPatId on audit (patId, level);
go