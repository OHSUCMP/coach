function enableDatePicker(sel) {
//    let currentYear = new Date().getFullYear();

    let minDate = new Date();
    if (minDate.getMonth() === 0) { // January
        minDate.setMonth(11);
        minDate.setFullYear(minDate.getFullYear() - 1);
    } else {
        minDate.setMonth(minDate.getMonth() - 1);
    }
    minDate.setDate(1);

    $(sel).datepicker({
        changeMonth: true,
        changeYear: true,
        showButtonPanel: true,
        showOn: 'focus', //'button',
        constrainInput: true,
        dateFormat: 'mm-dd-yy',
        minDate: minDate,
        maxDate: 0,
        gotoCurrent: true //,
//        yearRange: (currentYear - 1) + ':' + currentYear
    });
}

async function createBPReading(systolic1, diastolic1, pulse1, systolic2, diastolic2, pulse2, readingDate, confirm, _callback) {
    let readingDateTS = $.datepicker.formatDate('@', readingDate);

    let formData = new FormData();
    formData.append("systolic1", systolic1);
    formData.append("diastolic1", diastolic1);
    formData.append("pulse1", pulse1);
    formData.append("systolic2", systolic2);
    formData.append("diastolic2", diastolic2);
    formData.append("pulse2", pulse2);
    formData.append("readingDateTS", readingDateTS);
    formData.append("confirm", confirm);

    let response = await fetch("/bp-readings/create", {
        method: "POST",
        body: formData
    });

    let bpreadings = await response.json();

    _callback(bpreadings);
}

function appendBPReadingToTable(obj) {
    let container = $('#bpreadingsTable');
    let unsortedData = $(container).find('tr');

    // note : keep this section synced with bp-readings.mustache

    let html = "<tr class='data' data-id='" + obj.id + "' data-timestamp='" + obj.readingDateTimestamp + "'>" +
        "<td>" + obj.readingDateString + "</td>" +
        "<td>" + obj.systolic + "</td>" +
        "<td>" + obj.diastolic + "</td>" +
        "<td>" + obj.pulse + "</td>" +
        "<td><span class=\"link\" onclick=\"deleteBPReading(" + obj.id + ")\">Delete</span></td>" +
        "</tr>\n";

    // now sort
    // adapted from https://stackoverflow.com/questions/6133723/sort-divs-in-jquery-based-on-attribute-data-sort

    let sortedData = $(unsortedData).add(html).sort(function(a,b) {
        let tsA = $(a).data('timestamp');
        let tsB = $(b).data('timestamp');
        return (tsA < tsB) ? 1 : (tsA > tsB) ? -1 : 0;
    });

    $(container).html(sortedData);
}

async function deleteBPReading(id) {
    let formData = new FormData();
    formData.append("id", id);

    let response = await fetch("/bp-readings/delete", {
        method: "POST",
        body: formData
    });

    if (response.status === 200) {
        $("#bpreadingsTable tr.data[data-id='" + id + "']").remove();

    } else {
        let msg = await response.text();
        alert(msg);
    }
}
