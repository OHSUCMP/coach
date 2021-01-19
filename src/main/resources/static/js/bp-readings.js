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

function populateReadingTimesList(el) {
    let min_arr = ['00', '15', '30', '45'];

    let html = "<option value='' disabled selected>-- Select Time --</option>\n";
    for (let hour = 0; hour <= 23; hour ++) {
        let display_hour = hour <= 12 ? hour : hour - 12;
        if (display_hour === 0) display_hour = 12;
        let ampm = hour < 12 ? "AM" : "PM";

        $(min_arr).each(function() {
            let min = this;
            let time = hour + ':' + min;
            let display_time = display_hour + ':' + min + ' ' + ampm;
            html += "<option value='" + time + "'>" + display_time + "</option>\n";
        });
    }

    $(el).html(html);
}

function populateSelect(el, label, min, max) {
    let html = "<option value='' disabled selected>" + label + "</option>\n";

    for (let i = min; i <= max; i ++) {
        html += "<option value='" + i + "'>" + i + "</option>\n";
    }

    $(el).html(html);
}

async function createRecord(systolic, diastolic, timestamp, _callback) {
    let timestamp_ms = $.datepicker.formatDate('@', timestamp);

    let formData = new FormData();
    formData.append("systolic", systolic);
    formData.append("diastolic", diastolic);
    formData.append("timestamp", timestamp_ms);

    let response = await fetch("/bp-readings/create", {
        method: "POST",
        body: formData
    });

    let bpreading = await response.json();

    _callback(bpreading);
}

function appendRecordToTable(obj) {
    let container = $('#bpreadingsTable');
    let unsortedData = $(container).find('tr');

    let html = "<tr class='data' data-timestamp='" + obj.readingDateTimestamp + "'>" +
        "<td>" + obj.readingDateString + "</td>" +
        "<td>" + obj.systolic + "</td>" +
        "<td>" + obj.diastolic + "</td>" +
        "<td><span class=\"link\" onclick=\"deleteReading(" + obj.id + ")\">Delete</span></td>" +
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

function deleteRecord(id) {
    alert("deleting reading with id=" + id);
}
