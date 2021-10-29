async function loadOtherGoals(_callback) {
    let response = await fetch("/goals/other-goals", {
        method: "POST",
        headers: {
            "Content-Type": "application/json; charset=utf-8"
        }
    });

    let goals = await response.json();

    _callback(goals);
}

function populateOtherGoals() {
    let data = window.otherGoals;

    let html = '<table id="goalsTable" class="no-spacing">';

    if (Array.isArray(data) && data.length > 0) {
        data.forEach(function(g) {
            let inProgress = g.achievementStatus === 'IN_PROGRESS';
            let c = inProgress ? 'active' : 'completed';

            // goal info and actions
            html += "<tr class='goal' data-extGoalId='" + g.extGoalId + "'><td>";
            html += "<div class='goal " + c + "'>";
            html += "<span class='heading'>" + g.referenceDisplay + "</span>";
            html += "<table><tr><td class='expand'>";
            html += "<div>Your goal:<span class='goalText'>" + g.goalText + "</span></div>";
            html += "<div>Target completion date:<span class='targetDate'>" + toDateString(g.targetDate) + "</span></div>";
            html += "<div>Current status:<span class='" + c + "'>" + g.achievementStatusLabel + "</span></div>";
            html += "</td><td class='shrink'>";
            if (inProgress) {
                html += "<div class='markAchieved button'>Mark Achieved</div>";
                html += "<div class='markNotAchieved button'>Mark Not Achieved</div>";
            } else {
                html += "<div class='markInProgress button'>Mark In Progress</div>";
            }
            html += "</td></tr></table>";
            html += "</div>";
            html += "</td>";

            // goal history
            html += "<td><table class='goalHistory'>";
            html += "<tr class='goalHistoryHeader'>";
            html += "<th>Achievement Status</th>";
            html += "<th>Date</th>";
            html += "</tr>";
            if (g.history) {
                g.history.forEach(function(item) {
                    html += "<tr>";
                    html += "<td>" + item.achievementStatusLabel + "</td>";
                    html += "<td class='date'>" + toDateTimeString(item.createdDate) + "</td>";
                    html += "</tr>";
                });
            }
            html += "</table></td></tr>";
        });
    }
    html += "</table>";

    $('#otherGoalsContainer').html(html);
}

async function updateStatus(el, status) {
    let goal = $(el).closest('tr.goal');
    let extGoalId = $(goal).attr('data-extGoalId');

    let formData = new FormData();
    formData.append("extGoalId", extGoalId);
    formData.append("achievementStatus", status);

    let response = await fetch("/goals/update-status", {
        method: "POST",
        body: formData
    });

    if (response.status === 200) {
        await loadOtherGoals(function(otherGoals) {
            window.otherGoals = otherGoals;
            populateOtherGoals();
        });
    }
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

$(document).on('click', '#goalsTable .markInProgress.button', function() {
    updateStatus(this, 'IN_PROGRESS');
});

$(document).on('click', '#goalsTable .markAchieved.button', function() {
    updateStatus(this, 'ACHIEVED');
});

$(document).on('click', '#goalsTable .markNotAchieved.button', function() {
    updateStatus(this, 'NOT_ACHIEVED');
});
