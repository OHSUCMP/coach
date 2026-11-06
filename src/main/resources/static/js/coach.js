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

// escape a value for safe inclusion in HTML element content or a quoted attribute value
function escapeHtml(o) {
    return String(o)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// escape only quote characters, for values placed in quoted attributes that may legitimately contain
// HTML entities (e.g. recommendation card content).  prevents breaking out of the attribute while
// preserving existing entity decoding behavior
function escapeQuotes(o) {
    return String(o)
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// decode HTML entities (e.g. "&amp;" -> "&") the same way the browser would in an attribute value
function decodeEntities(s) {
    let t = document.createElement('textarea');
    t.innerHTML = s;
    return t.value;
}

// return an attribute-safe URL, or '#' if the URL uses a disallowed scheme (e.g. javascript:, data:)
function safeUrl(url) {
    try {
        let decoded = decodeEntities(String(url));   // check the URL the browser will actually see
        let protocol = new URL(decoded, window.location.href).protocol;
        if (protocol === 'http:' || protocol === 'https:' || protocol === 'mailto:' || protocol === 'tel:') {
            return escapeHtml(decoded);
        }
    } catch (e) {
        // fall through - unparseable URL
    }
    return '#';
}

$(document).ready(function() {
    enableHover('.link');
});

$(document).on('click', 'a', function() {
    let href = $(this).attr('href').toLowerCase();
    if (href.startsWith('http://') || href.startsWith('https://')) {
        $(this).attr('target', '_blank');
    }
    return true;
});