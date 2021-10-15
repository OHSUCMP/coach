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

async function getRecommendations(_callback) {
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
async function getRecommendation(id, _callback) {
    let formData = new FormData();
    formData.append("id", id);
    let response = await fetch("/recommendation", {
        method: "POST",
        body: formData
    });
    let cards = await response.json();
    _callback(cards);
}

function renderCards(cards) {
    let html = "";
    if (cards === undefined || cards === null || cards.length === 0) {
        return html;
    }

    cards.forEach(function (card) {
        html += "<div class='card " + card.indicator + "'>";

        if (card.errorMessage !== null) {
            html += "<div class='error'>" + card.errorMessage + "</div>";

        } else {
            html += "<table style='width:100%'><tr><td>";
            html += "<div class='circle'><span>XX</span></div>"
            html += "</td><td class='expand'>";
            html += "<div class='content'>";
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

            html += "</td><td class='shrink'>";

            html += buildAdverseEvents(card.suggestions);

            html += buildGoalsHTML(card.suggestions);

            html += buildLinksHTML(card.suggestions);
            // if (card.selectionBehavior !== null) {
            //     html += "<span class='selectionBehavior'>" + card.selectionBehavior + "</span>";
            // }

            html += "</td></tr></table>";
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
                html += "<td class='shrink'><div class='registerAdverseEventAction'><span>Register Action</span></div></td>";
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

                html += "<div class='" + c + "' data-id='" + s.id +
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
                html += "<td class='shrink'><div class='commitToGoal'><span>Commit to Goal</span></div></td>";
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
                html += "<div class='goal' data-id='" + s.id + "' data-reference-system='" + s.references.system + "' data-reference-code='" + s.references.code + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tr><td>";

                let id = randomChars(5);

                html += "<div><label for='achievementStatus" + id + "'>Achievement Status:</label> <select id='achievementStatus" + id + "' class='achievementStatus'>";

                let a_arr = ['IN_PROGRESS', 'ACHIEVED', 'NOT_ACHIEVED'];
                let a_status = s.goal.achievementStatus;
                a_arr.forEach(function(value) {
                    html += "<option value='" + value + "'";
                    if (value === a_status) {
                        html += " selected";
                    }
                    html += ">" + toLabel(value) + "</option>";
                });

                html += "</select></div>";

                html += "</td><td>";

                html += "<div class='updateGoal'>Record Progress</div></td>";
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
    let words = string.replaceAll("_", " ").toLowerCase().split(" ");
    let label = words.map(word => {
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
                // if (c === ':') {
                //     label = buf.join('').trim();
                //     buf = [];
                // } else if (c === ']') {
                //     defaultValue = buf.join('').trim();
                //     buf = [];
                // } else {
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
                html += "<div class='link'>";
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
    obj.targetDate = $(goal).find('.goalTargetDate').datepicker('getDate');
    return obj;
}

function buildBPGoalData(button) {
    let goal = $(button).closest('.bpGoal');
    let obj = {};
    // g.extGoalId = $(goal).attr('data-id');
    // g.referenceSystem = $(goal).attr('data-reference-system');
    // g.referenceCode = $(goal).attr('data-reference-code');
    let target = getGoalBPTarget(goal);
    obj.systolicTarget = target.systolic;
    obj.diastolicTarget = target.diastolic;
    // g.targetDate = $(goal).find('.goalTargetDate').datepicker('getDate');
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

async function registerCounselingReceived(c, _callback) {
    let formData = new FormData();
    formData.append("extCounselingId", c.extCounselingId);
    formData.append("referenceSystem", c.referenceSystem);
    formData.append("referenceCode", c.referenceCode);
    formData.append("counselingText", c.counselingText);

    const response = await fetch("/counseling/create", {
        method: "POST",
        body: formData
    });

    await response.text();

    _callback(response.status);
}

async function createGoal(g, _callback) {
    let targetDateTS = $.datepicker.formatDate('@', g.targetDate);

    let formData = new FormData();
    formData.append("extGoalId", g.extGoalId);
    formData.append("referenceSystem", g.referenceSystem);
    formData.append("referenceCode", g.referenceCode);
    formData.append("referenceDisplay", g.referenceDisplay);
    formData.append("goalText", g.goalText);
    formData.append("targetDateTS", targetDateTS);

    let response = await fetch("/goals/create", {
        method: "POST",
        body: formData
    });

    await response.text();

    _callback(response.status);
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

async function updateGoal(g, _callback) {
    let formData = new FormData();
    formData.append("extGoalId", g.extGoalId);
    formData.append("achievementStatus", g.achievementStatus);

    let response = await fetch("/goals/set-status", {
        method: "POST",
        body: formData
    });

    await response.text();

    _callback(response.status);
}

async function registerAdverseEventAction(ae, _callback) {
    let formData = new FormData();
    formData.append("adverseEventId", ae.adverseEventId);
    formData.append("actionTaken", ae.actionTaken);

    let response = await fetch("/adverse-event/register-action", {
        method: "POST",
        body: formData
    });

    await response.text();

    _callback(response.status);
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
    _complete(el);
//    $(el).fadeOut(400, _complete(el));
}

$(document).ready(function() {
    enableHover('.commitToGoal');
    enableHover('.updateGoal');
    enableHover('.registerAdverseEventAction');
});

$(document).on('click', '.goal .commitToGoal', function() {
    let g = buildGoalData(this);
    let container = $(this).closest('.goal');

    createGoal(g, function(status) {
        if (status === 200) {
            hide(container);
        }
    });
});

$(document).on('click', '.bpGoal .commitToGoal', function() {
    let g = buildBPGoalData(this);
    let container = $(this).closest('.bpGoal');

    updateBPGoal(g, function(status, g) {
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
    let g = buildGoalUpdateData(this);
    let container = $(this).closest('.goal');

    updateGoal(g, function(status) {
        if (status === 200) {
            hide(container);
        }
    });
});

$(document).on('click', '.counseling .actions a', function(event) {
    event.preventDefault();
    let a = $(this);
    let c = buildCounselingData(this);
    registerCounselingReceived(c, function(status) {
        window.location.href = $(a).attr('href');
    });
});

$(document).on('click', '.adverseEvent .registerAdverseEventAction', function() {
    let ae = buildAdverseEventData(this);
    let container = $(this).closest('.adverseEvent');

    registerAdverseEventAction(ae, function(status) {
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
