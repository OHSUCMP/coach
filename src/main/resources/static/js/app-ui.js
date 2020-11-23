function hoverOn(el) {
    $(el).addClass('hover');
}

function hoverOff(el) {
    $(el).removeClass('hover');
}

function enableHover(selector) {
    $(document).on({
        mouseover: function () {
            hoverOn($(this));
        },
        mouseleave: function () {
            hoverOff($(this));
        }
    }, selector);
}

function markTabSelected(tab) {
    $('#tabs > .tab').each(function () {
        if (this === tab && !$(tab).hasClass('selected')) {
            $(this).addClass('selected');

        } else if (this !== tab && $(this).hasClass('selected')) {
            $(this).removeClass('selected');
        }
    });
}

function getCurrentTabID() {
    return $('#tabData > div:not(:hidden)').first().attr('id');
}

function renderSelectedTabDiv() {
    var tab = $('#tabs > .tab.selected').get(0);
    var divID = $(tab).attr('data-assocDivID');
    $('#tabData > div').each(function () {
        var id = $(this).attr('id');
        if (id === divID && $(this).hasClass('hidden')) {
            $(this).removeClass('hidden');

        } else if (id !== divID && !$(this).hasClass('hidden')) {
            $(this).addClass('hidden');
        }
    });
}
