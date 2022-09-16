create database if not exists coach;

use coach;

-- drop user if exists 'coach'@'%';
-- create user 'coach'@'%' identified by 'coach';
-- grant all on coach.* to 'coach'@'%';

drop table if exists patient;
create table patient (
    id int not null auto_increment primary key,
    patIdHash char(64) unique not null
);

drop table if exists home_bp_reading;
create table home_bp_reading (
    id int not null auto_increment primary key,
    patId int not null,
    systolic int not null,
    diastolic int not null,
    readingDate datetime not null,
    followedInstructions tinyint(1) not null,
    createdDate datetime not null
);

create index idxPatId on home_bp_reading (patId);

drop table if exists home_pulse_reading;
create table home_pulse_reading (
                                 id int not null auto_increment primary key,
                                 patId int not null,
                                 pulse int,
                                 readingDate datetime not null,
                                 followedInstructions tinyint(1) not null,
                                 createdDate datetime not null
);

create index idxPatId on home_pulse_reading (patId);

-- drop goal_history first as it depends on goal
drop table if exists goal_history;

drop table if exists goal;
create table goal (
  id int not null auto_increment primary key,
  patId int not null,
  extGoalId varchar(100) not null,
  referenceSystem varchar(100) not null,
  referenceCode varchar(100) not null,
  referenceDisplay varchar(255) not null,
  goalText varchar(255) not null,
  systolicTarget int,
  diastolicTarget int,
  targetDate date,
  createdDate datetime not null
);

create unique index idxPatGoalId on goal (patId, extGoalId);

create table goal_history (
    id int not null auto_increment primary key,
    goalId int not null,
    achievementStatus varchar(20) not null,
    createdDate datetime not null,
    foreign key (goalId)
        references goal (id)
        on delete cascade
);

create index idxGoalId on goal_history (goalId);

drop table if exists counseling;
create table counseling (
    id int not null auto_increment primary key,
    patId int not null,
    extCounselingId varchar(100) not null,
    referenceSystem varchar(100) not null,
    referenceCode varchar(100) not null,
    counselingText varchar(255) not null,
    createdDate datetime not null
);

create index idxPatCounselingId on counseling (patId, extCounselingId);

drop table if exists adverse_event;
create table adverse_event (
    id int not null auto_increment primary key,
    description varchar(255) not null,
    conceptCode varchar(50) not null,
    conceptSystem varchar(100) not null,
    conceptSystemOID varchar(50) not null
);

-- FHIR terminology systems: https://www.hl7.org/fhir/terminologies-systems.html
-- also see: https://www.hl7.org/fhir/icd.html
-- ICD9CM: http://hl7.org/fhir/sid/icd-9-cm
-- ICD10CM: http://hl7.org/fhir/sid/icd-10-cm
-- SNOMEDCT: http://snomed.info/sct
-- CPT: http://www.ama-assn.org/go/cpt

insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Acute kidney problem', 'N17', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Acute kidney problem', 'N17.0', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Acute kidney problem', 'N17.1', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Acute kidney problem', 'N17.2', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Acute kidney problem', 'N17.8', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Acute kidney problem', 'N17.9', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', 'I49.5', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', 'I49.8', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', 'R00.1', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', '427.81', 'http://hl7.org/fhir/sid/icd-9-cm', '2.16.840.1.113883.6.42');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', '427.89', 'http://hl7.org/fhir/sid/icd-9-cm', '2.16.840.1.113883.6.42');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', '251162005', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', '29894000', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', '397841007', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', '44602002', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', '49044005', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Bradycardia', '49710005', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Electrolyte problems / metabolic panel', '80048', 'http://www.ama-assn.org/go/cpt', '2.16.840.1.113883.6.12');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Electrolyte problems / metabolic panel', '80053', 'http://www.ama-assn.org/go/cpt', '2.16.840.1.113883.6.12');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Electrolyte problems / metabolic panel', '80069', 'http://www.ama-assn.org/go/cpt', '2.16.840.1.113883.6.12');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Electrolyte problems / metabolic panel', '82310', 'http://www.ama-assn.org/go/cpt', '2.16.840.1.113883.6.12');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Fall', 'W19', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Fall', 'W11', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Fall', 'W13', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Fall', 'W12', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Fall', 'W14', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Fall', 'W15', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Fall', 'W06', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Fall', 'W07', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Fall', 'W09', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', 'I95.0', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', 'I95.1', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', 'I95.2', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', 'I95.3', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', 'I95.81', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', 'I95.89', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', 'I95.9', 'http://hl7.org/fhir/sid/icd-10-cm', '2.16.840.1.113883.6.90');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '458', 'http://hl7.org/fhir/sid/icd-9-cm', '2.16.840.1.113883.6.42');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '458.1', 'http://hl7.org/fhir/sid/icd-9-cm', '2.16.840.1.113883.6.42');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '458.21', 'http://hl7.org/fhir/sid/icd-9-cm', '2.16.840.1.113883.6.42');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '458.29', 'http://hl7.org/fhir/sid/icd-9-cm', '2.16.840.1.113883.6.42');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '458.8', 'http://hl7.org/fhir/sid/icd-9-cm', '2.16.840.1.113883.6.42');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '458.9', 'http://hl7.org/fhir/sid/icd-9-cm', '2.16.840.1.113883.6.42');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '195506001', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '200113008', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '200114002', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '230664009', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '234171009', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '271870002', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '286963007', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '371073003', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '408667000', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '408668005', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '429561008', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '45007003', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '61933008', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '70247006', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '75181005', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '77545000', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Hypotension', '88887003', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Syncope / Loss of consciousness', '32834005', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Syncope / Loss of consciousness', '40863000', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Syncope / Loss of consciousness', '419045004', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');
insert into adverse_event(description, conceptCode, conceptSystem, conceptSystemOID) values('Syncope / Loss of consciousness', '7862002', 'http://snomed.info/sct', '2.16.840.1.113883.6.96');

drop table if exists adverse_event_outcome;
create table adverse_event_outcome (
    id int not null auto_increment primary key,
    adverseEventIdHash char(64) unique not null,
    outcome varchar(30) not null,
    createdDate datetime not null,
    modifiedDate datetime
);

drop table if exists counseling_page;
create table counseling_page (
    id int not null auto_increment primary key,
    pageKey varchar(50) unique not null,
    title varchar(255) not null,
    body text not null
);

insert into counseling_page(pageKey, title, body) values('diet', 'Diet', '<p>Choose healthy meal and snack options to help you avoid high blood pressure and its complications. Be sure to eat plenty of fresh fruits and vegetables.</p><p>Talk with your health care team about eating a variety of foods rich in potassium, fiber, and protein and lower in salt (sodium) and saturated fat. For many people, making these healthy changes can help keep blood pressure low and protect against heart disease and stroke.</p><p>Reducing the amount of sodium in your diet can also help control blood pressure. Avoiding processed foods and natural foods with higher-than-average sodium content (such as cheese, seafood, olives, and some legumes).</p><p>The <a href=\'https://www.nhlbi.nih.gov/health-topics/dash-eating-plan\' target=\'_blank\'>DASH (Dietary Approaches to Stop Hypertension) Diet</a> is a healthy diet plan with a proven record of helping people lower their blood pressure.<br/>Visit the <a href=\'https://www.cdc.gov/nccdphp/dnpao/index.html\' target=\'_blank\'>CDC\'s Nutrition, Physical Activity, and Obesity</a> website to learn more about healthy eating and nutrition.<br/>Visit the American Heart Association\'s page: <a href=\'https://www.heart.org/en/health-topics/high-blood-pressure/changes-you-can-make-to-manage-high-blood-pressure/shaking-the-salt-habit-to-lower-high-blood-pressure\' target=\'_blank\'>Shaking the Salt Habit to Lower High Blood Pressure</a></p>');
insert into counseling_page(pageKey, title, body) values('weight-loss', 'Weight Loss', '<p>Being overweight or obese increases your risk for high blood pressure. If you are overweight, losing as little as 5 to 10 pounds can help reduce your blood pressure.</p><p>The two essentials to maintaining a healthy weight are:<ul><li>Eating well</li><li>Moving often</li></ul></p><p>If you need to lose weight, talk to your healthcare professional about a healthy approach. Your doctor can help you figure out how many calories you need for weight loss and advise you on which types of activities are best.</p><p>To determine whether your weight is in a healthy range, doctors often calculate your <a href=\'https://www.cdc.gov/healthyweight/assessing/bmi\' target=\'_blank\'>body mass index (BMI)</a>. If you know your weight and height, you can <a href=\'https://www.cdc.gov/healthyweight/assessing/index.html\' target=\'_blank\'>calculate your BMI at CDC\'s Assessing Your Weight website</a>. Doctors sometimes also use waist and hip measurements to assess body fat.</p><p>Talk with your health care team about ways to reach a healthy weight, including choosing healthy foods and getting regular physical activity.</p><p><a href=\'https://www.heart.org/-/media/files/health-topics/answers-by-heart/why-should-i-lose-weight.pdf\' target=\'_blank\'>Why should I lose weight?</a><br/><a href=\'https://www.heart.org/-/media/files/health-topics/answers-by-heart/how-can-i-manage-my-weight.pdf\' target=\'_blank\'>How can I manage my weight?</a></p>');
insert into counseling_page(pageKey, title, body) values('physical-activity', 'Physical Activity', '<p>Physical activity can help keep you at a healthy weight and lower your blood pressure. The <a href=\'https://health.gov/paguidelines/second-edition/\' target=\'_blank\'>Physical Activity Guidelines for Americans</a> recommends that adults get at least 2 hours and 30 minutes of moderate-intensity exercise, such as brisk walking or bicycling, every week. That\'s about 30 minutes per day, 5 days per week. Children and adolescents should get 1 hour of physical activity every day.</p><p>For overall health benefits to the heart, lungs and circulation, get regular aerobic activity using the following guidelines:<ul><li>For most healthy people, get the equivalent of at least 150 minutes (two hours and 30 minutes) per week of moderate-intensity physical activity, such as brisk walking.</li><li>You can break up your weekly physical activity goal however you like. An easy plan to remember is 30 minutes a day on at least five days a week. But shorter sessions count, too.</li><li>Physical activity should be spread throughout the week.</li><li>Include flexibility and stretching exercises.</li><li>Include muscle-strengthening activity at least two days each week.</li></ul></p><p>If you have not been active for quite some time or if you are beginning a new activity or exercise program, take it gradually. Consult your healthcare professional if you have cardiovascular disease or any other preexisting condition. It\'s best to start slowly with something you enjoy, like taking walks or riding a bicycle. Scientific evidence strongly shows that physical activity is safe for almost everyone. Moreover, the health benefits of physical activity far outweigh the risks.</p><p>If you love the outdoors, combine it with exercise and enjoy the scenery while you walk or jog. If you love to listen to audiobooks, enjoy them while you use an elliptical machine.</p><p>These activities are especially beneficial when done regularly:<ul><li>Brisk walking, hiking or stair-climbing</li><li>Jogging, running, bicycling, rowing or swimming</li><li>Fitness classes at your appropriate level</li><li>Activities such team sports, a dance class or fitness games</li></ul></p><p>Know how much activity is right for you. If you injure yourself right at the start, you are less likely to keep going. Focus on doing something that gets your heart rate up to a moderate level. If you\'re physically active regularly for longer periods or at greater intensity, you\'re likely to benefit more. But don\'t overdo it. Too much exercise can give you sore muscles and increase the risk of injury.</p><p><a href=\'https://www.heart.org/-/media/files/health-topics/answers-by-heart/why-should-i-be-physically-active.pdf\' target=\'_blank\'>Why should I be physically active?</a><br/><a href=\'https://www.heart.org/-/media/files/health-topics/answers-by-heart/how-can-physical-activity-become-a-way-of-life.pdf\' target=\'_blank\'>Making physical activity a way of life</a></p>');
insert into counseling_page(pageKey, title, body) values('smoking-cessation', 'Smoking Cessation', '<p>Smoking raises your blood pressure and puts you at higher risk for heart attack and stroke. If you do not smoke, do not start. If you do smoke, quitting will lower your risk for heart disease. Your doctor can suggest ways to help you quit.</p><p><a href=\'https://www.heart.org/-/media/files/health-topics/answers-by-heart/how-can-i-quit-smoking.pdf\' target=\'_blank\'>How can I quit smoking?</a><br/><a href=\'https://www.heart.org/-/media/files/health-topics/answers-by-heart/how-can-i-handle-the-stress-of-not-smoking.pdf\' target=\'_blank\'>Handling the stress of quitting</a><br/><a href=\'https://www.heart.org/-/media/files/health-topics/answers-by-heart/how-can-i-avoid-weight-gain-when-i-stop-smoking.pdf\' target=\'_blank\'>Avoiding weight gain while quitting</a></p>');
insert into counseling_page(pageKey, title, body) values('alcohol-moderation', 'Alcohol Moderation', '<p>Do not drink too much alcohol, which can raise your blood pressure. Men should have no more than 2 alcoholic drinks per day, and women should have no more than 1 alcoholic drink per day. A drink is one 12 oz. beer, 4 oz. of wine, 1.5 oz. of 80-proof spirits or 1 oz. of 100-proof spirits.</p><p>Red wine as a miracle drink for heart heath is a myth. The linkage reported in many of these studies may be due to other lifestyle factors rather than alcohol. Like any other dietary or lifestyle choice, it\'s a matter of moderation.</p><p>Excessive drinking is defined as:<ul><li>Binge drinking (consuming 4 or more alcoholic beverages per occasion for women or 5 or more drinks per occasion for men)</li><li>Heavy drinking (consuming 8 or more alcoholic beverages per week for women or 15 or more alcoholic beverages per week for men)</li><li>Any drinking by pregnant women or those younger than age 21</li></ul></p><p><a href=\'https://www.heart.org/en/healthy-living/healthy-eating/eat-smart/nutrition-basics/alcohol-and-heart-health\' target=\'_blank\'>Alcohol and heart health</a></p>');

drop table if exists contact_message;
create table contact_message (
    id int not null auto_increment primary key,
    messageKey varchar(50) unique not null,
    subject varchar(255) not null,
    body text not null
);

insert into contact_message(messageKey, subject, body) values('urgent-bp', 'Alert: Blood pressure of >180/120 mmHg recorded.', 'This person sending the message had a high blood pressure recorded (>180/120 mmHg). They were counseled to seek immediate care if they had any symptoms; sending this message indicates they did not have symptoms. A follow-up contact, such as a visit or call, is likely warranted.
This message was generated by the COACH tool and actively sent by the patient.');
insert into contact_message(messageKey, subject, body) values('suspect-htn-stage2', 'Multiple blood pressure readings of >=140/90 mmHg recorded. Consider diagnosing patient with Stage 2 Hypertension.', '4 or more blood pressure readings that average >=140/90 mmHg have been recorded. A diagnosis of high blood pressure was not found by this tool. A diagnosis of stage 2 hypertension should be considered.
This message was generated by the COACH tool and actively sent by the patient. ');
insert into contact_message(messageKey, subject, body) values('possible-htn-stage2', 'One or more blood pressure readings of >=140/90 mmHg recorded. A diagnosis of Stage 2 Hypertension may be appropriate.', 'One or more blood pressure readings of >=140/90 mmHg have been recorded. A diagnosis of high blood pressure was not found by this tool. A diagnosis of stage 2 hypertension may be appropriate.
This message was generated by the COACH tool and actively sent by the patient.');
insert into contact_message(messageKey, subject, body) values('possible-htn-stage1', 'Blood pressure readings of >130/80 mmHg recorded. A diagnosis of Stage 1 Hypertension may be appropriate.', 'Blood pressure readings of >130/80 suggest that the patient may have hypertension, but no diagnosis was found by this tool. It may be reasonable to diagnose the patient with stage 1 hypertension.
This message was generated by the COACH tool and actively sent by the patient.');
insert into contact_message(messageKey, subject, body) values('ambulatory-bps', 'High blood pressure variability detected. Ambulatory blood pressure monitoring is recommended.', 'A high degree of variability was detected in recent blood pressure readings. Identifying patterns within this variability may require the use of ambulatory blood pressure monitoring over a period of up to 24 hours.
This message was generated by the COACH tool.');
insert into contact_message(messageKey, subject, body) values('recommend-pharma-and-nonpharma', 'Blood pressure readings are substantially above goal. Lifestyle change counseling and antihypertensive medication were recommended.', 'The set of blood pressures recorded is substantially above goal. A combination of pharmacologic and non-pharmacologic treatment is recommended to better control blood pressure.
This message was generated by the COACH tool and actively sent by the patient.');
insert into contact_message(messageKey, subject, body) values('recommend-nonpharma', 'Blood pressure readings are above goal.  Lifestyle change counseling, followed by treatment with antihypertensive medication was recommended.', 'This person had high blood pressures and was recommended to select from a set of behavior and lifestyle changes. They will be recommended to follow-up after they work on their goals to consider changing medications.
This message was generated by the COACH tool and actively sent by the patient.');
insert into contact_message(messageKey, subject, body) values('recommend-pharma', 'Blood pressure is above goal after lifestyle changes. Treatment with antihypertensive medication is recommended.', 'Blood pressure readings are substantially above goal. Antihypertensive medication will be needed to lower blood pressure and meet goal. Discussing potential medication options and blood pressure goals with patient may be warranted.
This message was generated by the COACH tool and actively sent by the patient.');
insert into contact_message(messageKey, subject, body) values('recommend-advancing-treatment', 'Blood pressure remains above goal despite treatment. Additional antihypertensive treatment may be considered.', 'Blood pressure remains above goal despite current treatment. Changes to medication regimen, possibly including change of medication or prescription of additional medication may be require to control blood pressure. Discussing potential changes to blood pressure therapies may be warranted.
This message was generated by the COACH tool and actively sent by the patient.');
insert into contact_message(messageKey, subject, body) values('untreated-adverse-event', 'Alert: Possible adverse reaction detected. Consider changing course of treatment and blood pressure goal.', 'A significant adverse event often associated with antihypertensive medications has been detected. Depending on the seriousness of the reaction, a change to course of treatment may be necessary. This will require consultation between care team and patient to decide best course of action, and my involve adjustment of blood pressure goals.
This message was generated by the COACH tool and actively sent by the patient.');
insert into contact_message(messageKey, subject, body) values('treated-adverse-event', 'Adverse reaction detected but marked as addressed. If reaction recurs, consider changing course of treatment and blood pressure goal.', 'A significant adverse event associated with antihypertensive medication has been detected and addressed, per the patient.  If reaction recurs, adjustment to medication and blood pressure goal may be required to avoid future recurrence.
This message was generated by the COACH tool and actively sent by the patient.');


-- the following added 2022-08-05

alter table home_bp_reading add source varchar(30) after followedInstructions;
update home_bp_reading set source = 'HOME' where source is null;
alter table home_bp_reading change source source varchar(30) not null;
alter table home_bp_reading change systolic systolic int;
alter table home_bp_reading change diastolic diastolic int;

-- add foreign key constraints stemming from the main patient table, useful for clearing data - added 2022-09-01

alter table home_bp_reading add foreign key (patId) references patient (id) on delete cascade;
alter table home_pulse_reading add foreign key (patId) references patient (id) on delete cascade;
alter table goal add foreign key (patId) references patient (id) on delete cascade;
alter table counseling add foreign key (patId) references patient (id) on delete cascade;

-- the following added 2022-09-14 for vsac-integration

drop table if exists vsac_valueset;
create table vsac_valueset (
    id int not null auto_increment primary key,
    oid varchar(255) not null,
    displayName varchar(255) not null,
    version varchar(255) not null,
    source varchar(255),
    purpose text,
    type varchar(50),
    binding varchar(50),
    status varchar(50),
    revisionDate date,
    created datetime not null default current_timestamp,
    updated datetime not null default current_timestamp on update current_timestamp,
    constraint c1 unique (oid, version)
);

drop table if exists vsac_concept;
create table vsac_concept (
    id int not null auto_increment primary key,
    code varchar(255) not null,
    codeSystem varchar(255) not null,
    codeSystemName varchar(255) not null,
    codeSystemVersion varchar(255) not null,
    displayName varchar(255) not null,
    created datetime not null default current_timestamp,
    updated datetime not null default current_timestamp on update current_timestamp,
    constraint c1 unique (code, codeSystem, codeSystemVersion)
);

-- alter table vsac_valueset_concept drop constraint fk1;
-- alter table vsac_valueset_concept drop constraint fk2;

drop table if exists vsac_valueset_concept;
create table vsac_valueset_concept (
    valueSetId int not null,
    conceptId int not null,
    constraint pk1 primary key (valueSetId, conceptId),
    constraint fk1 foreign key (valueSetId) references vsac_valueset (id)
       on delete cascade,
    constraint fk2 foreign key (conceptId) references vsac_concept (id)
       on delete cascade
);