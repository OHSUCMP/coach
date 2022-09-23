function doClearSupplementalData(_callback) {
    $.ajax({
        method: "POST",
        url: "/clear-supplemental-data"
    }).done(function(msg, textStatus, jqXHR) {
        _callback(msg);
    });
}
function loadBloodPressureObservations(_callback) {
    $.ajax({
        method: "POST",
        url: "/blood-pressure-observations-list"
    }).done(function(bpdata, textStatus, jqXHR) {
        if (jqXHR.status === 200) {
            bpdata.forEach(function(item) {
                item.readingDate = new Date(item.readingDate);
            });

            bpdata.sort(function (a, b) {
                return a.readingDate - b.readingDate;
            });

            _callback(bpdata);
        }
    });
}

function loadMedications(_callback) {
    $.ajax({
        method: "POST",
        url: "/medications-list"
    }).done(function(meds, textStatus, jqXHR) {
        if (jqXHR.status === 200) {
            _callback(meds);
        }
    });
}

function populateMedications() {
    let data = window.meds;

    if (Array.isArray(data) && data.length > 0) {
        let arr = [];
        data.forEach(function (m) {
            arr.push('<span class="medication" data-system="' + m.system + '" data-code="' + m.code + '">' +
                m.description +
                '</span>');
        });

        let el = $('#currentMedications');
        let html = 'Your Active Antihypertensive Medications: ' + arr.join(', ');

        $(el).html(html);
    }
}

function loadAdverseEvents(_callback) {
    $.ajax({
        method: "POST",
        url: "/adverse-events-list"
    }).done(function(adverseEvents, textStatus, jqXHR) {
        if (jqXHR.status === 200) {
            _callback(adverseEvents);
        }
    });
}

function populateAdverseEvents() {
    let data = window.adverseEvents;

    if (Array.isArray(data) && data.length > 0) {
        let arr = [];
        data.forEach(function (m) {
            arr.push('<span class="adverseEvent" data-system="' + m.system + '" data-code="' + m.code + '">' +
                m.description +
                '</span>');
        });

        let html = 'Your Adverse Events: ' + arr.join(', ');

        $('#adverseEvents').html(html);
    }
}

function populateSummaryDiv() {
    let data = window.bpchart.data;
    let totalSystolic = 0;
    let totalDiastolic = 0;
    let avgSystolic = 0;
    let avgDiastolic = 0;

    let hasData = Array.isArray(data) && data.length > 0;

    if (hasData) {
        let systolicCount = 0;
        let diastolicCount = 0;

        data.forEach(function (o) {
            if (o.systolic) {
                totalSystolic += o.systolic.value;
                systolicCount += 1;
            }
            if (o.diastolic) {
                totalDiastolic += o.diastolic.value;
                diastolicCount += 1;
            }
        });

        avgSystolic = Math.round(totalSystolic / systolicCount);
        avgDiastolic = Math.round(totalDiastolic / diastolicCount);
    }
    $('#avgSystolic').html(avgSystolic);
    $('#avgDiastolic').html(avgDiastolic);

    // build the BP icon and other Hypertension classification stuff based on avgSystolic and avgDiastolic above
    let indicator = 'info';

    if (avgSystolic > 180 || avgDiastolic > 120) { // crisis
        indicator = 'critical';

    } else if (avgSystolic >= 140 || avgDiastolic >= 90) { // stage 2
        indicator = 'critical';

    } else if ((avgSystolic >= 130 && avgSystolic < 140) || (avgDiastolic >= 80 && avgDiastolic < 90)) { // stage 1
        indicator = 'warning';

    } else if (avgSystolic >= 120 && avgSystolic < 130 && avgDiastolic < 80) { // elevated
        indicator = 'warning';

    } else if (avgSystolic < 120 && avgDiastolic < 80) { // normal
        indicator = 'info';
    }

    let el = $('#bpIcon');
    $(el).html("<img src='/images/" + indicator + "-icon.png' class='icon' alt='" + indicator + "' />");
    $('#bpLabel').html('Average BP:');

    let el2 = $('#bpGoalMetLabel');
    if (hasData) {
        let currentBPGoal = getCurrentBPGoal();
        if (avgSystolic > currentBPGoal.systolic || avgDiastolic > currentBPGoal.diastolic) {
            $(el2).html('You are above goal');
        } else {
            $(el2).html('You are at goal');
        }
    } else {
        $(el2).html('');
    }
}

function getCurrentBPGoal() {
    let el = $('#currentBPGoal');
    return {
        systolic: $(el).attr('data-systolic'),
        diastolic: $(el).attr('data-diastolic')
    }
}

function buildPointStyleArray(data) {
    // see https://www.chartjs.org/docs/latest/configuration/elements.html for options
    let arr = [];
    data.forEach(function(item) {
        if (item.source === 'HOME' || item.source === 'HOME_BLUETOOTH') {
            arr.push('circle');
        } else if (item.source === 'OFFICE') {
            arr.push('rect');
        }
    });
    return arr;
}

function updateChart() {
    // calling buildChart() without first replacing the DOM element creates wonkiness
    $('#chart').replaceWith('<canvas id="chart" width="700" height="250"></canvas>');
    buildChart();
}

function truncateData(data, startDate) {
    return jQuery.grep(data, function (item) {
        return item.readingDate >= startDate;
    });
}

function toScatterData(data, type) {
    let arr = [];
    data.forEach(function (item) {
        let val = null;
        if (type === 'systolic' && item.systolic) {
            val = item.systolic.value;
        } else if (type === 'diastolic' && item.diastolic) {
            val = item.diastolic.value;
        }

        if (val !== null) {
            arr.push({
                x: new Date(item.readingDate),
                y: val
            });
        }
    });
    return arr;
}

function toLOESSData(data, type) {
    let map = data.map(function(item) {
        return item[type] != null ? [item.readingDate, item[type].value] : null;
    }).filter(function(item) {
        return item != null;
    });
    return loess(map, getLOESSBandwidth());
}

function getLOESSBandwidth() {
    return parseFloat($('#LOESSBandwidth').html());
}

function getDateRange(data) {
    let minDate = null;
    let maxDate = null;

    data.forEach(function (item) {
        let d = item.readingDate;
        if (minDate == null || d < minDate) {
            minDate = d;
        }
        if (maxDate == null || d > maxDate) {
            maxDate = d;
        }
    });

    return {
        min: minDate,
        max: maxDate
    }
}
