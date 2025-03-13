-- fix side-effects link for #267
update contact_message
set aboveText=replace(aboveText, '/side-effects', '/resources/side-effects')
where messageKey='untreated-adverse-event';

