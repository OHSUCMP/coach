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

async function applyGoals() {
    let goals = await getRecordedGoals();
    $('.recommendation').each(function () {
        let cardsContainer = $(this).find('.cardsContainer');
        goals.forEach(function(goal) {
            $(cardsContainer).find('input.goal[data-extGoalId="' + goal.extGoalId + '"').each(function () {
                if ($(this).attr('type') === 'checkbox') {
                    $(this).prop('checked', true);
                }
            });
        });
    });
}

async function executeRecommendations(_callback) {
    $('.recommendation').each(function () {
        let recommendationId = $(this).attr('data-id');
        let cardsContainer = $(this).find('.cardsContainer');
        executeRecommendation(recommendationId, function (cards) {
            _callback(cardsContainer, cards);
        });
    });
}

async function executeRecommendation(id, _callback) {
    let formData = new FormData();
    formData.append("id", id);
    let response = await fetch("/recommendations/execute", {
        method: "POST",
        body: formData
    });
    let cards = await response.json();
    _callback(cards);
}

async function getCachedRecommendations(_callback) {
    $(".recommendation").each(function () {
        let recommendationId = $(this).attr('data-id');
        let cardsContainer = $(this).find('.cardsContainer');
        getCachedRecommendation(recommendationId, function (cards) {
            _callback(cardsContainer, cards);
            enableDatePicker($(cardsContainer).find('.goalTargetDate'));
        });
    });
}

// get a recommendation from cache, but do not execute it
async function getCachedRecommendation(id, _callback) {
    let formData = new FormData();
    formData.append("id", id);
    let response = await fetch("/recommendations/getCached", {
        method: "POST",
        body: formData
    });
    let cards = await response.json();
    _callback(cards);
}

function renderCards(cards) {
    let html = "";
    cards.forEach(function (card) {
        html += "<div class='card " + card.indicator + "'>\n";
        html += "<table style='width:100%'><tr><td>\n";
        html += "<div class='circle'><span>XX</span></div>\n"
        html += "</td><td>\n";
        html += "<div class='content'>\n";
        html += "<span class='summary heading'>" + card.summary + "</span>\n";

        if (card.rationale !== null) {
            html += "<span class='rationale'>" + card.rationale + "</span>\n";
        }

        if (card.source.label !== null && card.source.url !== null) {
            html += "<span class='source'>";
            html += "<a href='" + card.source.url + "' target='_blank' rel='noopener noreferrer'>" +
                card.source.label + "</a>";
            html += "</span>\n";
        }

        if (card.links !== null) {
            html += "<div class='links'>";
            card.links.forEach(function(link) {
                html += "<a class='link' href='" + link.url + "'>" + link.label + "</a>\n";
            });
            html += "</div>\n";
        }

        html += buildCounselingHTML(card.suggestions);

        html += "</td><td>\n";

        html += buildGoalsHTML(card.suggestions);

        html += buildLinksHTML(card.suggestions);
        // if (card.selectionBehavior !== null) {
        //     html += "<span class='selectionBehavior'>" + card.selectionBehavior + "</span>\n";
        // }

        html += "</div>\n";
        html += "</td></tr></table>\n";
        html += "</div>\n";
    });
    return html;
}

function buildCounselingHTML(suggestions) {
    let html = "";
    if (suggestions !== null) {
        suggestions.forEach(function(s) {
            if (s.type === 'counseling') {
                html += "<div class='counseling' data-id='" + s.id + "' data-reference-system='" + s.references.system + "' data-reference-code='" + s.references.code + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                if (s.actions !== null) {
                    html += "<ul class='actions'>";
                    s.actions.forEach(function(action) {
                        html += "<li class='action'><a href='" + action.url + "'>" + action.label + "</a></li>";
                    });
                    html += "</ul>";
                }
                html += "</div>\n";
            }
        });
    }
    return html !== "" ?
        "<div class='counselingContainer'>" + html + "</div>" :
        "";
}

function buildGoalsHTML(suggestions) {
    let html = "";
    if (suggestions !== null) {
        suggestions.forEach(function(s) {
            if (s.type === 'goal') {
                html += "<div class='goal' data-id='" + s.id + "' data-reference-system='" + s.references.system + "' data-reference-code='" + s.references.code + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tr><td>";

                if (s.actions === null || s.actions.length === 0) {
                    // textbox input
                    html += "<input type='text' class='action' placeholder='Describe your goal here'>";

                } else if (s.actions.length === 1) {
                    // predefined single goal, this is all you get, this is a label
                    html += "<div class='action'>" + s.actions[0].label + "</div>";

                } else {
                    // predefined multiple-choice goal, these are radio buttons
                    let i = 0;
                    let x = randomChars(5);
                    s.actions.forEach(function(action) {
                        html += "<input id='action" + x + "_" + i + "' class='action' type='radio' name='action" + x + "' value='" + action.label + "'>";
                        html += "<label for='action" + x + "_" + i + "'>" + action.label + "</label>\n";
                        i ++;
                    });
                }

                html += "</td><td>";

                html += "<div class='commitToGoalButton'><span>Commit to Goal</span></div></td>\n";
                html += "</td>";
                html += "</tr><tr>";

                let y = randomChars(5);
                html += "<td><label for='goalTargetDate" + y + "'>When do you want to achieve this goal?</label></td>";
                html += "<td><input id='goalTargetDate" + y + "' type='text' class='goalTargetDate' placeholder='--Select Date--' readOnly/></td>";

                html += "</tr></table>";
                html += "</div>\n";

            } else if (s.type === 'update-goal') {
                html += "<div class='goal' data-id='" + s.id + "' data-reference-system='" + s.references.system + "' data-reference-code='" + s.references.code + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tr><td>";

                // todo: complete this

                html += "</td></tr></table>";
                html += "</div>\n";
            }
        });
    }
    return html !== "" ?
        "<div class='goalsContainer'>" + html + "</div>" :
        "";
}

function buildLinksHTML(suggestions) {
    let html = "";
    if (suggestions !== null) {
        suggestions.forEach(function(s) {
            if (s.type === 'link') {
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
                html += "</div>\n";
            }
        });
    }
    return html !== "" ?
        "<div class='linksContainer'>" + html + "</div>" :
        "";
}

function buildGoalData(button) {
    let goal = $(button).closest('.goal');
    let g = {};
    g.extGoalId = $(goal).attr('data-id');
    g.referenceSystem = $(goal).attr('data-reference-system');
    g.referenceCode = $(goal).attr('data-reference-code');
    g.goalText = getGoalText(goal);
    g.targetDate = $(goal).find('.goalTargetDate').datepicker('getDate');
    g.followUpDays = 0;
    return g;
}

function getGoalText(goal) {
    let action = $(goal).find('.action');

    if ($(action).length === 1) {
        if ($(action).is('input[type="text"]')) {
            return $(action).val();

        } else if ($(action).is('div')) {
            return $(action).text();
        }

    } else if ($(action).length > 1) {
        return $(action).filter(":checked").val();
    }

    return null;
}

function buildCounselingData(a) {
    let counseling = $(a).closest('.counseling');
    let c = {};
    c.extCounselingId = $(counseling).attr('data-id');
    c.referenceSystem = $(counseling).attr('data-reference-system');
    c.referenceCode = $(counseling).attr('data-reference-code');
    c.counselingText = a.innerText;
    return c;
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
    formData.append("goalText", g.goalText);
    formData.append("targetDateTS", targetDateTS);
    formData.append("followUpDays", g.followUpDays || 0);

    let response = await fetch("/goals/create", {
        method: "POST",
        body: formData
    });

    let goal = await response.json();
    if (goal) {
        _callback(response.status, goal);
    }
}

function hideGoal(extGoalId) {
    $('.goal[data-id="' + extGoalId + '"]').fadeOut();
}

$(document).ready(function() {
    enableHover('.commitToGoalButton');

    $(document).on('click', '.goalsContainer .goal .commitToGoalButton', function() {
        let g = buildGoalData(this);

        createGoal(g, function(status, goal) {
            if (status === 200) {
                hideGoal(goal.extGoalId);
            }
        });
    });

    $(document).on('click', '.counselingContainer .counseling .actions a', function(event) {
        event.preventDefault();
        let a = $(this);
        let c = buildCounselingData(this);
        registerCounselingReceived(c, function(status) {
            window.location.href = $(a).attr('href');
        });
    });
});