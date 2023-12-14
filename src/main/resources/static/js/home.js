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

            bpdata.sort(function(a, b) {
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
        data.forEach(function(m) {
            arr.push('<span class="adverseEvent" data-system="' + m.system + '" data-code="' + m.code + '">' +
                m.description +
                '</span>');
        });

        let html = 'Your Adverse Events: ' + arr.join(', ');

        $('#adverseEvents').html(html);
    }
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
        twoLowCrisisBPs = lowCrisisBP && nextMostRecentBP !== undefined && isLowCrisisBP(nextMostRecentBP);
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
        bpIcon.html("<img src='/images/critical-icon.png' class='bp-icon' alt='Critical' />");
        bpIcon.show();
        bpIndicatorContainer.removeClass("col-md-3");
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
        bpIndicatorContainer.removeClass("col-md-3");
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
            bpIcon.html("<img src='/images/stoplight-yellow.png' class='bp-icon' alt='Above Goal' />");
            bpNote.html('Your BP is above your goal!');
        } else {
            bpIcon.html("<img src='/images/stoplight-green.png' class='bp-icon' alt='At or Below Goal' />");
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