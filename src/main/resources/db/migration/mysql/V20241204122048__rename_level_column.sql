-- rename the level column to something else for VUMC
alter table audit_data change level severity varchar(10) not null;
