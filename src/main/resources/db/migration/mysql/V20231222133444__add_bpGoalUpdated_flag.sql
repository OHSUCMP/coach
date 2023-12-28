alter table patient add bpGoalUpdated tinyint not null default 0;

update patient set bpGoalUpdated=1 where id in (select distinct patId from goal where extGoalId='bp-goal' and systolicTarget != 140 and diastolicTarget != 90);
