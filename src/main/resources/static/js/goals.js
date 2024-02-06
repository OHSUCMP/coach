function isEnhanced() {
    return $('#randomizationGroup').text() === 'ENHANCED';
}

function loadOtherGoals(_callback) {
    $.ajax({
        method: "POST",
        url: "/goals/other-goals"
    }).done(function(goals) {
        _callback(goals);
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
            html += "</div>";
            html += "<span class=\"note d-inline-block hidden mt-4\"></span>";
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
    let note = $(goal).find('.note');

    let data = {
        extGoalId: extGoalId,
        achievementStatus: status
    };

    $.ajax({
        method: "POST",
        url: "/goals/update-status",
        data: data
    }).done(function() {
        $(note).addClass('hidden');
        loadOtherGoals(function(otherGoals) {
            window.otherGoals = otherGoals;
            populateOtherGoals();
        });
    }).fail(function() {
        $(note).text("Error updating status - see logs for details.");
        $(note).removeClass('hidden');
        $(note).addClass("error");
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
    }).fail(function(jqXHR) {
        _callback(jqXHR.status);
    });
}

function enableDisableUpdateBPGoalButton() {
    let el = $('#bpContent .bpGoal');
    let systolic = $('#systolic').val();
    let diastolic = $('#diastolic').val();
    let origSystolic = $(el).attr('data-systolic');
    let origDiastolic = $(el).attr('data-diastolic');
    let changed = (systolic !== origSystolic || diastolic !== origDiastolic);

    let button = $('#updateBPGoal');
    let note = $('#updateNote');
    if (changed) {
        let pass = true;
        let messages = [];

        $(el).find('input').each(function() {
            let minStr = $(this).attr('data-min');
            let maxStr = $(this).attr('data-max');
            if (minStr !== '' && maxStr !== '') {   // validate as a number if either min and / or max is defined
                let label = $(this).attr('data-label');
                let valStr = $(this).val().trim();
                if (valStr === '') {
                    pass = false;
                    messages.push(label + ' is required.');
                    $(this).addClass('error');

                } else {
                    let val = parseInt(valStr);
                    let min = parseInt(minStr);
                    let max = parseInt(maxStr);
                    if (val < min || val > max) {
                        pass = false;
                        messages.push(label + ' must be between ' + min + ' and ' + max + '.');
                        $(this).addClass('error');
                    } else {
                        $(this).removeClass('error');
                    }
                }
            }
        });

        if (pass) {
            $(button).prop( "disabled", false );
            $(note).text('');
            $(note.removeClass('error'));
            $(note.removeClass('success'));

        } else {
            $(button).prop( "disabled", true );
            $(note).text(messages.join(' '));
            $(note).addClass('error');
        }

    } else {
        $(el).find('input').each(function() {
            $(this).removeClass('error');
        });
        $(button).prop( "disabled", true );
        $(note).text('');
        $(note.removeClass('error'));
        $(note.removeClass('success'));
    }
}

$(document).on('change', '#systolic, #diastolic', function() {
    enableDisableUpdateBPGoalButton();
});

$(document).on('click', '#updateBPGoal', function() {
    let container = $(this).closest('.bpGoal');

    let systolic = $('#systolic');
    let diastolic = $('#diastolic');

    let bpGoalData = {
        systolicTarget: $(systolic).val(),
        diastolicTarget: $(diastolic).val()
    };

    updateBPGoal(bpGoalData, function (status, bpGoal) {
        let note = $('#updateNote');
        if (status === 200) {  // verified updateBPGoal executes callback 'always'
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
    if (isEnhanced()) {
        loadOtherGoals(function(otherGoals) {
            window.otherGoals = otherGoals;
            populateOtherGoals();
        });
    }
});