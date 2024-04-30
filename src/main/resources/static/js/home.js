// Match the enum ObservationSource in Java
const BPSource = Object.freeze({
    Home: "HOME",
    CoachUI: "COACH_UI",
    Omron: "OMRON",
    Office: "OFFICE",
    Unknown: "UNKNOWN"
});

function isEnhanced() {
    return $('#randomizationGroup').text() === 'ENHANCED';
}

function isBasic() {
    return $('#randomizationGroup').text() === 'BASIC';
}

function doClearSupplementalData(_callback) {
    $.ajax({
        method: "POST",
        url: "/clear-supplemental-data"
    }).done(function() {
        _callback();
    });
}

function getBloodPressureObservations(_callback) {
    $.ajax({
        method: "POST",
        url: "/blood-pressure-observations-list"
    }).done(function(bpdata) {
        bpdata.forEach(function(item) {
            item.readingDate = new Date(item.readingDate);
        });

        bpdata.sort(function(a, b) {
            return a.readingDate - b.readingDate;
        });
        _callback(bpdata);
    });
}

function getMedications(_callback) {
    $.ajax({
        method: "POST",
        url: "/medications-list"
    }).done(function(meds) {
        _callback(meds);
    });
}

function populateMedications() {
    let data = window.meds;

    if (Array.isArray(data) && data.length > 0) {
        let arr = [];
        data.forEach(function(m) {
            arr.push('<span class="medication" data-system="' + m.system + '" data-code="' + m.code + '">' +
                m.description +
                '</span>');
        });

        let el = $('#currentMedications');
        let html = 'Your Active Antihypertensive Medications: ' + arr.join(', ');

        $(el).html(html);
    }
}

function getAdverseEvents(_callback) {
    $.ajax({
        method: "POST",
        url: "/adverse-events-list"
    }).done(function(adverseEvents) {
        _callback(adverseEvents);
    });
}

function populateAdverseEvents() {
    let data = window.adverseEvents;

    if (Array.isArray(data) && data.length > 0) {
        let arr = [];
        data.forEach(function(m) {
            arr.push('<span class="adverseEvent" data-system="' + m.system + '" data-code="' + m.code + '">' +
                m.description +
                '</span>');
        });

        let html = 'Your Adverse Events: ' + arr.join(', ');

        $('#adverseEvents').html(html);
    }
}

function getOmronStatus(_callback) {
    // console.log('loading Omron status -');
    $.ajax({
        method: "POST",
        url: "/omron/status"
    }).done(function(status) {
        // console.log('got Omron status: ' + JSON.stringify(status));
        _callback(status);
    });
}

function populateOmronStatus(data) {
    let el = $('#omron');
    let html = '';
    if (data !== undefined && data.status !== 'DISABLED') {
        if (isOmronSyncCompleted(data)) {
            html += '<div class="alert alert-warning" role="alert">Omron data synchronization is complete.</div>';

        } else if (data.status === 'READY') {
            let omronAuthRequestUrl = $('#omronAuthRequestUrl').html();
            html += '<div class="alert alert-warning" role="alert"><span id="omronAuthLink" class="link" data-target="' +
                omronAuthRequestUrl + '">Click here</span> to authenticate and synchronize with Omron.</div>';
            if (data.lastUpdated !== null) {
                html += '<div class="alert alert-teal" role="alert">Omron data last synchronized <em>' + data.lastUpdatedString + '</em>.</div>';
            }

        } else if (data.status === 'INITIATING_SYNC') {
            html += '<div class="alert alert-warning" role="alert">Initiating Omron synchronization.  Continue to use COACH as normal, but don\'t log out until this process is completed.</div>';

        } else if (data.status === 'SYNCHRONIZING') {
            let percentComplete = Math.floor(data.currentlyProcessing / data.totalToProcess * 100);
            html += '<div class="alert alert-warning" role="alert">Omron data is synchronizing (' + percentComplete +
                '% complete).  Continue to use COACH as normal, but don\'t log out until this process is completed.</div>';
        }
    }
    $(el).html(html);
}

function refreshChart() {
    // calling buildChart() without first replacing the DOM element creates wonkiness
    $('#chart').replaceWith('<canvas id="chart"></canvas>');
    getBloodPressureObservations(function(bpdata) {
        window.bpdata = bpdata;
        window.bpchart = {};
        window.bpchart.data = window.bpdata;

        populateSummaryDiv();
        buildChart();
    });
}

function refreshAdverseEvents() {
    getAdverseEvents(function(adverseEvents) {
        window.adverseEvents = adverseEvents;
        populateAdverseEvents();
    });
}

function refreshMedications() {
    getMedications(function(meds) {
        window.meds = meds;
        populateMedications();
    });
}

function refreshRecommendations() {
    getRecommendations(function(container, cards) {
        if (cards) {
            let html = renderCards(cards);
            $(container).html(html);
            if (html === '') {
                $(container).closest('.recommendation').addClass('hidden');

            } else {
                $(container).closest('.recommendation').removeClass('hidden');
                $(container).find('.bpGoal input.systolic').inputmask({
                    regex: "1?[0-9]{2}"
                });
                $(container).find('.bpGoal input.diastolic').inputmask({
                    regex: "1?[0-9]{2}"
                });
            }
        }
    });
}

function refreshOmronStatus() {
    getOmronStatus(function(omronStatus) {
        populateOmronStatus(omronStatus);

        if (omronStatus !== undefined && (omronStatus.status === 'INITIATING_SYNC' || omronStatus.status === 'SYNCHRONIZING')) {
            setTimeout(refreshOmronStatus, 1000);

        } else if (isOmronSyncCompleted(omronStatus)) {
            setTimeout(refreshOmronStatusAndPageAssets, 5000);
        }

        window.omronStatus = omronStatus;
    });
}

function refreshOmronStatusAndPageAssets() {
    refreshOmronStatus();
    refreshChart();
    refreshAdverseEvents();
    refreshRecommendations();
}

function isOmronSyncCompleted(omronStatus) {
    return window.omronStatus !== undefined &&
        (window.omronStatus.status === 'INITIATING_SYNC' || window.omronStatus.status === 'SYNCHRONIZING') &&
        omronStatus !== undefined &&
        omronStatus.status === 'READY';
}

// Sort blood pressures in date order, returning a new sorted array
function sortByDateAsc(bps) {
    return bps.slice(0).sort((a, b) => Number(a.readingDate) - Number(b.readingDate));
}

// Sort blood pressures in reverse date order, returning a new sorted array
function sortByDateDesc(bps) {
    return bps.slice(0).sort((a, b) => Number(b.readingDate) - Number(a.readingDate));
}

/**
 * Get the set of BPs to use in average calculation or null if a set doesn't exist
 * @param {*} bps
 * @returns
 */
function getBPSet(bps) {
    const bpsDesc = sortByDateDesc(bps);
    let set = bpsDesc.reduce((acc, bp) => {
        if (acc.score >= 4.0) {
            return acc;
        }
        acc.bpset.push(bp);
        if (bp.source === BPSource.Home || bp.source === BPSource.CoachUI || bp.source === BPSource.Omron ) {
            acc.score += 0.334;
        } else {
            // Anything explicitly not home is considered OFFICE and given 1 point
            acc.score += 1.0;
        }
        return acc;
    }, {
        score: 0.0,
        bpset: []
    });

    if (set.score >= 4) {
        return set.bpset;
    }

    return null;
}

function getBPSetStartDate(bps) {
    const bpset = getBPSet(bps);
    if (bpset) {
        const sorted = sortByDateAsc(bpset);
        return sorted[0].readingDate;
    }

    return null;
}

/* Calculate the BP Average using the same logic from the recommendations */
function calculateAverageBP(bps) {
    const bpset = getBPSet(bps);
    if (bpset) {
        const systolicReadings = bpset.filter(r => r.systolic !== null).map(r => r.systolic.value);
        const avgSys = systolicReadings.reduce((acc, val) => acc + val, 0) / systolicReadings.length;
        const diastolicReadings = bpset.filter(r => r.diastolic !== null).map(r => r.diastolic.value);
        const avgDia = diastolicReadings.reduce((acc, val) => acc + val, 0) / diastolicReadings.length;
        return {
            systolic: avgSys,
            diastolic: avgDia
        };

    }

    return null;
}

function isCrisisBP(bp) {
    return bp.systolic.value >= 180 || bp.diastolic.value >= 120
}

function isLowCrisisBP(bp) {
    return bp.systolic.value < 90 || bp.diastolic.value < 60;
}

function within14Days(bp) {
    // Note that 'today' will be set based on timezone of the browser, so it may not be a completely accurate comparison.
    // We're assuming it's enough to know that the two BPs are relatively recent.
    const today = new Date();
    return Math.floor((today - bp.readingDate) / (1000*60*60*24)) <= 14;
}

// Generic function to remove classes col-md-? and col-lg-? so the actual numbers can change if need be.
function removeBootstrapCols(selection) {
    selection.removeClass(function(n, c) {
        const match = c.match(/(col-md-(\d)+)/);
        if (c.includes("col-md")) return match[1];
        else return "";
    });
    selection.removeClass(function(n, c) {
        const match = c.match(/(col-lg-(\d)+)/);
        if (c.includes("col-lg")) return match[1];
        else return "";
    });
}

function populateSummaryDiv() {
    let mostRecentBP = {};
    let crisisBP = false;
    let lowCrisisBP = false;
    let twoCrisisBPs = false;
    let twoLowCrisisBPs = false;
    let avgSystolic = 0;
    let avgDiastolic = 0;
    let aboveGoal = false;

    let hasData = Array.isArray(window.bpdata) && window.bpdata.length > 0;
    if (hasData) {
        const bpsByDateDesc = sortByDateDesc(window.bpdata);
        mostRecentBP = bpsByDateDesc[0];
        crisisBP = isCrisisBP(mostRecentBP);
        lowCrisisBP = isLowCrisisBP(mostRecentBP);
        const nextMostRecentBP = bpsByDateDesc[1];
        // The two crisis BPs need to have been taken within the last 14 days
        twoCrisisBPs = crisisBP && nextMostRecentBP !== undefined && isCrisisBP(nextMostRecentBP) && within14Days(mostRecentBP) && within14Days(nextMostRecentBP);
        twoLowCrisisBPs = lowCrisisBP && nextMostRecentBP !== undefined && isLowCrisisBP(nextMostRecentBP) && within14Days(mostRecentBP) && within14Days(nextMostRecentBP);
        let avg = calculateAverageBP(window.bpdata);
        if (avg) {
            avgSystolic = Math.round(avg.systolic);
            avgDiastolic = Math.round(avg.diastolic);

            let currentBPGoal = getCurrentBPGoal();
            aboveGoal = avgSystolic > currentBPGoal.systolic || avgDiastolic > currentBPGoal.diastolic;
        }
    }

    let bpIcon = $('#bpIcon');
    let bpIndicatorContainer = $('#bpIndicatorContainer');
    let chartContainer = $('#chartContainer');
    let bpPlaceholder = $('#bpPlaceholder');
    let bpContainer = $('#bpContainer');
    let bpLabel = $('#bpLabel');
    let bpNote = $('#bpNote');
    let systolic = $('#systolic');
    let diastolic = $('#diastolic');

    if (twoCrisisBPs || crisisBP || twoLowCrisisBPs || lowCrisisBP) {
        bpIcon.html("<img src='/images/critical-icon.png' class='bp-icon' alt='Critical' /><span class='tiptext'>Your most recent BP is very high. Check the Resources tab to learn more about what to do.</span>");
        bpIcon.show();
        removeBootstrapCols(bpIndicatorContainer);
        chartContainer.hide();
        bpPlaceholder.hide();
        bpContainer.show();

        bpLabel.html('Most Recent BP:');
        bpLabel.show();
        systolic.html(mostRecentBP.systolic.value);
        systolic.addClass('crisis');
        diastolic.html(mostRecentBP.diastolic.value);
        diastolic.addClass('crisis');
        if (twoCrisisBPs) {
            bpNote.html('Warning: Your most recent BP is still very high. Take action below.');
        } else if (crisisBP) {
            bpNote.html('Warning: Your BP is very high. Take action below.');
        } else if (twoLowCrisisBPs) {
            bpNote.html('Warning: Your most recent BP is still very low. Take action below.');
        } else if (lowCrisisBP) {
            bpNote.html('Warning: Your BP is very low. Take action below.');
        }
        bpNote.addClass('crisis');
        bpNote.show();

    } else if (avgSystolic === 0) { // not enough readings to compute average
        // Add the placeholder image and text
        bpPlaceholder.show();
        bpPlaceholder.append("<img src='/images/info-icon.png' class='bp-icon' alt='Enter more blood pressures to see average' title='Enter more blood pressures to see average'/>");
        bpPlaceholder.append("<div class='mt-4'>Enter more blood pressures to see average</div>");

    } else if (isBasic()) {
        removeBootstrapCols(bpIndicatorContainer);
        chartContainer.hide();
        bpIcon.hide();
        bpNote.hide();
        bpPlaceholder.hide();
        bpContainer.show();

        bpLabel.html('Most Recent BP:');
        bpLabel.show();
        systolic.html(mostRecentBP.systolic.value);
        systolic.removeClass('crisis');
        diastolic.html(mostRecentBP.diastolic.value);
        diastolic.removeClass('crisis');

    } else if (isEnhanced()) {
        if (aboveGoal) {
            bpIcon.html("<img src='/images/stoplight-yellow.png' class='bp-icon' alt='Above Goal' /><span class='tiptext'>Your average BP is above goal. Average is calculated based on a maximum of 12 recent readings. Check the Resources tab to learn more about what to do.</span>");
            bpNote.html('Your BP is above your goal!');
        } else {
            bpIcon.html("<img src='/images/stoplight-green.png' class='bp-icon' alt='At or Below Goal' /><span class='tiptext'>Your average BP is at goal. Average is calculated based on a maximum of 12 recent readings. Check the Resources tab to learn more about what to do.</span>");
            bpNote.html('You reached your goal!');
        }

        bpIcon.show();
        bpNote.removeClass('crisis');
        bpNote.show();
        bpPlaceholder.hide();
        bpContainer.show();

        bpLabel.html('Recent BP Average:').attr('title', 'Average of the last several readings shaded in grey');
        systolic.html(avgSystolic);
        systolic.removeClass('crisis');
        diastolic.html(avgDiastolic);
        diastolic.removeClass('crisis');
    } else {
        console.error("Unexpected randomization group")
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
        if (item.source === BPSource.Home || item.source === BPSource.CoachUI || item.source === BPSource.Omron ) {
            arr.push('rect');
        } else if (item.source === BPSource.Office) {
            arr.push('circle');
        }
    });
    return arr;
}

function updateChart() {
    // calling buildChart() without first replacing the DOM element creates wonkiness
    $('#chart').replaceWith('<canvas id="chart"></canvas>');
    buildChart();
}

function truncateData(data, startDate) {
    return jQuery.grep(data, function(item) {
        return item.readingDate >= startDate;
    });
}

function toScatterData(data, type) {
    let arr = [];
    data.forEach(function(item) {
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

/**
 * Create a LOESS trendline for the BP data using the following rules:
 * - Eliminate untyped BP data
 * - If bandwidth is unspecified in URL, choose the starting value based on size of data
 * - If regression errors occur or any regression points are outside the min/max BP value, increase the bandwidth and try again
 * @param {*} data
 * @param {*} type
 * @returns
 */
function toLOESSData2(data, type) {
    const sortedData = sortByDateAsc(data);

    let map = sortedData.map(function(item) {
        return item[type] != null ? [item.readingDate, item[type].value] : null;
    }).filter(function(item) {
        return item != null;
    });

    // Adjust bandwidth based on number of points in the data
    let bandwidth = 0.3;
    let numPts = map.length;
    // Allow an override of the bandwidth in the URL for testing
    if (getLOESSBandwidth() !== -1) {
        bandwidth = getLOESSBandwidth();
    } else if (numPts <= 8) {
        bandwidth = 0.6;
    } else if (numPts <= 15) {
        bandwidth = 0.5;
    } else if (numPts <= 25) {
        bandwidth = 0.4;
    }

    // console.log("Starting bandwidth: " + bandwidth);
    // console.log("Original points: ");
    // console.log(map);
    const vals = map.map(m => m[1])
    const minVal = Math.min(...vals)
    const maxVal = Math.max(...vals)

    let xval = [],
        yval = [];
    map.forEach(function(item) {
        xval.push(item[0]);
        yval.push(item[1]);
    });

    let loess;
    // Try incrementally increasing bandwidth if an error is thrown or regression doesn't meet standards
    while (bandwidth <= 1.0) {
        loess = science.stats.loess().bandwidth(bandwidth);
        try {
            let loess_data = loess(xval, yval);
            let loess_points = loess_data.map(function(yval, index) {
                return [xval[index], yval];
            });


            // If any points are out of bounds, try increasing bandwidth
            const filtered = loess_points.filter(pt => !isOutOfBounds(pt[1], minVal, maxVal));
            if (filtered.length < loess_points.length) {
                bandwidth = bandwidth + 0.1;
            } else {
                // console.log("Final bandwidth: " + bandwidth);
                // console.log("Regression line: ");
                // console.log(loess_points);
                return filtered;
            }
        } catch (error) {
            bandwidth = bandwidth + 0.1;
        }
    }


    // Fall back on a direct plot
    // console.log("Regression failed. Falling back on direct plot.")
    return map;

}

// Boundaries on the LOESS interpretation
function isOutOfBounds(pt, minVal, maxVal) {
    return isNaN(pt) || pt < minVal || pt > maxVal
}

function getLOESSBandwidth() {
    return Number($('#LOESSBandwidth').html());
}

function getDateRange(data) {
    let minDate = null;
    let maxDate = null;

    data.forEach(function(item) {
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