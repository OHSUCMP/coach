function doRefresh(_callback) {
    $.ajax({
        method: "POST",
        url: "/refresh"
    }).done(function(msg, textStatus, jqXHR) {
        _callback(msg);
    });
}