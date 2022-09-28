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
                data: toLOESSData2(window.bpchart.data, 'diastolic')
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