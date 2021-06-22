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

        html += "</td><td>\n";

        if (card.suggestions !== null) {
            html += "<div class='suggestions'>";
            let i = 0;
            card.suggestions.forEach(function(suggestion) {
                html += "<div class='suggestion' data-id='" + suggestion.id + "' data-category='" + suggestion.category + "' data-type='" + suggestion.type + "'>";
                html += "<span class='heading'>" + suggestion.label + "</span>";
                html += "<table><tr><td>";
                if (suggestion.actions !== null) {
                    html += "<ul class='actions'>";
                    suggestion.actions.forEach(function(action) {
                        html += "<li class='action'>" + action + "</li>";
                    });
                    html += "</ul>";
                }
                html += "</td>";
                if (suggestion.type === 'goal') {
                    html += "<td><span class='commitToGoalButton' data-extGoalId='" + suggestion.id + "'>Commit to Goal</span></td>\n";
                    html += "</tr><tr>";
                    html += "<td><label for='goalTargetDate" + i + "'>When do you want to achieve this goal?</label></td>";
                    html += "<td><input id='goalTargetDate" + i + "' type='text' class='goalTargetDate' placeholder='--Select Date--' readOnly/></td>";
                }
                html += "</tr></table>";
                html += "</div>\n";
            });
            html += "</div>";
        }

        // if (card.selectionBehavior !== null) {
        //     html += "<span class='selectionBehavior'>" + card.selectionBehavior + "</span>\n";
        // }

        html += "</div>\n";
        html += "</td></tr></table>\n";
        html += "</div>\n";
    });
    return html;
}

function buildGoalData(suggestion) {
    if ($(suggestion).attr('data-type') === 'goal') {
        let g = {};
        g.extGoalId = $(suggestion).attr('data-id');
        g.category = $(suggestion).attr('data-category');
        g.goalText = getGoalText(suggestion);
        g.followUpDays = 0;
        return g;

    } else {
        return null;
    }
}

function getGoalText(suggestion) {
    return ''; // todo : get the selected / entered goal text
}

function buildCounselingData(a) {
    let suggestion = $(a).closest('.suggestion');
    let c = {};
    c.extCounselingId = $(suggestion).attr('data-id');
    c.category = $(suggestion).attr('data-category');
    c.counselingText = a.innerText;
    return c;
}

async function registerCounselingReceived(c, _callback) {
    let formData = new FormData();
    formData.append("extCounselingId", c.extCounselingId);
    formData.append("category", c.category);
    formData.append("counselingText", c.counselingText);

    const response = await fetch("/counseling/create", {
        method: "POST",
        body: formData
    });

    await response.text();

    _callback(response.status);
}

$(document).ready(function() {
    $(document).on('click', '#recommendationsContainer .suggestion[data-type="goal"] .commitToGoalButton', function() {
        let g = buildGoalData($(this).closest('.suggestion'));

        if ($(this).is(':checked')) {
            createGoal(g.extGoalId, g.category, g.goalText, g.followUpDays, function(goal) {
                alert("created goal: " + goal.extGoalId);
            });

        } else {
            deleteGoal(g.extGoalId, function(deletedExtGoalId) {
                alert("deleted goal: " + deletedExtGoalId);
            });
        }
    });

    $(document).on('click', '#recommendationsContainer .suggestion[data-type="counseling"] .actions a', function(event) {
        event.preventDefault();
        let a = $(this);
        let c = buildCounselingData(this);
        registerCounselingReceived(c, function(status) {
            window.location.href = $(a).attr('href');
        });
    });
});