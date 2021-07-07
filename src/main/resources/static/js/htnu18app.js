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

// pulled from https://stackoverflow.com/questions/1349404/generate-random-string-characters-in-javascript
function randomChars(length) {
    let result           = '';
    let characters       = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let charactersLength = characters.length;
    for ( let i = 0; i < length; i++ ) {
        result += characters.charAt(Math.floor(Math.random() * charactersLength));
    }
    return result;
}

function toLabel(string) {
    let words = string.replaceAll("_", " ").toLowerCase().split(" ");
    let label = words.map(word => {
        return word[0].toUpperCase() + word.substring(1);
    }).join(" ");
    return label;
}