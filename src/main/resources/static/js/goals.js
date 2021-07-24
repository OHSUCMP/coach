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

async function updateStatus(el, status) {
    let extGoalId = $(el).closest('tr').attr('data-extGoalId');

    let formData = new FormData();
    formData.append("extGoalId", extGoalId);
    formData.append("achievementStatus", status);

    let response = await fetch("/goals/set-status", {
        method: "POST",
        body: formData
    });

    let goalHistory = await response.json();
    if (goalHistory) {
        let goalRow = $('tr.goal.data[data-extGoalId="' + extGoalId + '"]');
        if (goalRow) {
            $(goalRow).children('td.status').html(goalHistory.achievementStatus);
            $(goalRow).children('td.actions').html(buildActionsHTML(goalHistory.achievementStatus));
        }
        let historyTable = $('tr.goal.history[data-extGoalId="' + extGoalId + '"]').find('table');
        if (historyTable) {
            $(historyTable).find('tr:last').after(buildHistoryHTML(goalHistory));
        }
    }
}

function buildActionsHTML(status) {
    // this should be kept synchronized with goals.mustache
    return status === 'IN_PROGRESS' ?
        '<div class="markAchieved link">Mark Achieved</div> <div class="markNotAchieved link">Mark Not Achieved</div>' :
        '<div class="markInProgress link">Mark In Progress</div>';
}

function buildHistoryHTML(goalHistory) {
    return "<tr><td>" + goalHistory.achievementStatus + "</td><td>" + goalHistory.createdDate + "</td></tr>\n";
}