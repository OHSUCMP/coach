function loadOtherGoals(_callback) {
    $.ajax({
        method: "POST",
        url: "/goals/other-goals"
    }).done(function(goals, textStatus, jqXHR) {
        if (jqXHR.status === 200) {
            _callback(goals);
        }
    });
}

function populateOtherGoals() {
    let data = window.otherGoals;

    let html = '';

    if (Array.isArray(data) && data.length > 0) {
        data.forEach(function(g, index) {
            let inProgress = g.achievementStatus === 'IN_PROGRESS';
            let c = inProgress ? 'active' : 'completed';

            // goal info and actions
            html += "<div class='col-lg-6 mt-2'>";
            html += "<div class='card h-100 goal' data-extGoalId='" + g.extGoalId + "'>";
            html += "<div class='card-header fw-bold'>" + g.referenceDisplay + "</div>";
            html += "<div class='card-body'>";
            html += "<p class='mb-0'><strong>Your goal:</strong> " + g.goalText + "</p>";
            html += "<p class='mb-0'><strong>Target completion date:</strong> " + toDateString(g.targetDate) + "</p>";
            html += "<p class='mb-0'><strong>Current status:</strong> <span class='" + c + "'>" + g.achievementStatusLabel + "</span></p>";
            html += "<div class='mt-4 d-flex justify-content-evenly'>";
            if (inProgress) {
                html += "<button class='btn button-primary markAchieved'>Mark Achieved</button>";
                html += "<button class='btn button-primary markNotAchieved'>Mark Not Achieved</button>";
            } else {
                html += "<button class='btn button-primary markInProgress'>Mark In Progress</button>";
            }
            html += "</div>"
            html += "<div class='accordion mt-4' id='goalHistory'><div class='accordion-item'>";
            html += "<h2 class='accordion-header' id='flush-heading" + index + "'></h2>";
            html += "<button class='accordion-button collapsed' type='button' data-bs-toggle='collapse' data-bs-target='#flush-collapse" + index + "' aria-expanded='false' aria-controls='flush-collapse" + index + "'>Goal History</button>";
            html += "</h2>";
            html += "<div id='flush-collapse" + index + "' class='accordion-collapse collapse' aria-labelledby='flush-heading" + index + "'>";
            html += "<div class='accordion-body'>";
            html += "<table class='table table-striped'>";
            html += "<thead><tr><th>Status</th><th>Date</th></tr></thead>";
            html += "<tbody>";
            if (g.history) {
                g.history.forEach(function(item) {
                    html += "<tr>";
                    html += "<td>" + item.achievementStatusLabel + "</td>";
                    html += "<td class='date'>" + toDateTimeString(item.createdDate) + "</td>";
                    html += "</tr>";
                });
            }
            html += "</tbody></table>";
            html += "</div></div></div></div></div></div></div>";
        });
    }

    $('#otherGoalsContainer').html(html);
}

function updateStatus(el, status) {
    let goal = $(el).closest('.goal');
    let extGoalId = $(goal).attr('data-extGoalId');

    let data = {
        extGoalId: extGoalId,
        achievementStatus: status
    };

    $.ajax({
        method: "POST",
        url: "/goals/update-status",
        data: data
    }).done(function(data, textStatus, jqXHR) {
        if (jqXHR.status === 200) {
            loadOtherGoals(function(otherGoals) {
                window.otherGoals = otherGoals;
                populateOtherGoals();
            });
        }
    });
}

function updateBPGoal(bpGoalData, _callback) {
    let formData = new FormData();
    formData.append("systolicTarget", bpGoalData.systolicTarget);
    formData.append("diastolicTarget", bpGoalData.diastolicTarget);

    $.ajax({
        method: "POST",
        url: "/goals/update-bp",
        data: bpGoalData
    }).done(function(bpGoal, textStatus, jqXHR) {
        _callback(jqXHR.status, bpGoal);
    });
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
        $(button).prop( "disabled", false );
    } else {
        $(button).prop( "disabled", true );
    }
}

$(document).on('change', 'input.systolic, input.diastolic', function() {
    enableDisableUpdateBPGoalButton();
});

$(document).on('click', '#updateBPGoal', function() {
    let container = $(this).closest('.bpGoal');

    let systolic = $(container).find('input.systolic');
    let diastolic = $(container).find('input.diastolic');

    let bpGoalData = {
        systolicTarget: $(systolic).val(),
        diastolicTarget: $(diastolic).val()
    };

    updateBPGoal(bpGoalData, function (status, bpGoal) {
        let note = $('#updateNote');
        if (status === 200) {
            $(container).attr('data-systolic', bpGoal.systolicTarget);
            $(container).attr('data-diastolic', bpGoal.diastolicTarget);

            $(note).text("Goal updated successfully.");
            $(note).removeClass("error");
            $(note).addClass("success");

            $('#updateBPGoal').prop("disabled", true);

        } else {
            $(note).text("Error updating goal - see logs for details.");
            $(note).removeClass("success");
            $(note).addClass("error");
        }
    });
});

$(document).on('click', '.markInProgress:button', function() {
    updateStatus(this, 'IN_PROGRESS');
});

$(document).on('click', '.markAchieved:button', function() {
    updateStatus(this, 'ACHIEVED');
});

$(document).on('click', '.markNotAchieved:button', function() {
    updateStatus(this, 'NOT_ACHIEVED');
});

$(document).ready(function() {
    loadOtherGoals(function(otherGoals) {
        window.otherGoals = otherGoals;
        populateOtherGoals();
    });
});