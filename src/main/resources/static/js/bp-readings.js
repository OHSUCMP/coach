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

function buildBPReadingData() {
    let data = {};
    data.systolic1 = $('#systolic1').val();
    data.diastolic1 = $('#diastolic1').val();
    let pulse1 = $('#pulse1').val();
    if (pulse1 !== '') data.pulse1 = pulse1;
    let systolic2 = $('#systolic2').val();
    if (systolic2 !== '') data.systolic2 = systolic2;
    let diastolic2 = $('#diastolic2').val();
    if (diastolic2 !== '') data.diastolic2 = diastolic2;
    let pulse2 = $('#pulse2').val();
    if (pulse2 !== '') data.pulse2 = pulse2;

    let readingDate = $('#readingDate').datepicker('getDate');
    let timeArr = $('#readingTime').val().match(/(\d+):(\d+)\s+(am|pm)/);
    let h = parseInt(timeArr[1]);
    let m = parseInt(timeArr[2]);
    let ampm = timeArr[3];
    if      (h === 12 && ampm === 'am') h = 0;
    else if (h  <  12 && ampm === 'pm') h += 12;
    readingDate.setHours(h);
    readingDate.setMinutes(m);

    data.readingDate = readingDate;

    data.followedInstructions = $('input[type=radio][name=confirm]:checked').val() === 'yes';

    return data;
}

async function createBPReading(data, _callback) {
    let readingDateTS = $.datepicker.formatDate('@', data.readingDate);

    let formData = new FormData();
    formData.append("systolic1", data.systolic1);
    formData.append("diastolic1", data.diastolic1);
    if (data.pulse1)        formData.append("pulse1", data.pulse1);
    if (data.systolic2)     formData.append("systolic2", data.systolic2);
    if (data.diastolic2)    formData.append("diastolic2", data.diastolic2);
    if (data.pulse2)        formData.append("pulse2", data.pulse2);
    formData.append("readingDateTS", readingDateTS);
    formData.append("followedInstructions", data.followedInstructions);

    let response = await fetch("/bp-readings/create", {
        method: "POST",
        body: formData
    });

    let status = response.status;
    let bpreadings = await response.json();

    _callback(status, bpreadings);
}

function appendBPReadingToTable(obj) {
    let container = $('#bpreadingsTable');
    let unsortedData = $(container).find('tr');

    // note : keep this section synced with bp-readings.mustache

    // todo : obj was HomeBloodPressureReading, ***IS NOW*** BloodPressureModel

    let html = "<tr class='data' data-id='" + obj.id + "' data-timestamp='" + obj.readingDateTimestamp + "'>" +
        "<td>" + obj.readingDateString + "</td>" +
        "<td>" + obj.systolic + "</td>" +
        "<td>" + obj.diastolic + "</td>" +
        "<td>" + (obj.pulse === null ? '' : obj.pulse) + "</td>" +
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
