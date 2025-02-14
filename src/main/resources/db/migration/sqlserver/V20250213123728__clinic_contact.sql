drop table if exists clinic_contact;
create table clinic_contact (
    id int not null identity(1,1) primary key,
    name varchar(50) not null,
    primaryPhone varchar(14) not null,
    afterHoursPhone varchar(14)
);
