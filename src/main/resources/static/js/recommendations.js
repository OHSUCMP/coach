async function executeRecommendations() {
    $("td.cards").each(function() {
        let container = this;
        let recommendationId = $(container).attr('data-recommendation-id');
        executeRecommendation(recommendationId, function(cards) {
            $(container).html(renderCards(cards));
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
        html += "<div class='card " + card.indicator +
            "'>\n<span class='cardTitle'>" + card.summary +
            "</span>\n<span class='cardDetail'>" + card.detail +
            "</span>\n";

        if (card.source.label !== undefined && card.source.url !== undefined) {
            html += "See: <a href='" + card.source.url + "' target='_blank' rel='noopener noreferrer'>" +
                card.source.label + "</a>\n";
        }

        html += "</div>\n";
    });
    return html;
}