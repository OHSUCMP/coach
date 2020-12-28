function populateRecommendations(_callback) {
    $.ajax({
        "url": "http://localhost:8082/recommendation/list",
        "type": "GET",
        "dataType": "json",
        "contentType": "application/json; charset=utf-8",
//        "data": buildRulerRequest(fhirServer, bearerToken),
        "success": function (data) {
            populateRecommendationsSelect(data);
            _callback();
        }
    });
}

function populateRecommendationsSelect(arr) {
    let options = "<option value='' selected>-- Select Recommendation --</option>\n";
    let info = '';

    arr.forEach(function (item) {
        let trimmedTitle = item.title;
        if (trimmedTitle.indexOf("Recommendation - ") === 0) {
            trimmedTitle = trimmedTitle.substring(17);
        }

        options += "<option value='" + item.id + "'>" + item.title + "</option>\n";
        info += "<div class='recommendation hidden' data-recommendation-id='" + item.id +
            "'>\n<span class='recommendationTitle'>" + item.title +
            "</span>\n<span class='recommendationDesc'>" + item.description +
            "</span></div>\n";
    });

    $('#recommendationsSelect').html(options);
    $('#recommendationInfo').html(info);
}

function recommendationChanged() {
    let button = $('#executeRecommendationButton');
    let recId = $('#recommendationsSelect').children('option:selected').attr('value');
    if (recId === '') {
        $(button).prop('disabled', true);
        $('div.recommendation').not('.hidden').each(function () {
            $(this).addClass('hidden');
        });

    } else {
        $(button).prop('disabled', false);
        $('div.recommendation').each(function () {
            if ($(this).attr('data-recommendation-id') === recId) {
                $(this).removeClass('hidden');
            } else {
                $(this).addClass('hidden');
            }
        });
    }

    $('#cards').html('');
}

async function executeSelectedRecommendation() {
    let recommendationId = $('#recommendationsSelect').children("option:selected").attr("value");
    let button = $('#executeRecommendationButton');
    let body = $('body');

    $(body).css('cursor', 'wait');
    $(button).attr('disabled', true);

    let formData = new FormData();
    formData.append("id", recommendationId);
    let response = await fetch("/recommendation/execute", {
        method: "POST",
        body: formData
    });

    if (response.status === 200) {
        $(button).removeAttr('disabled');

        $(body).css('cursor', 'auto');
        console.log("Success - received response: " + response.body);

        populateCards(response.body);

    } else {
        $(body).html("Error - received response " + response.status + " status");
    }
}

function populateCards(cards) {
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
    $('#cards').html(html);
}