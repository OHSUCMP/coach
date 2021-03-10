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

function resetForm(form) {
    $(form).find('input[type=number]').each(function() {
        $(this).val('');
    });

    $(form).find('input[type=text]').each(function() {
        $(this).val('');
    });

    $(form).find('input[type=radio]').each(function() {
        $(this).prop('checked', false);
    })

    $(form).find('select').each(function() {
        $(this).prop('selectedIndex', 0);
    })
}

function validateForm(form, outputContainer) {
    let pass = true;
    $(form).find('.field').each(function() {
        if ( ! validateField(this) ) {
            pass = false;
        }
    });

    let el = $(outputContainer);
    $(el).removeClass();
    if (pass) {
        $(el).addClass('hidden');
        $(el).html('');

    } else {
        $(el).addClass('error');
        $(el).html("Please fill missing fields and try again.");
    }

    return pass;
}

function validateField(field) {
    let pass = true;

    $(field).find('input[type=number]').each(function() {
        if ($(this).val().trim() === '') {
            pass = false;
        }
    });

    $(field).find('input[type=text]').each(function() {
        if ($(this).val().trim() === '') {
            pass = false;
        }
    });

    $(field).find('select').each(function() {
        if ($(this).prop('selectedIndex') === 0) {
            pass = false;
        }
    });

    let radio = $(field).find('input[type=radio]');
    if (radio.length > 0 && $(radio).filter(':checked').length === 0) {
        pass = false;
    }

    if (pass) {
        $(field).removeClass('error');
    } else {
        $(field).addClass('error');
    }

    return pass;
}

async function createRecord(systolic1, diastolic1, pulse1, systolic2, diastolic2, pulse2, readingDate, confirm, _callback) {
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

    let bpreading = await response.json();

    _callback(bpreading);
}

function appendRecordToTable(obj) {
    let container = $('#bpreadingsTable');
    let unsortedData = $(container).find('tr');

    // note : keep this section synced with bp-readings.mustache

    let html = "<tr class='data' data-id='" + obj.id + "' data-timestamp='" + obj.readingDateTimestamp + "'>" +
        "<td>" + obj.readingDateString + "</td>" +
        "<td>" + obj.systolic1 + "</td>" +
        "<td>" + obj.diastolic1 + "</td>" +
        "<td>" + obj.pulse1 + "</td>" +
        "<td>" + obj.systolic2 + "</td>" +
        "<td>" + obj.diastolic2 + "</td>" +
        "<td>" + obj.pulse2 + "</td>" +
        "<td><span class=\"link\" onclick=\"deleteRecord(" + obj.id + ")\">Delete</span></td>" +
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

async function deleteRecord(id) {
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
