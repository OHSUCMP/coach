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
        let name = $(el).attr('data-name');
        let html = 'Your Active ' + name + ': ' + arr.join(', ');

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
        data.forEach(function (o) {
            totalSystolic += o.systolic.value;
            totalDiastolic += o.diastolic.value;
        });

        avgSystolic = Math.round(totalSystolic / data.length);
        avgDiastolic = Math.round(totalDiastolic / data.length);
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

function buildChart() {
    $('#loadingChart').addClass('hidden');
    let elNoData = $('#noChartData');
    let ctx = $('#chart');

    if (window.bpchart.data.length === 0) {
        $(elNoData).removeClass('hidden');
        $(ctx).addClass('hidden');
        return;
    }

    $(elNoData).addClass('hidden');
    $(ctx).removeClass('hidden');


    $(ctx).removeClass('hidden');
    $('#chartKeyContainer, #chartTimelineContainer').removeClass('hidden');

    let pointStyleArr = buildPointStyleArray(window.bpchart.data);

    let goal = getCurrentBPGoal();

    let config = {
        type: 'line',
        data: {
            datasets: [{
                type: 'scatter',
                label: 'Systolic',
                pointRadius: 3,
                pointStyle: pointStyleArr,
                fill: false,
                borderColor: 'rgba(126, 194, 185, 0.6)',
                borderWidth: 2,
                pointBorderColor: 'rgba(126, 194, 185, 1)',
                pointBackgroundColor: 'rgba(126, 194, 185, 0.6)',
                tension: 0,
                data: toScatterData(window.bpchart.data, 'systolic')
            }, {
                type: 'line',
                label: 'Systolic Trend',
                pointRadius: 0,
                fill: false,
                borderColor: 'rgba(0, 127, 109, 1)',
                borderWidth: 2,
                tension: 0.1,
                data: toTrendLineData(window.bpchart.data, 'systolic')
            }, /* {
                type: 'line',
                label: 'Systolic Regression',
                pointRadius: 0,
                fill: false,
                borderColor: 'rgba(255, 0, 0, 0.3)',
                borderWidth: 3,
                data: toRegressionData(data, 'systolic')
            },*/ {
                type: 'scatter',
                label: 'Diastolic',
                pointRadius: 3,
                pointStyle: pointStyleArr,
                fill: false,
                borderColor: 'rgba(207, 178, 137, 0.6)',
                borderWidth: 2,
                pointBorderColor: 'rgba(207, 178, 137, 1)',
                pointBackgroundColor: 'rgba(207, 178, 137, 0.6)',
                tension: 0,
                data: toScatterData(window.bpchart.data, 'diastolic')
            }, {
                type: 'line',
                label: 'Diastolic Trend',
                pointRadius: 0,
                fill: false,
                borderColor: 'rgba(153, 97, 36, 1)',
                borderWidth: 1,
                tension: 0.1,
                data: toTrendLineData(window.bpchart.data, 'diastolic')
            } /*, {
                type: 'line',
                label: 'Diastolic Regression',
                pointRadius: 0,
                fill: false,
                borderColor: 'rgba(0, 0, 255, 0.3)',
                borderWidth: 3,
                data: toRegressionData(data, 'diastolic')
            }*/ ]
        },
        options: {
            title: {
                text: "Blood Pressure"
            },
            legend: {
                labels: {
                    usePointStyle: true
                }
            },
            scales: {
                x: {
                    type: 'time'
                },
                y: {
                    type: 'linear',
                    title: {
                        text: 'Blood Pressure',
                        display: true
                    },
                    suggestedMin: 0,
                    suggestedMax: 200
                }
            },
            plugins: {
                legend: {
                    display: false
                },
                annotation: {
                    annotations: {
                        targetSystolic: {
                            drawTime: 'beforeDatasetsDraw',
                            id: 'target-systolic',
                            type: 'box',
                            yScaleID: 'y',
                            yMin: 90,
                            yMax: goal.systolic,
                            backgroundColor: 'rgba(172, 242, 233, 0.5)',
                            borderWidth: 0
                        },
                        targetDiastolic: {
                            drawTime: 'beforeDatasetsDraw',
                            id: 'target-diastolic',
                            type: 'box',
                            yScaleID: 'y',
                            yMin: 60,
                            yMax: goal.diastolic,
                            backgroundColor: 'rgba(244, 225, 172, 0.5)',
                            borderWidth: 0
                        }
                        // systolicGoal: {
                        //     type: 'line',
                        //     yMin: goal.systolic,
                        //     yMax: goal.systolic,
                        //     borderColor: 'rgb(255,0,0)', //'rgba(73,167,156,1)',
                        //     borderWidth: 1
                        // },
                        // diastolicGoal: {
                        //     type: 'line',
                        //     yMin: goal.diastolic,
                        //     yMax: goal.diastolic,
                        //     borderColor: 'rgb(255,0,255)', //'rgba(167,139,51,1)',
                        //     borderWidth: 1
                        // }
                    }
                }
            }
        }
    };

    if (window.bpchart.startDate !== undefined) {
        config.options.scales.x.suggestedMin = window.bpchart.startDate;
    }

    if (window.bpchart.endDate !== undefined) {
        config.options.scales.x.suggestedMax = window.bpchart.endDate;
    }

    return new Chart(ctx, config);
}

function buildPointStyleArray(data) {
    // see https://www.chartjs.org/docs/latest/configuration/elements.html for options
    let arr = [];
    data.forEach(function(item) {
        if (item.source === 'HOME') {
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

/*
function buildChartSlider() {
    let el = $('#chartRangeSlider');
    let data = window.bpdata;
    let minYear = data[0].timestamp.getFullYear();
    let maxYear = data[data.length - 1].timestamp.getFullYear();

    $(el).slider({
        range: true,
        min: minYear,
        max: maxYear,
        values: [minYear, maxYear],
        slide: function (event, ui) {
            $('#sliderRangeFrom').val(ui.values[0]);
            $('#sliderRangeTo').val(ui.values[1]);
            let truncatedData = truncateData(window.chartData, ui.values[0], ui.values[1]);
            updateChart(truncatedData);
        }
    });
    $('#sliderRangeFrom').val($(el).slider("values", 0));
    $('#sliderRangeTo').val($(el).slider("values", 1));
}
*/

// function truncateData(data, minYear, maxYear) {
//     return jQuery.grep(data, function (item) {
//         let y = item.timestamp.getFullYear();
//         return y >= minYear && y <= maxYear;
//     });
// }

function truncateData(data, startDate) {
    return jQuery.grep(data, function (item) {
        return item.readingDate >= startDate;
    });
}

function toScatterData(data, type) {
    let arr = [];
    data.forEach(function (item) {
        let val = type === 'systolic' ? item.systolic.value : item.diastolic.value;
        arr.push({
            x: new Date(item.readingDate),
            y: val
        });
    });
    return arr;
}

function toTrendLineData(data, type) {
    let chunks = 20;
    let groupingFactor = 10;
    let dateRange = getDateRange(data);
    let minTime = dateRange.min.getTime();
    let maxTime = dateRange.max.getTime();
    let threshold = Math.round((maxTime - minTime) / chunks);

    let arr = [];
    let tempArr = [];
    let lastDate = null;
    // let distanceFromLastDate = null;
    let diffArr = [];

    data.forEach(function (item) {
        let val = type === 'systolic' ? item.systolic.value : item.diastolic.value;

        if (lastDate !== null) {
            let diff = item.readingDate.getTime() - lastDate.getTime();
            diffArr.push(diff);
            let avgDiff = Math.round(diffArr.reduce((a, b) => a + b, 0) / diffArr.length);

            if (diff > threshold || (diffArr.length > 1 && diff > avgDiff * groupingFactor)) {
                let lastDates = tempArr.map(o => o.timestamp.getTime());
                let lastDateAvg = Math.round(lastDates.reduce((a, b) => a + b, 0) / lastDates.length);
                let lastVals = tempArr.map(o => o.val);
                let lastValsAvg = Math.round(lastVals.reduce((a, b) => a + b, 0) / lastVals.length);
                arr.push({
                    x: new Date(lastDateAvg),
                    y: lastValsAvg
                });
                diffArr = [];
                tempArr = [];
            }
        }

        tempArr.push({
            timestamp: item.readingDate,
            val: val
        });
        lastDate = item.readingDate;
    });

    if (tempArr.length > 0) {   // process final records
        let lastDates = tempArr.map(o => o.timestamp.getTime());
        let lastDateAvg = Math.round(lastDates.reduce((a, b) => a + b, 0) / lastDates.length);
        let lastVals = tempArr.map(o => o.val);
        let lastValsAvg = Math.round(lastVals.reduce((a, b) => a + b, 0) / lastVals.length);
        arr.push({
            x: new Date(lastDateAvg),
            y: lastValsAvg
        });
    }

    return arr;
}

// function toRegressionData(data, type) {
//     // the regression library can't handle large X values (where "large" is not really that large at all),
//     // so we need to finagle the data it consumes so that can process things correctly.  the way I've decided
//     // to do this is to feed it *proportional* values (numbers between 0 and 100) that represent the relative
//     // position of those dates with respect to the min and max dates in the dataset.  so that's what's going
//     // on here.
//
//     // first calculate min and max dates in the dataset -
//     var dateRange = getDateRange(data);
//     var minTime = dateRange.min.getTime();
//     var maxTime = dateRange.max.getTime();
//
//     // now we craft an array of arrays that we'll feed to the regression engine, where the X value
//     // is a number between 0 and 100 that represents the relative position of the date in the range
//     var arr = [];
//     data.forEach(function (item, i) {
//         let val = type === 'systolic' ? item.systolic.value : item.diastolic.value;
//         arr.push([
//             ((item.timestamp.getTime() - minTime) / (maxTime - minTime)) * 100,
//             val
//         ]);
//     });
//
//     // calculate the regression (duh) -
//     var result = regression.linear(arr);
//
//     // then construct a new array of {x,y} objects where the X value is the original date used in the
//     // dataset (we don't care about the proportions anymore, that was just for calculating regression)
//     var r = [];
//     result.points.forEach(function (item, i) {
//         r.push({
//             x: new Date(data[i].timestamp),
//             // x: moment(data[i].timestamp),
//             y: item[1]
//         });
//     });
//     return r;
// }

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
