function populateSummaryDiv() {
    let data = window.bpdata;
    let totalSystolic = 0;
    let totalDiastolic = 0;
    let avgSystolic = 0;
    let avgDiastolic = 0;

    if (Array.isArray(data) && data.length > 0) {
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
    let iconColor = 'green';
    let iconHTML = '&nbsp;';
    let bpLabel = 'NORMAL';

    if (avgSystolic > 180 || avgDiastolic > 120) { // crisis
        iconColor = 'red';
        iconHTML = '!!!';
        bpLabel = 'Hypertension Crisis';

    } else if (avgSystolic >= 140 || avgDiastolic >= 90) { // stage 2
        iconColor = 'red';
        iconHTML = '!!';
        bpLabel = 'Hypertension Stage 2';

    } else if ((avgSystolic >= 130 && avgSystolic < 140) || (avgDiastolic >= 80 && avgDiastolic < 90)) { // stage 1
        iconColor = 'yellow';
        iconHTML = '!';
        bpLabel = 'Hypertension Stage 1';

    } else if (avgSystolic >= 120 && avgSystolic < 130 && avgDiastolic < 80) { // elevated
        iconColor = 'yellow';
        iconHTML = '!';
        bpLabel = 'Elevated';

    } else if (avgSystolic < 120 && avgDiastolic < 80) { // normal
        iconColor = 'green';
        iconHTML = '&nbsp;';
        bpLabel = 'Normal';
    }

    let icon = $('#bpIcon');
    $(icon).attr('style', 'background-color: ' + iconColor);
    $(icon).html(iconHTML);
    $('#bpLabel').html(bpLabel + ':');
}

function buildChart() {
    let data = window.bpdata;

    $('#loadingChart').addClass('hidden');

    let el = $('#chart');
    $(el).removeClass('hidden');

    return new Chart(el, {
        type: 'line',
        data: {
            datasets: [{
                type: 'scatter',
                label: 'Systolic',
                backgroundColor: 'red',
                data: toScatterData(data, 'systolic'),
                pointBorderColor: 'red',
                pointBackgroundColor: 'white'
            }, {
                type: 'line',
                label: 'Systolic Trend',
                pointRadius: 1,
                fill: false,
                borderColor: 'rgba(255, 0, 0, 1)',
                borderWidth: 1,
                data: toTrendLineData(data, 'systolic')
            }, {
                type: 'line',
                label: 'Systolic Regression',
                pointRadius: 0,
                fill: false,
                borderColor: 'rgba(255, 0, 0, 0.3)',
                borderWidth: 3,
                data: toRegressionData(data, 'systolic')
            }, {
                type: 'scatter',
                label: 'Diastolic',
                backgroundColor: 'blue',
                data: toScatterData(data, 'diastolic'),
                pointBorderColor: 'blue',
                pointBackgroundColor: 'white'
            }, {
                type: 'line',
                label: 'Diastolic Trend',
                pointRadius: 1,
                fill: false,
                borderColor: 'rgba(0, 0, 255, 1)',
                borderWidth: 1,
                data: toTrendLineData(data, 'diastolic')
            }, {
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
                }, {
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

function updateChart(data) {
    // calling buildChart() without first replacing the DOM element creates wonkiness
    $('#chart').replaceWith('<canvas id="chart" width="500" height="250"></canvas>');
    buildChart(data);
}

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

function truncateData(data, minYear, maxYear) {
    return jQuery.grep(data, function (item) {
        let y = item.timestamp.getFullYear();
        return y >= minYear && y <= maxYear;
    });
}

function toScatterData(data, type) {
    let arr = [];
    data.forEach(function (item) {
        let val = type === 'systolic' ? item.systolic.value : item.diastolic.value;
        arr.push({
            x: moment(item.timestamp),
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
    let distanceFromLastDate = null;
    let diffArr = [];

    data.forEach(function (item) {
        let val = type === 'systolic' ? item.systolic.value : item.diastolic.value;

        if (lastDate !== null) {
            let diff = item.timestamp.getTime() - lastDate.getTime();
            diffArr.push(diff);
            let avgDiff = Math.round(diffArr.reduce((a, b) => a + b, 0) / diffArr.length);

            if (diff > threshold || (diffArr.length > 1 && diff > avgDiff * groupingFactor)) {
                let lastDates = tempArr.map(o => o.timestamp.getTime());
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
            timestamp: item.timestamp,
            val: val
        });
        lastDate = item.timestamp;
    });

    if (tempArr.length > 0) {   // process final records
        let lastDates = tempArr.map(o => o.timestamp.getTime());
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

    data.forEach(function (item) {
        let d = item.timestamp;
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
    data.forEach(function (item, i) {
        let val = type === 'systolic' ? item.systolic.value : item.diastolic.value;
        arr.push([
            ((item.timestamp.getTime() - minTime) / (maxTime - minTime)) * 100,
            val
        ]);
    });

    // calculate the regression (duh) -
    var result = regression.linear(arr);

    // then construct a new array of {x,y} objects where the X value is the original date used in the
    // dataset (we don't care about the proportions anymore, that was just for calculating regression)
    var r = [];
    result.points.forEach(function (item, i) {
        r.push({
            x: moment(data[i].timestamp),
            y: item[1]
        });
    });
    return r;
}
