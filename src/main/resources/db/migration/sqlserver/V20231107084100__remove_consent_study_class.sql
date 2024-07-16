alter table patient drop column studyClass;
ALTER TABLE patient
DROP CONSTRAINT DF_PATIENT_CONSENT_DEF;
alter table patient drop column consentGranted;
