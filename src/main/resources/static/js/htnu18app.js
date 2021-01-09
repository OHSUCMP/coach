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
