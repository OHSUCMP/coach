function enableDatePicker(sel) {
    let minDate = new Date();
    minDate.setDate(minDate.getDate() + 1);

    $(sel).datepicker({
        changeMonth: true,
        changeYear: true,
        showButtonPanel: true,
        showOn: 'focus', //'button',
        constrainInput: true,
        dateFormat: 'mm-dd-yy',
        minDate: minDate,
        gotoCurrent: true //,
    });
}

function getRecommendations(_callback) {
    $(".recommendation").each(function () {
        let recommendationId = $(this).attr('data-id');
        let cardsContainer = $(this).find('.cardsContainer');
        getRecommendation(recommendationId, function (cards) {
            _callback(cardsContainer, cards);
            enableDatePicker($(cardsContainer).find('.goalTargetDate'));
        });
    });
}

// get a recommendation from cache, but do not execute it
function getRecommendation(id, _callback) {
    let data = {
        id: id
    };

    $.ajax({
        method: "POST",
        url: "/recommendation",
        timeout: 3600000,
        data: data
    }).done(function(cards, textStatus, jqXHR) {
        if (jqXHR.status === 200) {
            _callback(cards);
        }
    });
}

function renderCards(cards) {
    if (cards === undefined || cards === null || cards.length === 0) {
        return '';
    }

    let html = "";
    cards.forEach(function (card) {
        html += "<div class='card'>";

        if (card.prefetchModified) {
            $('#prefetchModifiedInfo.hidden').removeClass('hidden');
        }

        if (card.errorMessage !== null) {
            html += "<div class='error'>" + card.errorMessage + "</div>";

        } else {
            html += "<div class='container'><div class='row'>";
            html += "<div class='col-1 p-0 pl-1 pt-1' style='min-width:40px;'><img src='/images/" + card.indicator + "-icon.png' class='icon' alt='" + card.indicator + "' /></div>";
            html += "<div class='col-11 col-md-4 content p-1'>";
            html += "<span class='summary heading'>" + card.summary + "</span>";

            if (card.rationale !== null) {
                html += "<span class='rationale'>" + card.rationale + "</span>";
            }

            if (card.source.label !== null && card.source.url !== null) {
                html += "<span class='source'>";
                html += "<a href='" + card.source.url + "' target='_blank' rel='noopener noreferrer'>" +
                    card.source.label + "</a>";
                html += "</span>";
            }

            if (card.links !== null) {
                html += "<div class='links'>";
                card.links.forEach(function(link) {
                    html += "<a class='link' href='" + link.url + "'>" + link.label + "</a>";
                });
                html += "</div>";
            }

            html += buildCounselingHTML(card.suggestions);

            html += "</div><div class='col-12 col-md-7 p-1'>"

            html += buildAdverseEvents(card.suggestions);
            html += buildGoalsHTML(card.suggestions);
            html += buildLinksHTML(card.suggestions);

            html += "</div></div></div>"
        }
        html += "</div>";
    });
    return html;
}

function buildCounselingHTML(suggestions) {
    let html = "";
    if (suggestions !== null) {
        suggestions.forEach(function(s) {
            if (s.type === 'counseling-link') {
                html += "<div class='counseling' data-id='" + s.id + "' data-reference-system='" + s.references.system + "' data-reference-code='" + s.references.code + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                if (s.actions !== null) {
                    html += "<ul class='actions'>";
                    s.actions.forEach(function(action) {
                        html += "<li class='action'><a href='" + action.url + "'>" + action.label + "</a></li>";
                    });
                    html += "</ul>";
                }
                html += "</div>";
            }
        });
    }
    return html !== "" ?
        "<div class='counselingContainer'>" + html + "</div>" :
        "";
}

function buildAdverseEvents(suggestions) {
    let html = "";
    if (suggestions != null) {
        suggestions.forEach(function(s) {
            if (s.type === 'adverse-event') {
                html += "<div class='adverseEvent' data-id='" + s.id + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tr>";
                html += "<td class='expand'>";

                let x = randomChars(5);
                html += "<div class='action'>";
                html += "<input name='action" + x + "' type='radio' id='action" + x + "_y' value='yes' />";
                html += "<label for='action" + x + "_y'>Yes</label></div>";
                html += "</div>";

                html += "<div class='action'>";
                html += "<input name='action" + x + "' type='radio' id='action" + x + "_n' value='no' />";
                html += "<label for='action" + x + "_y'>No</label></div>";
                html += "</div>";

                html += "</td>";
                html += "<td class='shrink'><div><button class='btn btn-sm button-primary registerAdverseEventAction'>Register Action</button></div></td>";
                html += "</tr>";

                html += "</tr></table>";
                html += "</div>";
            }
        });
    }

    if (html !== "") {
        html = "<div class='adverseEventsContainer'>" +
            "<div class='heading'>Have you discussed any of these conditions with your care team?</div>" +
            html +
            "</div>";
    }

    return html;
}

function buildGoalsHTML(suggestions) {
    let html = "";
    if (suggestions !== null) {
        suggestions.forEach(function(s) {
            if (s.type === 'goal' || s.type === 'bp-goal') {
                let c = s.type === 'bp-goal' ? 'bpGoal' : 'goal';

                html += "<div class='" + c + " p-2' data-id='" + s.id +
                    "' data-reference-system='" + s.references.system +
                    "' data-reference-code='" + s.references.code +
                    "' + data-reference-display='" + s.references.display + "'>";

                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tr>";

                html += "<td class='expand'>";
                if (s.type === 'goal') {
                    if (s.actions === null || s.actions.length === 0) {
                        // freeform input
                        html += "<div class='action'>";
                        html += "<input type='text' placeholder='Describe your goal here' />";
                        html += "</div>";

                    } else {
                        // predefined multiple-choice goal, these are radio buttons
                        let i = 0;
                        let x = randomChars(5);
                        s.actions.forEach(function (action) {
                            html += "<div class='action'>";
                            let arr = buildGoalInputData(action.label);
                            if (arr.length === 1) {
                                // presume that if there's just one thing in the array, that it's a fixed response,
                                // it's the only thing that makes sense
                                html += "<input name='action" + x + "' type='radio' id='action" + x + "_" + i + "' value='" + action.label + "' />";
                                html += "<label for='action" + x + "_" + i + "'>" + action.label + "</label>";

                            } else {
                                html += "<input name='action" + x + "' type='radio' class='madlib' id='action" + x + "_" + i + "' />";
                                arr.forEach(function(item) {
                                    if (typeof(item) === 'string') {
                                        html += "<span class='madlibResponse'>" + item + '</span> ';

                                    } else {
                                        // html += "<input type='text' class='madlibResponse' data-type='" + item.type + "' placeholder='" + item.label + "' disabled/> ";
                                        html += "<input type='text' class='madlibResponse' placeholder='" + item.label + "'";
                                        if (item.defaultValue) {
                                            html += " value='" + item.defaultValue + "'";
                                        }
                                        html += " disabled/> ";
                                    }
                                });
                            }

                            html += "</div>";
                            i++;
                        });

                        html += "<div class='action'>";
                        html += "<input name='action" + x + "' type='radio' class='freetext' />";
                        html += "<input type='text' class='freetextResponse' placeholder='Describe your goal here' disabled/>";
                        html += "</div>";
                    }

                } else if (s.type === 'bp-goal') {
                    if (s.actions === null || s.actions.length === 0) {
                        // freeform input
                        html += "<div class='action'>";
                        html += "<input type='text' class='systolic' placeholder='Systolic' /> /";
                        html += "<input type='text' class='diastolic' placeholder='Diastolic' />";
                        html += "</div>";

                    } else {
                        // predefined multiple-choice goal, these are radio buttons
                        let i = 0;
                        let x = randomChars(5);
                        s.actions.forEach(function (action) {
                            let bpdata = parseBPData(action.label);
                            html += "<div class='action'>";
                            html += "<input name='action" + x + "' type='radio' id='action" + x + "_" + i + "' value='" + action.label + "' data-systolic='" + bpdata.systolic + "' data-diastolic='" + bpdata.diastolic + "' />";
                            html += "<label for='action" + x + "_" + i + "'>" + action.label + "</label></div>";
                            i++;
                        });

                        html += "<div class='action'>";
                        html += "<input name='action" + x + "' type='radio' class='custom' />";
                        html += "<input type='text' class='customResponse systolic' placeholder='Systolic' disabled/> / ";
                        html += "<input type='text' class='customResponse diastolic' placeholder='Diastolic' disabled/>";
                        html += "</div>";
                    }
                }
                html += "</td>";
                html += "<td class='shrink'><div><button class='btn btn-sm button-primary commitToGoal'>Commit to Goal</button></div></td>";
                html += "</tr>";

                if (s.type === 'goal') {
                    html += "<tr>";
                    let y = randomChars(5);
                    html += "<td><label for='goalTargetDate" + y + "'>When do you want to achieve this goal?</label></td>";
                    html += "<td><input id='goalTargetDate" + y + "' type='text' class='goalTargetDate' placeholder='--Select Date--' readOnly/></td>";
                    html += "</tr>";
                }

                html += "</table>";
                html += "</div>";

            } else if (s.type === 'update-goal') {
                html += "<div class='goal p-2' data-id='" + s.id + "' data-reference-system='" + s.references.system + "' data-reference-code='" + s.references.code + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tr><td>";

                let id = randomChars(5);

                html += "<div><label for='achievementStatus" + id + "'>Achievement Status:</label> <select id='achievementStatus" + id + "' class='achievementStatus'>";

                let a_arr = ['IN_PROGRESS', 'ACHIEVED', 'NOT_ACHIEVED'];

                let a_status = s.goal ?
                    s.goal.achievementStatus :
                    'UNKNOWN';

                a_arr.forEach(function(value) {
                    html += "<option value='" + value + "'";
                    if (value === a_status) {
                        html += " selected";
                    }
                    html += ">" + toLabel(value) + "</option>";
                });
                html += "</select></div>";
                html += "</td>";
                html += "<td class='shrink'><div class='mb-3 me-3'><button class='btn btn-sm button-primary updateGoal'>Record Progress</button></div></td>";
                html += "</td>";
                html += "</tr><tr>";

                html += "</td></tr></table>";
                html += "</div>";
            }
        });
    }
    return html !== "" ?
        "<div class='goalsContainer'>" + html + "</div>" :
        "";
}

function toLabel(string) {
    let words = string.replace(/_/g, ' ').toLowerCase().split(" ");

    let label = words.map(function(word) {
        return word[0].toUpperCase() + word.substring(1);
    }).join(" ");

    return label;
}

function buildGoalInputData(s) {
    let arr = [];
    let buf = [];
    let chars = s.split('');
    while (chars.length > 0) {
        let c = chars.shift();

        // parse madlib token
        if (c === '[') {
            if (buf.length > 0) {
                arr.push(buf.join('').trim());
                buf = [];
            }
            while (c !== ']' && chars.length > 0) {
                c = chars.shift();
                if (c !== ']') {
                    buf.push(c);
                }
                // }
            }
            let buf_arr = buf.join('').split(':');
            let obj = {
                label: buf_arr[0]
            };
            if (buf_arr.length > 1) {
                obj.defaultValue = buf_arr[1];
            }
            arr.push(obj);
            buf = [];

        } else {
            buf.push(c);
        }
    }
    if (buf.length > 0) {
        arr.push(buf.join('').trim());
    }

    return arr;
}

function buildLinksHTML(suggestions) {
    let html = "";
    if (suggestions !== null) {
        suggestions.forEach(function(s) {
            if (s.type === 'suggestion-link') {
                html += "<div class='slink p-2'>";      // 'slink' to differentiate from 'link'
                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tbody>";

                // Iterate through links
                s.actions.forEach(function(action) {
                	html += "<tr><td>";
                	html += "<a href='" + action.url + "'>" + action.label + "</a>";
                	html += "</td></tr>";
                });
 
                html += "</tbody></table>";
                html += "</div>";
            }
        });
    }
    return html !== "" ?
        "<div class='linksContainer'>" + html + "</div>" :
        "";
}

function parseBPData(s) {
    // expected format: "{systolic}/{diastolic}"
    let regex = /^.*?(\d+)\s*\/\s*(\d+).*?$/
    let match = regex.exec(s);

    let obj = {};
    obj.systolic = match[1];
    obj.diastolic = match[2];
    return obj;
}

function buildGoalData(button) {
    let goal = $(button).closest('.goal');
    let obj = {};
    obj.extGoalId = $(goal).attr('data-id');
    obj.referenceSystem = $(goal).attr('data-reference-system');
    obj.referenceCode = $(goal).attr('data-reference-code');
    obj.referenceDisplay = $(goal).attr('data-reference-display');
    obj.goalText = getGoalText(goal);
    obj.systolicTarget = 0;
    obj.diastolicTarget = 0;
    let targetDate = $(goal).find('.goalTargetDate').datepicker('getDate');
    obj.targetDateTS = $.datepicker.formatDate('@', targetDate);
    return obj;
}

function buildBPGoalData(button) {
    let goal = $(button).closest('.bpGoal');
    let obj = {};
    let target = getGoalBPTarget(goal);
    obj.systolicTarget = target.systolic;
    obj.diastolicTarget = target.diastolic;
    return obj;
}

function getGoalText(goal) {
    let action = $(goal).find('.action');

    if ($(action).length === 1) {
        return $(action).find('input').val();

    } else {
        let radio = $(action).find("input[type='radio']:checked");
        if ($(radio).hasClass('freetext')) {
            return $(radio).siblings('input.freetextResponse').val();

        } else if ($(radio).hasClass('madlib')) {
            let parts = [];
            $(radio).siblings('.madlibResponse').each(function() {
                if ($(this).is('span')) {
                    parts.push(this.innerHTML);

                } else {
                    parts.push($(this).val());
                }
            });
            return parts.join(' ');

        } else {
            return $(radio).val();
        }
    }
}

function getGoalBPTarget(bpGoal) {
    let action = $(bpGoal).find('.action');

    if ($(action).length === 1) {
        let obj = {};
        obj.systolic = $(action).find('input.systolic').val();
        obj.diastolic = $(action).find('input.diastolic').val();
        return obj;

    } else {
        let radio = $(action).find("input[type='radio']:checked");
        if ($(radio).hasClass('custom')) {
            let obj = {};
            obj.systolic = $(radio).siblings('input.customResponse.systolic').val();
            obj.diastolic = $(radio).siblings('input.customResponse.diastolic').val();
            return obj;

        } else {
            let obj = {};
            obj.systolic = $(radio).attr('data-systolic');
            obj.diastolic = $(radio).attr('data-diastolic');
            return obj;
        }
    }
}

function buildGoalUpdateData(button) {
    let goal = $(button).closest('.goal');
    let obj = {};
    obj.extGoalId = $(goal).attr('data-id');
    obj.achievementStatus = $(goal).find('.achievementStatus').find(':selected').val();
    return obj;
}

function buildCounselingData(a) {
    let counseling = $(a).closest('.counseling');
    let obj = {};
    obj.extCounselingId = $(counseling).attr('data-id');
    obj.referenceSystem = $(counseling).attr('data-reference-system');
    obj.referenceCode = $(counseling).attr('data-reference-code');
    obj.counselingText = a.innerText;
    return obj;
}

function registerCounselingReceived(counselingData, _callback) {
    $.ajax({
        method: "POST",
        url: "/counseling/create",
        data: counselingData
    }).done(function(data, textStatus, jqXHR) {
        _callback(jqXHR.status);
    });
}

function createGoal(goalData, _callback) {
    $.ajax({
        method: "POST",
        url: "/goals/create",
        data: goalData
    }).done(function(data, textStatus, jqXHR) {
        _callback(jqXHR.status);
    });
}

function updateBPGoal(bpGoalData, _callback) {
    $.ajax({
        method: "POST",
        url: "/goals/update-bp",
        data: bpGoalData
    }).done(function(data, textStatus, jqXHR) {
        _callback(jqXHR.status, bpGoalData);
    });
}

function updateGoal(goalUpdateData, _callback) {
    $.ajax({
        method: "POST",
        url: "/goals/update-status",
        data: goalUpdateData
    }).done(function(data, textStatus, jqXHR) {
        _callback(jqXHR.status);
    });
}

function registerAdverseEventAction(adverseEventData, _callback) {
    $.ajax({
        method: "POST",
        url: "/adverse-event/register-action",
        data: adverseEventData
    }).done(function(data, textStatus, jqXHR) {
        _callback(jqXHR.status);
    });
}

function buildAdverseEventData(button) {
    let ae = $(button).closest('.adverseEvent');
    let obj = {};
    obj.adverseEventId = $(ae).attr('data-id');

    let action = $(ae).find('.action');
    let val = $(action).find("input[type='radio']:checked").val();
    obj.actionTaken = val === 'yes';

    return obj;
}


function hide(el, _complete) {
    $(el).addClass('hidden');
    if (_complete !== undefined) {
        _complete(el);
    }
//    $(el).fadeOut(400, _complete(el));
}

$(document).on('click', '.goal .commitToGoal', function() {
    let container = $(this).closest('.goal');

    let goalData = buildGoalData(this);
    createGoal(goalData, function(status) {
        if (status === 200) {
            hide(container);
        }
    });
});

$(document).on('click', '.bpGoal .commitToGoal', function() {
    let container = $(this).closest('.bpGoal');

    let bpGoalData = buildBPGoalData(this);
    updateBPGoal(bpGoalData, function(status, g) {
        if (status === 200) {
            let el = $('#currentBPGoal');
            $(el).attr('data-systolic', g.systolicTarget);
            $(el).attr('data-diastolic', g.diastolicTarget);
            $(el).html("Your Current Blood Pressure Goal: <em><strong>Below " + g.systolicTarget + "/" + g.diastolicTarget + "</strong></em>");

            updateChart();

            hide(container);
        }
    });
});

$(document).on('click', '.goal .updateGoal', function() {
    let container = $(this).closest('.goal');

    let goalUpdateData = buildGoalUpdateData(this);
    updateGoal(goalUpdateData, function(status) {
        if (status === 200) {
            hide(container);
        }
    });
});

$(document).on('click', '.counseling .actions a', function(event) {
    event.preventDefault();
    let a = $(this);
    let counselingData = buildCounselingData(this);
    registerCounselingReceived(counselingData, function(status) {
        window.location.href = $(a).attr('href');
    });
});

$(document).on('click', '.adverseEvent .registerAdverseEventAction', function() {
    let container = $(this).closest('.adverseEvent');

    let adverseEventData = buildAdverseEventData(this);
    registerAdverseEventAction(adverseEventData, function(status) {
        if (status === 200) {
            hide(container, function(el) {
                let parent = $(el).closest('.adverseEventsContainer');
                let anyChildrenVisible = $(parent).find('.adverseEvent:visible').length > 0;
                if (parent.is(':visible') && ! anyChildrenVisible) {
                    hide(parent);
                }
            });
        }
    });
});

$(document).on('click', '.goal .action input[type="radio"]', function() {
    let ftrmlr = $(this).closest('.goal').find('input.freetextResponse,input.madlibResponse');
    $(ftrmlr).prop('disabled', true);

    if ($(this).hasClass('freetext')) {
        let ftr = $(this).siblings('input.freetextResponse');
        $(ftr).prop('disabled', false);
        $(ftr).focus();

    } else if ($(this).hasClass('madlib')) {
        let mlr = $(this).siblings('input.madlibResponse');
        $(mlr).prop('disabled', false);
        $(mlr).first().focus();
    }
});

$(document).on('click', '.bpGoal .action input[type="radio"]', function() {
    let el = $(this).closest('.bpGoal').find('input.customResponse');
    if ($(this).hasClass('custom')) {
        $(el).prop('disabled', false);
        $(el).first().focus();

    } else {
        $(el).prop('disabled', true);
    }
});
