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
                data: toLOESSData2(window.bpchart.data, 'systolic')
            }, {
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
                data: toLOESSData2(window.bpchart.data, 'diastolic')
            } ]
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
                    type: 'time',
                    time: {
                        displayFormats: {
                            millisecond: 'MMM dd yyyy HH:mm:ss',
                            second: 'MMM dd yyyy H:mm',
                            minute: 'MMM dd yyyy H:mm',
                            hour: 'MMM dd yyyy ha',
                            day: 'MMM dd yyyy',
                            week: 'MMM dd yyyy',
                            month: 'MMM yyyy',
                            quarter: 'MMM yyyy',
                            year: 'MMM yyyy'
                        }
                    },
                    ticks: {
                        autoSkip: true,
                        maxTicksLimit: 15
                    }    
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
                    }
                }
            }
        }
    };

    let mostRecentBP = sortByDateDesc(window.bpdata)[0];
    if (mostRecentBP !== undefined && (mostRecentBP.systolic.value > 180 || mostRecentBP.diastolic.value > 120)) {
        // BP crisis
        config.options.plugins.annotation.annotations.recent = {
            drawTime: 'afterDatasetsDraw',
            id: 'recent',
            type: 'box',
            xScaleID: 'x',
            xMin: mostRecentBP.readingDate,
            backgroundColor: 'transparent',
            borderColor: 'rgba(255, 0, 0, 1)',
            borderWidth: 5,
            display: true
        };

    } else {
        let bpSetStartDate = window.bpchart.bpSetStartDate;
        let showRecentAnnotation = true;
        if (bpSetStartDate === undefined) {
            // On first call, get the BPSetStartDate for chartjs annotation
            bpSetStartDate = getBPSetStartDate(window.bpdata);
        }
        if (!bpSetStartDate) {//|| (window.bpchart.startDate !== undefined && window.bpchart.startDate == bpSetStartDate)) {
            // Don't shade recent BPs if no set exists or if "Recent" is selected in chart options
            showRecentAnnotation = false;
        }

        config.options.plugins.annotation.annotations.recent = {
            drawTime: 'afterDatasetsDraw',
            id: 'recent',
            type: 'box',
            xScaleID: 'x',
            xMin: bpSetStartDate,
            backgroundColor: 'transparent',
            borderColor: 'rgba(91, 107, 104, 1)',
            borderWidth: 5,
            display: showRecentAnnotation
        };
    }


    if (window.bpchart.startDate !== undefined) {
        let startDate = null;
        window.bpchart.data.forEach(function(item) {
            if (item.readingDate >= window.bpchart.startDate && (startDate === null || startDate < item.readingDate)) {
                startDate = item.readingDate;
            }
        });
        config.options.scales.x.suggestedMin = startDate;
    }

    if (window.bpchart.endDate !== undefined) {
        let endDate = null;
        window.bpchart.data.forEach(function(item) {
            if (item.readingDate < window.bpchart.endDate && (endDate === null || endDate > item.readingDate)) {
                endDate = item.readingDate;
            }
        });
        config.options.scales.x.suggestedMax = endDate;
    }

    return new Chart(ctx, config);
}