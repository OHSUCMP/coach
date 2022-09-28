// Adapted by Matthew Storer to consolidate logic and remove module wrappings for basic use
// in plain-vanilla JS environment
// License: http://github.com/OHSUCMP/coach/TODO:ADD ACTUAL PATH HERE AFTER INITIAL COMMIT
// Source: https://github.com/OHSUCMP/coach/TODO:ADD ACTUAL PATH HERE AFTER INITIAL COMMIT
// Adapted from science.js by Jason Davies
// License: https://github.com/jasondavies/science.js/blob/master/LICENSE
// Source: https://github.com/jasondavies/science.js/blob/master/src/stats/loess.js
//
// Based on org.apache.commons.math.analysis.interpolation.LoessInterpolator
// from http://commons.apache.org/math/

var bandwidth = .3,
    robustnessIters = 2,
    accuracy = 1e-12;

    function loess_smooth(xval, yval, p_bandwidth = .3, weights) {
        if (p_bandwidth !== undefined) {
            bandwidth = p_bandwidth;
        }

        let n = xval.length,
            i;

        if (n !== yval.length) throw {error: "Mismatched array lengths"};
        if (n == 0) throw {error: "At least one point required."};

        // if (arguments.length < 3) {
        if (weights === undefined) {
            weights = [];
            i = -1; while (++i < n) weights[i] = 1;
        }

        science_stats_loessFiniteReal(xval);
        science_stats_loessFiniteReal(yval);
        science_stats_loessFiniteReal(weights);
        science_stats_loessStrictlyIncreasing(xval);

        if (n == 1) return [yval[0]];
        if (n == 2) return [yval[0], yval[1]];

        let bandwidthInPoints = Math.floor(bandwidth * n);

        if (bandwidthInPoints < 2) throw {error: "Bandwidth too small."};

        let res = [],
            residuals = [],
            robustnessWeights = [];

        // Do an initial fit and 'robustnessIters' robustness iterations.
        // This is equivalent to doing 'robustnessIters+1' robustness iterations
        // starting with all robustness weights set to 1.
        i = -1; while (++i < n) {
            res[i] = 0;
            residuals[i] = 0;
            robustnessWeights[i] = 1;
        }

        let iter = -1;
        while (++iter <= robustnessIters) {
            let bandwidthInterval = [0, bandwidthInPoints - 1];
            // At each x, compute a local weighted linear regression
            let x;
            i = -1; while (++i < n) {
                x = xval[i];

                // Find out the interval of source points on which
                // a regression is to be made.
                if (i > 0) {
                    science_stats_loessUpdateBandwidthInterval(xval, weights, i, bandwidthInterval);
                }

                let ileft = bandwidthInterval[0],
                    iright = bandwidthInterval[1];

                // Compute the point of the bandwidth interval that is
                // farthest from x
                let edge = (xval[i] - xval[ileft]) > (xval[iright] - xval[i]) ? ileft : iright;

                // Compute a least-squares linear fit weighted by
                // the product of robustness weights and the tricube
                // weight function.
                // See http://en.wikipedia.org/wiki/Linear_regression
                // (section "Univariate linear case")
                // and http://en.wikipedia.org/wiki/Weighted_least_squares
                // (section "Weighted least squares")
                let sumWeights = 0,
                    sumX = 0,
                    sumXSquared = 0,
                    sumY = 0,
                    sumXY = 0,
                    denom = Math.abs(1 / (xval[edge] - x));

                for (let k = ileft; k <= iright; ++k) {
                    let xk   = xval[k],
                        yk   = yval[k],
                        dist = k < i ? x - xk : xk - x,
                        w    = science_stats_loessTricube(dist * denom) * robustnessWeights[k] * weights[k],
                        xkw  = xk * w;
                    sumWeights += w;
                    sumX += xkw;
                    sumXSquared += xk * xkw;
                    sumY += yk * w;
                    sumXY += yk * xkw;
                }

                let meanX = 0,
                    meanY = 0,
                    meanXY = 0,
                    meanXSquared = 0;

                if (sumWeights !== 0) {
                    meanX = sumX / sumWeights;
                    meanY = sumY / sumWeights;
                    meanXY = sumXY / sumWeights;
                    meanXSquared = sumXSquared / sumWeights;
                }

                let beta = (Math.sqrt(Math.abs(meanXSquared - meanX * meanX)) < accuracy)
                    ? 0 : ((meanXY - meanX * meanY) / (meanXSquared - meanX * meanX));

                let alpha = meanY - beta * meanX;

                res[i] = beta * x + alpha;
                residuals[i] = Math.abs(yval[i] - res[i]);
            }

            // No need to recompute the robustness weights at the last
            // iteration, they won't be needed anymore
            if (iter === robustnessIters) {
                break;
            }

            // Recompute the robustness weights.

            // Find the median residual.
            let medianResidual = science_stats_median(residuals);

            if (Math.abs(medianResidual) < accuracy)
                break;

            let arg,
                w;
            i = -1; while (++i < n) {
                arg = residuals[i] / (6 * medianResidual);
                robustnessWeights[i] = (arg >= 1) ? 0 : ((w = 1 - arg * arg) * w);
            }
        }

        return res;
    }

function science_stats_loessFiniteReal(values) {
    let n = values.length,
        i = -1;

    while (++i < n) if (!isFinite(values[i])) return false;

    return true;
}

function science_stats_loessStrictlyIncreasing(xval) {
    let n = xval.length,
        i = 0;

    while (++i < n) if (xval[i - 1] >= xval[i]) return false;

    return true;
}

// Compute the tricube weight function.
// http://en.wikipedia.org/wiki/Local_regression#Weight_function
function science_stats_loessTricube(x) {
    return (x = 1 - x * x * x) * x * x;
}

// Given an index interval into xval that embraces a certain number of
// points closest to xval[i-1], update the interval so that it embraces
// the same number of points closest to xval[i], ignoring zero weights.
function science_stats_loessUpdateBandwidthInterval(xval, weights, i, bandwidthInterval) {
    let left = bandwidthInterval[0],
        right = bandwidthInterval[1];

    // The right edge should be adjusted if the next point to the right
    // is closer to xval[i] than the leftmost point of the current interval
    let nextRight = science_stats_loessNextNonzero(weights, right);
    if ((nextRight < xval.length) && (xval[nextRight] - xval[i]) < (xval[i] - xval[left])) {
        let nextLeft = science_stats_loessNextNonzero(weights, left);
        bandwidthInterval[0] = nextLeft;
        bandwidthInterval[1] = nextRight;
    }
}

function science_stats_loessNextNonzero(weights, i) {
    let j = i + 1;
    while (j < weights.length && weights[j] === 0) j++;
    return j;
}

function science_stats_median(x) {
    return science_stats_quantiles(x, [.5])[0];
}

// Uses R's quantile algorithm type=7.
function science_stats_quantiles(d, quantiles) {
    d = d.slice().sort(function(a, b) {
        return a - b;
    });
    let n_1 = d.length - 1;
    return quantiles.map(function(q) {
        if (q === 0) return d[0];
        else if (q === 1) return d[n_1];

        let index = 1 + q * n_1,
            lo = Math.floor(index),
            h = index - lo,
            a = d[lo - 1];

        return h === 0 ? a : a + h * (d[lo] - a);
    });
}
