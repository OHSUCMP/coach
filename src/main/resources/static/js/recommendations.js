async function executeRecommendations() {
    let goals = await getRecordedGoals();

    $(".recommendation").each(function () {
        let recommendationId = $(this).attr('data-id');
        let cardsContainer = $(this).find('.cardsContainer');
        executeRecommendation(recommendationId, function (cards) {
            $(cardsContainer).html(renderCards(cards));

            goals.forEach(function(goal) {
                $(cardsContainer).find('input.goal[data-goalid="' + goal.goalId + '"').each(function () {
                    if ($(this).attr('type') === 'checkbox') {
                        $(this).prop('checked', true);
                    }
                });
            });
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

function renderCards(cards) {
    let html = "";
    cards.forEach(function (card) {
        html += "<div class='card " + card.indicator + "'>\n";
        html += "<div class='circle'><span>XX</span></div>\n"
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

        if (card.suggestions !== null) {
            html += "<div class='suggestions'>";
            card.suggestions.forEach(function(suggestion) {
                html += "<div class='suggestion'>";
                html += "<span class='heading'>Suggestion: " + suggestion.label + "</span>";
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

        if (card.links !== null) {
            html += "<div class='links'>";
            card.links.forEach(function(link) {
                html += "<a class='link' href='" + link.url + "'>" + link.label + "</a>\n";
            });
            html += "</div>\n";
        }

        // if (card.selectionBehavior !== null) {
        //     html += "<span class='selectionBehavior'>" + card.selectionBehavior + "</span>\n";
        // }

        html += "</div></div>\n";
    });
    return html;
}


$(document).ready(function() {
    $('#recommendationsContainer').on('change', 'input.goal[type="checkbox"]', function() {
//         let recommendationId = $(this).closest('.recommendation').attr('data-id');
        let goalId = $(this).attr('data-goalid');
        let goalText = $(this).next('label').text();
        let followUpDays = $(this).attr('data-followup-days');

        if ($(this).is(':checked')) {
            createGoal(goalId, goalText, followUpDays, function(goal) {
                alert("created goal: " + goal.goalId);
            });

        } else {
            deleteGoal(goalId, function(deletedGoalId) {
                alert("deleted goal: " + deletedGoalId);
            });
        }
    });
});