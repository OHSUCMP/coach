async function executeRecommendations() {
    let goals = await getRecordedGoals();

    $(".recommendation").each(function () {
        let recommendationId = $(this).attr('data-id');
        let cardsContainer = $(this).find('.cardsContainer');
        executeRecommendation(recommendationId, function (cards) {
            $(cardsContainer).html(renderCards(cards));

            goals.forEach(function(goal) {
                $(cardsContainer).find('input.goal[data-id="' + goal.goalId + '"').each(function () {
                    if ($(this).attr('type') === 'checkbox') {
                        $(this).prop('checked', goal.value === 'true');
                    }
                });
            });
        });
    });
}

async function getRecordedGoals() {
    let response = await fetch("/goals", {
        method: "GET"
    });

    let goals = await response.json();

    return goals;
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

function renderCards(cards) {
    let html = "";
    cards.forEach(function (card) {
        html += "<div class='card " + card.indicator + "'>\n";
        html += "<span class='summary heading'>" + card.summary + "</span>\n";

        if (card.rationale !== null) {
            html += "<span class='rationale'>" + card.rationale + "</span>\n";
        }

        if (card.source.label !== null && card.source.url !== null) {
            html += "<span class='source'>";
            html += "See: <a href='" + card.source.url + "' target='_blank' rel='noopener noreferrer'>" +
                card.source.label + "</a>";
            html += "</span>\n";
        }

        if (card.source2 !== null) {
            // label is just the tail path part of the URL
            let label = card.source2.substring(card.source2.lastIndexOf('/') + 1);

            html += "<span class='source'>";
            html += "See: <a href='" + card.source2 + "' target='_blank' rel='noopener noreferrer'>" +
                label + "</a>";
            html += "</span>\n";
        }

        if (card.suggestions !== null) {
            html += "<div class='suggestions'>";
            card.suggestions.forEach(function(suggestion) {
                html += "<div class='suggestion'>";
                html += "<span class='heading'>" + suggestion.label + "</span>";
                if (suggestion.actions !== null) {
                    html += "<ul class='actions'>";
                    suggestion.actions.forEach(function(action) {
                        html += "<li class='action'>" + action + "</li>";
                    });
                    html += "</ul>";
                }
                html += "</div>\n";
            });
            html += "</div>\n";
        }

        if (card.selectionBehavior !== null) {
            html += "<span class='selectionBehavior'>" + card.selectionBehavior + "</span>\n";
        }

        html += "</div>\n";
    });
    return html;
}

async function setGoal(goalId, followUpDays, value) {
    let formData = new FormData();
    formData.append("goalId", goalId);
    formData.append("followUpDays", followUpDays || 0);
    formData.append("value", value);

    let response = await fetch("/goals/put", {
        method: "PUT",
        body: formData
    });

    let obj = await response.json();

//    alert('toggled "' + goalId + '" - new value = "' + value + '" with followUpDays = "' + followUpDays + '".  obj="' + obj + '"');
}

$(document).ready(function() {
    $('#recommendationsContainer').on('change', 'input.goal[type="checkbox"]', function() {
//         let recommendationId = $(this).closest('.recommendation').attr('data-id');
        let goalId = $(this).attr('data-id');
        let followUpDays = $(this).attr('data-followup-days');
        let value = $(this).is(':checked');
        setGoal(goalId, followUpDays, value);
    });
});