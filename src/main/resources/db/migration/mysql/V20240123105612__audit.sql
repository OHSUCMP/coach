drop table if exists audit;
create table audit (
    id int not null auto_increment primary key,
    patId int not null,
    level varchar(10) not null,
    event varchar(100) not null,
    details varchar(1000),
    created datetime not null default current_timestamp
);

create index idxPatId on audit (patId, level);
