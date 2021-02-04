async function executeRecommendations() {
    $(".recommendation").each(function() {
        let recommendationId = $(this).attr('data-id');
        let cardsContainer = $(this).find('.cardsContainer');
        executeRecommendation(recommendationId, function(cards) {
            $(cardsContainer).html(renderCards(cards));
        });
    });
}

async function executeRecommendation(id, _callback) {
    let formData = new FormData();
    formData.append("id", id);

    let response = await fetch("/recommendation/execute", {
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
        html += "<span class='summary'>" + card.summary + "</span>\n";

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
            html += "<span class='suggestions'>" + card.suggestions + "</span>\n";
        }

        if (card.selectionBehavior !== null) {
            html += "<span class='selectionBehavior'>" + card.selectionBehavior + "</span>\n";
        }

        html += "</div>\n";
    });
    return html;
}