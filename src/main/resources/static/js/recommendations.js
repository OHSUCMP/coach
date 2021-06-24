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
        html += "<table width='100%'><tr><td>\n";
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

        if (card.source2 !== null) {
            // label is just the tail path part of the URL
            let label = card.source2.substring(card.source2.lastIndexOf('/') + 1);

            html += "<span class='source'>";
            html += "<a href='" + card.source2 + "' target='_blank' rel='noopener noreferrer'>" +
                label + "</a>";
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
        let i = 0;
        suggestions.forEach(function(s) {
            if (s.type === 'goal') {
                html += "<div class='goal' data-id='" + s.id + "' data-reference-system='" + s.references.system + "' data-reference-code=" + s.references.code + "'>";
                html += "<span class='heading'>" + s.label + "</span>";
                html += "<table><tr><td>";
                if (s.actions !== null) {

                    // todo : construct HTML inputs as required

                    html += "<ul class='actions'>";
                    s.actions.forEach(function(action) {
                        html += "<li class='action'>" + action.label + "</li>";
                    });
                    html += "</ul>";
                }
                html += "</td><td>";
                html += "<span class='commitToGoalButton' data-extGoalId='" + s.id + "'>Commit to Goal</span></td>\n";
                html += "</td>";
                html += "</tr><tr>";

                let id = randomChars(5);
                html += "<td><label for='goalTargetDate" + id + "'>When do you want to achieve this goal?</label></td>";
                html += "<td><input id='goalTargetDate" + id + "' type='text' class='goalTargetDate' placeholder='--Select Date--' readOnly/></td>";

                html += "</tr></table>";
                html += "</div>\n";
            }
        });
    }
    return html !== "" ?
        "<div class='goalsContainer'>" + html + "</div>" :
        "";
}

function buildGoalData(button) {
    let goal = $(button).closest('.goal');
    let g = {};
    g.extGoalId = $(goal).attr('data-id');
    g.referenceSystem = $(goal).attr('data-reference-system');
    g.referenceCode = $(goal).attr('data-reference-code');
    g.goalText = getGoalText(goal);
    g.followUpDays = 0;
    return g;
}

function getGoalText(suggestion) {
    return ''; // todo : get the selected / entered goal text
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

$(document).ready(function() {
    $(document).on('click', '.goalsContainer .goal .commitToGoalButton', function() {
        let g = buildGoalData(this);

        createGoal(g, function(goal) {
            alert("created goal: " + goal.extGoalId);
        });
        //
        // } else {
        //     deleteGoal(g.extGoalId, function(deletedExtGoalId) {
        //         alert("deleted goal: " + deletedExtGoalId);
        //     });
        // }
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