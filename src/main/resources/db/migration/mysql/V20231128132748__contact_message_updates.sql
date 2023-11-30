alter table contact_message add aboveText varchar(1000);
alter table contact_message add belowText varchar(1000);

update contact_message
set body='I collected {readings} BP readings over the last {days} days, and my blood pressure is still above goal; my BP average is {systolic}/{diastolic}.  I am currently taking {meds}.  What steps would you like me to take next?  <ADD MORE DETAILS HERE>',
    aboveText='<p>Add any of these details to the note below to help your doctor get a full picture of your health. Factors that can impact your BP include:</p>
<ul>
<li>Missed doses of medicines over the last week</li>
<li>Started a new medication</li>
<li>Been under stress recently</li>
<li>Started an over the counter supplement</li>
<li>Changed my diet</li>
</ul>',
    belowText='<p class="critical">For urgent medical questions, call your clinic. Call 911 if this is an emergency.  Average clinic response time is 2-3 business days.</p>'
where messageKey='recommend-pharma'
   or messageKey='recommend-advancing-treatment';

update contact_message
set body='I experienced a side effect recently, I believe due to blood pressure management.  To safely manage my blood pressure at home, what steps would you like me to take next?  <ADD MORE DETAILS HERE>',
    aboveText='<p>Add details regarding your side effect to the message below. These details can help your doctor get a full picture of your health. <a href="/side-effects" target="_blank">Click here</a> for a complete list of side effects. Relevant details could be:</p>
<ul>
<li>Details about side effect</li>
<li>Frequency and duration</li>
<li>When it started</li>
<li>Any triggers</li>
<li>Medication information</li>
</ul>',
    belowText='<p class="critical">For urgent medical questions, call your clinic. Call 911 if this is an emergency.  Average clinic response time is 2-3 business days.</p>'
where messageKey='untreated-adverse-event';
