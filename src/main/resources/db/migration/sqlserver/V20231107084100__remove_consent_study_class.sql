alter table patient drop column studyClass;
go

alter table patient drop constraint c_patient_consentGranted;
alter table patient drop column consentGranted;
