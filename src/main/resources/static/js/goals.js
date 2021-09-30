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

async function updateBPGoal(g, _callback) {
    let formData = new FormData();
    formData.append("systolicTarget", g.systolicTarget);
    formData.append("diastolicTarget", g.diastolicTarget);

    let response = await fetch("/goals/update-bp", {
        method: "POST",
        body: formData
    });

    await response.text();

    _callback(response.status, g);
}

function enableDisableUpdateBPGoalButton() {
    let el = $('#bpContent .bpGoal');
    let systolic = $(el).find('input.systolic').val();
    let diastolic = $(el).find('input.diastolic').val();
    let origSystolic = $(el).attr('data-systolic');
    let origDiastolic = $(el).attr('data-diastolic');
    let changed = (systolic !== origSystolic || diastolic !== origDiastolic);

    let button = $('#updateBPGoal');
    if (changed) {
        $(button).removeClass('disabled');
    } else {
        $(button).addClass('disabled');
    }
}

$(document).ready(function() {
    enableHover('.button');
    enableHover('.bpGoal .link');
});

$(document).on('change', 'input.systolic, input.diastolic', function() {
    enableDisableUpdateBPGoalButton();
});

$(document).on('click', '#updateBPGoal:not(.disabled)', function() {
    let container = $(this).closest('.bpGoal');

    let systolic = $(container).find('input.systolic');
    let diastolic = $(container).find('input.diastolic');

    let g = {};
    g.systolicTarget = $(systolic).val();
    g.diastolicTarget = $(diastolic).val();

    updateBPGoal(g, function (status, g) {
        let note = $('#updateNote');
        if (status === 200) {
            $(container).attr('data-systolic', g.systolicTarget);
            $(container).attr('data-diastolic', g.diastolicTarget);

            $(note).text("Goal updated successfully.");
            $(note).removeClass("error");
            $(note).addClass("success");

            $('#updateBPGoal').addClass('disabled');

        } else {
            $(note).text("Error updating goal - see logs for details.");
            $(note).removeClass("success");
            $(note).addClass("error");
        }
    });
});