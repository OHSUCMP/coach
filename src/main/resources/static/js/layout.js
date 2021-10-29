async function doRefresh(_callback) {
    let response = await fetch("/refresh", {
        method: "POST",
        headers: {
            "Content-Type": "application/json; charset=utf-8"
        }
    });

    let msg = await response.text();

    _callback(msg);
}