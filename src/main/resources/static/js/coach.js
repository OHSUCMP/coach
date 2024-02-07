function loadScript(src, callback) {
    // NOTE: this function uses a callback function to perform subsequent tasks that require the
    //       specified script to be loaded.  this is because loading a script is an asynchronous
    //       task; the call to load returns before loading is complete.  thus, race conditions
    //       are likely to occur.  placing dependent logic in callbacks negates this issue.

    // pulled from https://aaronsmith.online/easily-load-an-external-script-using-javascript/
    let script = document.createElement('script')
    script.type = 'text/javascript'

    if (script.readyState) { // IE
        script.onreadystatechange = function () {
            if (script.readyState === 'loaded' || script.readyState === 'complete') {
                script.onreadystatechange = null;
                callback();
            }
        }
    } else { // Others
        script.onload = callback;
    }

    script.src = src;
    document.head.appendChild(script);
}

// function isInternetExplorer() {
//     // adapted from https://www.scratchcode.io/how-to-detect-ie-browser-in-javascript/
//     let isIE = /*@cc_on!@*/false || !!document.documentMode;
//     return isIE || navigator.userAgent.indexOf("MSIE") > 0;
// }

// function loadScript(document, url) {
//     console.log("loading script " + url);
//     // taken from https://stackoverflow.com/questions/3590779/using-javascript-to-load-other-external-javascripts
//     let script = document.createElement('script');
//     script.type = 'text/javascript';
//     script.src = url;
//     document.body.appendChild(script);
// }

function loadStylesheet(url) {
    console.log("loading stylesheet " + url);
    let stylesheet = document.createElement('link');
    stylesheet.rel = 'stylesheet';
    stylesheet.href = url;
    document.body.appendChild(stylesheet);
}

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

// todo : this needs to preserve leading zeros!
// yyyy-MM-ddThh:mm:ss.+zz:zz
function toDateString(o) {
    let d = new Date(Date.parse(o));
    return (d.getMonth() + 1) + "/" + d.getDate() + "/" + d.getFullYear();
}

// todo : this needs to preserve leading zeros!
// yyyy-MM-ddThh:mm:ss.+zz:zz
function toDateTimeString(o) {
    let d = new Date(Date.parse(o));
    return (d.getMonth() + 1) + "/" + d.getDate() + "/" + d.getFullYear() + " " +
        d.getHours() + ":" + pad(d.getMinutes(), '0', 2) + ":" + pad(d.getSeconds(), '0', 2);
}

function pad(o, padChar, fillToLen) {
    let s = o.toString();
    if (s.length >= fillToLen) {
        return s;
    } else {
        let paddedPart = '';
        for (let i = 0; i < fillToLen - s.length; i ++) {
            paddedPart = paddedPart + padChar;
        }
        return paddedPart + s;
    }
    // return s.length >= fillToLen ?
    //     s :
    //     padChar.repeat(fillToLen - s.length) + s;
}

$(document).ready(function() {
    enableHover('.link');
    enableHover('.anchor');
});

$(document).on('click', '.anchor:not(.selected)', function() {
    window.location.href = $(this).attr('data-href');
});

$(document).on('click', 'a', function() {
    let href = $(this).attr('href').toLowerCase();
    if (href.startsWith('http://') || href.startsWith('https://')) {
        $(this).attr('target', '_blank');
    }
    return true;
});