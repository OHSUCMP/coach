function enableHover(selector) {
    $(document).on({
        mouseover: function () {
            $(this).addClass('hover')
        },
        mouseleave: function () {
            $(this).removeClass('hover');
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

// pulled from https://stackoverflow.com/questions/31593297/using-execcommand-javascript-to-copy-hidden-text-to-clipboard
function setClipboard(value) {
    let tempInput = document.createElement("input");
    tempInput.style = "position: absolute; left: -1000px; top: -1000px";
    tempInput.value = value;
    document.body.appendChild(tempInput);
    tempInput.select();
    document.execCommand("copy");
    document.body.removeChild(tempInput);
}