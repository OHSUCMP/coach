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

        if (card.detail !== undefined) {
            html += "<span class='details'>" + card.detail + "</span>\n";
        }

        if (card.source.label !== undefined && card.source.url !== undefined) {
            html += "<span class='source'>";
            html += "See: <a href='" + card.source.url + "' target='_blank' rel='noopener noreferrer'>" +
                card.source.label + "</a>";
            html += "</span>\n";
        }

        html += "</div>\n";
    });
    return html;
}