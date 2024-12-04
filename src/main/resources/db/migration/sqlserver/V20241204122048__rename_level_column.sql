-- rename the level column to something else for VUMC
exec sp_rename 'dbo.audit_data.level', 'severity', 'COLUMN';
go
