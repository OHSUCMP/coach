function enableDatePicker(sel) {
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

function buildVitalsData() {
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

    data.readingDateTS = $.datepicker.formatDate('@', readingDate);

    data.followedInstructions = $('input[type=radio][name=confirm]:checked').val() === 'yes';

    return data;
}

function containsHighReading(vitalsData) {
    let isHigh = false;
    if ($.isNumeric(vitalsData.systolic2) && $.isNumeric(vitalsData.diastolic2)) {
        isHigh = vitalsData.systolic2 >= 180 || vitalsData.diastolic2 >= 120;
    } else if ($.isNumeric(vitalsData.systolic1) && $.isNumeric(vitalsData.diastolic1)) {
        isHigh = vitalsData.systolic1 >= 180 || vitalsData.diastolic1 >= 120;
    }
    return isHigh;
}

function containsLowReading(vitalsData) {
    let isLow = false;
    if ($.isNumeric(vitalsData.systolic2) && $.isNumeric(vitalsData.diastolic2)) {
        isLow = vitalsData.systolic2 < 90 || vitalsData.diastolic2 < 60;
    } else if ($.isNumeric(vitalsData.systolic1) && $.isNumeric(vitalsData.diastolic1)) {
        isLow = vitalsData.systolic1 < 90 || vitalsData.diastolic1 < 60;
    }
    return isLow;
}

function createVitals(vitalsData, _callback) {
    $.ajax({
        method: "POST",
        url: "/vitals/create",
        data: vitalsData
    }).done(function(vitals, textStatus, jqXHR) {
        _callback(jqXHR.status, vitals);
    });
}

function appendReadingToTable(obj) {
    let container = $('#vitalsTable tbody');
    let unsortedData = $(container).find('tr');

    // note : keep this section synced with vitals.mustache

    // todo : obj was HomeBloodPressureReading, ***IS NOW*** AbstractVitalsModel

    let html = "<tr class='data' data-timestamp='" + obj.readingDateTimestamp + "'>" +
        "<td>" + obj.readingDateString + "</td>" +
        "<td>" + obj.readingType + "</td>" +
        "<td>" + obj.value + "</td>" +
        "</tr>\n";

    // now sort
    // adapted from https://stackoverflow.com/questions/6133723/sort-divs-in-jquery-based-on-attribute-data-sort

    let sortedData = $(unsortedData).add(html).sort(function(a,b) {
        let tsA = $(a).data('timestamp');
        let tsB = $(b).data('timestamp');
        return (tsA > tsB) ? 1 : (tsA < tsB) ? -1 : 0;
    });

    $(container).html(sortedData);
}

function populateReadingTimestampNow() {
    let now = new Date($.now());

    // readingDate : mm-dd-yyyy
    let month = now.getMonth() + 1;
    let dayOfMonth = now.getDate();
    let year = now.getFullYear();

    // readingTime : hh:mm am
    let hour = now.getHours(); // 0 - 23.  0 - 11: am.  12-23: pm.
    let ampm = 'am';
    if (hour >= 12) {
        hour = hour - 12;
        ampm = 'pm';
    }
    if (hour === 0) {
        hour = 12;
    }
    let min = now.getMinutes();;

    let readingDate = String(month).padStart(2, '0') + '-' + String(dayOfMonth).padStart(2, '0') + '-' + year;
    let readingTime = String(hour).padStart(2, '0') + ':' + String(min).padStart(2, '0') + ' ' + ampm;

//    alert("now = '" + now + '; readingDate = "' + readingDate + '", readingTime = "' + readingTime + '"');

    $('#readingDate').val(readingDate);
    $('#readingTime').val(readingTime);
}