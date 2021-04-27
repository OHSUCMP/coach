async function getRecordedGoals() {
    let response = await fetch("/goals/list", {
        method: "GET"
    });

    let goals = await response.json();

    return goals;
}

async function createGoal(goalId, goalText, followUpDays, _callback) {
    let formData = new FormData();
    formData.append("goalId", goalId);
    formData.append("goalText", goalText);
    formData.append("followUpDays", followUpDays || 0);

    let response = await fetch("/goals/create", {
        method: "POST",
        body: formData
    });

    let goal = await response.json();
    if (goal) {
        _callback(goal);
    }
}

function appendGoalToTable(goal) {
    let container = $('#goalsTable');
    let unsortedData = $(container).find('tr');
    let status = goal.completed ? "Active" : "Completed";

    // note : keep this section synced with bp-readings.mustache

    let html = "<tr class='data' data-goalid='" + goal.goalId + "'>" +
        "<td>" + goal.goalText + "</td>" +
        "<td>" + status + "</td>" +
        "<td>" + goal.createdDate + "</td>" +
        "<td><span class=\"link\" onclick=\"deleteGoal(" + goal.id + ")\">Delete</span></td>" +
        "</tr>\n";

    // now sort
    // adapted from https://stackoverflow.com/questions/6133723/sort-divs-in-jquery-based-on-attribute-data-sort

    let sortedData = $(unsortedData).add(html).sort(function(a,b) {
        let tsA = $(a).data('timestamp');
        let tsB = $(b).data('timestamp');
        return (tsA < tsB) ? 1 : (tsA > tsB) ? -1 : 0;
    });

    $(container).html(sortedData);
}

async function setCompleted(el, completed) {
    let goalId = $(el).closest('tr').attr('data-goalid');

    let formData = new FormData();
    formData.append("goalId", goalId);
    formData.append("completed", completed);

    let response = await fetch("/goals/setCompleted", {
        method: "PUT",
        body: formData
    });

    let goal = await response.json();
    if (goal) {
        let row = $('tr.data[data-goalid=' + goal.goalId + ']');
        if (row) {
            let status = goal.completed ? 'Completed' : 'Active';
            $(row).children('td.status').html(status);

            let action = goal.completed ?
                '<span class="link" onclick="setCompleted(this, false)">Mark Active</span>' :
                '<span class="link" onclick="setCompleted(this, true)">Mark Completed</span>';
            $(row).children('td.actions').html(action);
        }
    }

}

async function deleteGoal(goalId, _callback) {
    let formData = new FormData();
    formData.append("goalId", goalId);

    let response = await fetch("/goals/delete", {
        method: "POST",
        body: formData
    });

    let deletedGoalId = await response.text();
    if (deletedGoalId) {
        _callback(deletedGoalId);
    }
}
