const LOINC = "http://loinc.org";

function extractPatientName(p) {
    if (p.name) {
        let names = p.name.map(function(name) {
            return name.given.join(" ") + " " + name.family;
        });
        return names.join(" / ");
    } else {
        return "anonymous";
    }
}

// fetchBloodPressureData adapted from https://docs.smarthealthit.org/client-js/fhirjs-equivalents
function fetchBloodPressureData(client, _callback) {
    let loincCodes = ['85354-9', '55284-4']; //, '55284-4', '8462-4', '8480-6'];
    let query = new URLSearchParams();
    query.set("patient", client.patient.id);
//    query.set("_count", 100); // Try this to fetch fewer pages

    query.set("code", loincCodes.map(item => "http://loinc.org|" + item).join(","));

    client.request("Observation?" + query, {
        pageLimit: 0,   // get all pages
        flat     : true // return flat array of Observation resources
    }).then(function(observations) {
        let arr = [];

        observations.forEach(function(o) {
            let effectiveDate = new Date(o.effectiveDateTime);
            let coding = getCoding(o.code.coding, LOINC);
            let systolicVal = getSystolic(o);
            let diastolicVal = getDiastolic(o);

            if (systolicVal !== null && diastolicVal !== null) {
                arr.push({
                    date: effectiveDate,
                    code: coding.code,
                    systolic: systolicVal,
                    diastolic: diastolicVal
                });
            }
        });

        arr.sort(function(a, b) { return a.date - b.date });
        _callback(observations, arr);
    });
}

function getSystolic(o) {
    let code = getCoding(o.code.coding, LOINC).code;
    if (code === "85354-9" || code === "55284-4") {
        return getComponent(o.component, "8480-6").valueQuantity.value;

    } else if (code === "8480-6") {
        return o.valueQuantity.value;

    } else {
        return null;
    }
}

function getDiastolic(o) {
    let code = getCoding(o.code.coding, LOINC).code;
    if (code === "85354-9" || code === "55284-4") {
        return getComponent(o.component, "8462-4").valueQuantity.value;

    } else if (code === "8462-4") {
        return o.valueQuantity.value;

    } else {
        return null;
    }
}

function getComponent(components, loincCode) {
    for (let i = 0; i < components.length; i ++) {
        let component = components[i];
        let coding = getCoding(component.code.coding, LOINC);
        if (coding && coding.code === loincCode) {
            return component;
        }
    }
    return undefined;
}

function getCoding(codings, system) {
    for (let i = 0; i < codings.length; i ++) {
        let coding = codings[i];
        if (coding.system === system) {
            return coding;
        }
    }
    return undefined;
}

function populateSummaryDiv(data, el) {
    let totalSystolic = 0;
    let totalDiastolic = 0;
    let avgSystolic = 0;
    let avgDiastolic = 0;

    if (Array.isArray(data) && data.length > 0) {
        data.forEach(function(o) {
            totalSystolic += o.systolic;
            totalDiastolic += o.diastolic;
        });

        avgSystolic = Math.round(totalSystolic / data.length);
        avgDiastolic = Math.round(totalDiastolic / data.length);

//        avgSystolic = Math.round(totalSystolic / data.length);
//        avgDiastolic = Math.round(totalDiastolic / data.length);
    }

    $(el).html("<div id='avgBP'>Average BP:<br/>" + avgSystolic + "/" + avgDiastolic + "</div>");
}

function populateBPList(data, el) {
    let success = false;

    if (Array.isArray(data) && data.length > 0) {
        el.innerHTML = "";
        data.forEach(function(o) {
            el.innerHTML += "<li>" + o.systolic + "/" + o.diastolic + " on " + o.date.toLocaleDateString() + "</li>";
            success = true;
        });
    }

    if ( ! success ) {
        el.innerHTML = "No observations found for the selected patient";
    }
}

function buildChartSlider(data) {
    let minYear = data[0].date.getFullYear();
    let maxYear = data[data.length-1].date.getFullYear();

    $('#chartRangeSlider').slider({
        range: true,
        min: minYear,
        max: maxYear,
        values: [minYear, maxYear],
        slide: function(event, ui) {
            $('#sliderRangeFrom').val(ui.values[0]);
            $('#sliderRangeTo').val(ui.values[1]);
            let truncatedData = truncateData(window.chartData, ui.values[0], ui.values[1]);
            populateBPList(truncatedData, document.getElementById('bpList'));
            updateChart(truncatedData);
        }
    });
    $('#sliderRangeFrom').val($('#chartRangeSlider').slider("values", 0));
    $('#sliderRangeTo').val($('#chartRangeSlider').slider("values", 1));
}

function truncateData(data, minYear, maxYear) {
    let truncatedData = jQuery.grep(data, function(item) {
        let y = item.date.getFullYear();
        return y >= minYear && y <= maxYear;
    });
    return truncatedData;
}

function updateChart(data) {
    // calling buildChart() without first replacing the DOM element creates wonkiness
    $('#chart').replaceWith('<canvas id="chart" width="800" height="400"></canvas>');
    buildChart(data);
}

function buildChart(data) {
    var ctx = $('#chart');

    var chart = new Chart(ctx, {
        type: 'line',
        data: {
            datasets: [{
                type: 'scatter',
                label: 'Systolic',
                backgroundColor: 'red',
                data: toScatterData(data, 'systolic'),
                pointBorderColor: 'red',
                pointBackgroundColor: 'white'
            },{
                type: 'line',
                label: 'Systolic Trend',
                pointRadius: 1,
                fill: false,
                borderColor: 'rgba(255, 0, 0, 1)',
                borderWidth: 1,
                data: toTrendLineData(data, 'systolic')
            },{
                type: 'line',
                label: 'Systolic Regression',
                pointRadius: 0,
                fill: false,
                borderColor: 'rgba(255, 0, 0, 0.3)',
                borderWidth: 3,
                data: toRegressionData(data, 'systolic')
            },{
                type: 'scatter',
                label: 'Diastolic',
                backgroundColor: 'blue',
                data: toScatterData(data, 'diastolic'),
                pointBorderColor: 'blue',
                pointBackgroundColor: 'white'
            },{
                type: 'line',
                label: 'Diastolic Trend',
                pointRadius: 1,
                fill: false,
                borderColor: 'rgba(0, 0, 255, 1)',
                borderWidth: 1,
                data: toTrendLineData(data, 'diastolic')
            },{
                type: 'line',
                label: 'Diastolic Regression',
                pointRadius: 0,
                fill: false,
                borderColor: 'rgba(0, 0, 255, 0.3)',
                borderWidth: 3,
                data: toRegressionData(data, 'diastolic')
            }]
        },
        options: {
            title: {
                text: "Blood Pressure"
            },
            scales: {
                xAxes: [{
                    type: 'time'
                }],
                yAxes: [{
                    type: 'linear',
                    ticks: {
                        suggestedMin: 50,
                        suggestedMax: 160
                    }
                }]
            },
            annotation: {
                drawTime: 'beforeDatasetsDraw',
                annotations: [{
                    id: 'max-systolic',
                    type: 'line',
                    mode: 'horizontal',
                    scaleID: 'y-axis-0',
                    value: '130',
                    borderColor: 'rgba(255, 0, 0, 0.3)',
                    borderDash: [5],
                    borderWidth: 1
                },{
                    id: 'max-diastolic',
                    type: 'line',
                    mode: 'horizontal',
                    scaleID: 'y-axis-0',
                    value: '80',
                    borderColor: 'rgba(0, 0, 255, 0.3)',
                    borderDash: [5],
                    borderWidth: 1
                }]
            }
        }
    });
}

function toScatterData(data, type) {
    let arr = [];
    data.forEach(function(item) {
        let val = type == 'systolic' ? item.systolic : item.diastolic;
        arr.push({
            x: moment(item.date),
            y: val
        });
    });
    return arr;
}

function toTrendLineData(data, type) {
    var chunks = 20;
    var groupingFactor = 10;
    var dateRange = getDateRange(data);
    var minTime = dateRange.min.getTime();
    var maxTime = dateRange.max.getTime();
    var threshold = Math.round((maxTime - minTime) / chunks);

    let arr = [];
    let tempArr = [];
    let lastDate = null;
    let distanceFromLastDate = null;
    let diffArr = [];

    data.forEach(function(item) {
        let val = type == 'systolic' ? item.systolic : item.diastolic;

        if (lastDate !== null) {
            let diff = item.date.getTime() - lastDate.getTime();
            diffArr.push(diff);
            let avgDiff = Math.round(diffArr.reduce((a, b) => a + b, 0) / diffArr.length);

            if (diff > threshold || (diffArr.length > 1 && diff > avgDiff * groupingFactor)) {
                let lastDates = tempArr.map(o => o.date.getTime());
                let lastDateAvg = Math.round(lastDates.reduce((a, b) => a + b, 0) / lastDates.length);
                let lastVals = tempArr.map(o => o.val);
                let lastValsAvg = Math.round(lastVals.reduce((a, b) => a + b, 0) / lastVals.length);
                arr.push({
                    x: moment(new Date(lastDateAvg)),
                    y: lastValsAvg
                });
                diffArr = [];
                tempArr = [];
            }
        }

        tempArr.push({
            date: item.date,
            val: val
        });
        lastDate = item.date;
    });

    if (tempArr.length > 0) {   // process final records
        let lastDates = tempArr.map(o => o.date.getTime());
        let lastDateAvg = Math.round(lastDates.reduce((a, b) => a + b, 0) / lastDates.length);
        let lastVals = tempArr.map(o => o.val);
        let lastValsAvg = Math.round(lastVals.reduce((a, b) => a + b, 0) / lastVals.length);
        arr.push({
            x: moment(new Date(lastDateAvg)),
            y: lastValsAvg
        });
    }

    return arr;
}

function getDateRange(data) {
    var minDate = null;
    var maxDate = null;

    data.forEach(function(item) {
        let d = item.date;
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

function toRegressionData(data, type) {
    // the regression library can't handle large X values (where "large" is not really that large at all),
    // so we need to finagle the data it consumes so that can process things correctly.  the way I've decided
    // to do this is to feed it *proportional* values (numbers between 0 and 100) that represent the relative
    // position of those dates with respect to the min and max dates in the dataset.  so that's what's going
    // on here.

    // first calculate min and max dates in the dataset -
    var dateRange = getDateRange(data);
    var minTime = dateRange.min.getTime();
    var maxTime = dateRange.max.getTime();

    // now we craft an array of arrays that we'll feed to the regression engine, where the X value
    // is a number between 0 and 100 that represents the relative position of the date in the range
    var arr = [];
    data.forEach(function(item, i) {
        let val = type == 'systolic' ? item.systolic : item.diastolic;
        arr.push([
            ((item.date.getTime() - minTime) / (maxTime - minTime)) * 100,
            val
        ]);
    });

    // calculate the regression (duh) -
    var result = regression.linear(arr);

    // then construct a new array of {x,y} objects where the X value is the original date used in the
    // dataset (we don't care about the proportions anymore, that was just for calculating regression)
    var r = [];
    result.points.forEach(function(item, i) {
        r.push({
            x: moment(data[i].date),
            y: item[1]
        });
    });
    return r;
}
