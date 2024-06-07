function doRefresh(_callback) {
    $.ajax({
        method: "POST",
        url: "/refresh"
    }).done(function() {
        _callback();
    });
}