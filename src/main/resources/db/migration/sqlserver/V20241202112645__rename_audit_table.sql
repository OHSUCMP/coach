-- rename the audit table to something else for VUMC
exec sp_rename 'dbo.audit', 'audit_data';
go