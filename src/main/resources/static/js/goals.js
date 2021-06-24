async function getRecordedGoals() {
    let response = await fetch("/goals/list", {
        method: "GET"
    });

    let goals = await response.json();

    return goals;
}

async function createGoal(g, _callback) {
    let formData = new FormData();
    formData.append("extGoalId", g.extGoalId);
    formData.append("referenceSystem", g.referenceSystem);
    formData.append("referenceCode", g.referenceCode);
    formData.append("goalText", g.goalText);
    formData.append("followUpDays", g.followUpDays || 0);

    let response = await fetch("/goals/create", {
        method: "POST",
        body: formData
    });

    let goal = await response.json();
    if (goal) {
        _callback(goal);
    }
}

// function appendGoalToTable(goal) {
//     let container = $('#goalsTable');
//     let unsortedData = $(container).find('tr');
//     let status = goal.completed ? 'Completed' : 'Active';
//
//     // note : keep this section synced with goals.mustache
//
//     let html = "<tr class='data' data-goalid='" + goal.goalId + "'>" +
//         "<td>" + goal.goalText + "</td>" +
//         "<td class='status'>" + status + "</td>" +
//         "<td>" + goal.createdDate + "</td>" +
//         "<td class='actions'>" + buildActionLink(goal.created) + "</td>" +
//         "</tr>\n";
//
//     // now sort
//     // adapted from https://stackoverflow.com/questions/6133723/sort-divs-in-jquery-based-on-attribute-data-sort
//
//     let sortedData = $(unsortedData).add(html).sort(function(a,b) {
//         let tsA = $(a).data('timestamp');
//         let tsB = $(b).data('timestamp');
//         return (tsA < tsB) ? 1 : (tsA > tsB) ? -1 : 0;
//     });
//
//     $(container).html(sortedData);
// }

async function setCompleted(el, completed) {
    let extGoalId = $(el).closest('tr').attr('data-extGoalId');

    let formData = new FormData();
    formData.append("extGoalId", extGoalId);
    formData.append("completed", completed);

    let response = await fetch("/goals/setCompleted", {
        method: "PUT",
        body: formData
    });

    let goal = await response.json();
    if (goal) {
        let row = $('tr.data[data-extGoalId=' + goal.extGoalId + ']');
        if (row) {
            let status = goal.completed ? 'Completed' : 'Active';
            $(row).children('td.status').html(status);

            let action = buildLifecycleStatusDiv(goal.completed);
            $(row).children('td.actions').html(action);
        }
    }
}

function buildLifecycleStatusDiv(goal_completed) {

    // todo : enhance this to show all available Goal statuses

    return goal_completed ?
        '<span class="markActive link">Mark Active</span>' :
        '<span class="markCompleted link">Mark Completed</span>';
}

async function deleteGoal(extGoalId, _callback) {
    let formData = new FormData();
    formData.append("extGoalId", extGoalId);

    let response = await fetch("/goals/delete", {
        method: "POST",
        body: formData
    });

    let deletedExtGoalId = await response.text();
    if (deletedExtGoalId) {
        _callback(deletedExtGoalId);
    }
}
