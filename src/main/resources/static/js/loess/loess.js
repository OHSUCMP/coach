// Adapted from d3-regression by Matthew Storer
// (consolidated logic and removed module wrappings for basic use in plain-vanilla JS environment)
// License: http://github.com/OHSUCMP/coach/TODO:ADD ACTUAL PATH HERE AFTER INITIAL COMMIT
// Source: https://github.com/OHSUCMP/coach/TODO:ADD ACTUAL PATH HERE AFTER INITIAL COMMIT
// Original Source: https://github.com/HarryStevens/d3-regression/blob/766f24bba6ec50a39398aa19df3930f6746793c1/src/loess.js
// Adapted from science.js by Jason Davies
// License: https://github.com/jasondavies/science.js/blob/master/LICENSE
// Source: https://github.com/jasondavies/science.js/blob/master/src/stats/loess.js
// Adapted from vega-statistics by Jeffrey Heer
// License: https://github.com/vega/vega/blob/f058b099decad9db78301405dd0d2e9d8ba3d51a/LICENSE
// Source: https://github.com/vega/vega/blob/f21cb8792b4e0cbe2b1a3fd44b0f5db370dbaadb/packages/vega-statistics/src/regression/loess.js

function loess(data, bandwidth = 0.3) {
    let x = d => d[0],
        y = d => d[1];

    const maxiters = 2, epsilon = 1e-12;

    const [xv, yv, ux, uy] = points(data, x, y, true),
        n = xv.length,
        bw = Math.max(2, ~~(bandwidth * n)), // # nearest neighbors
        yhat = new Float64Array(n),
        residuals = new Float64Array(n),
        robustWeights = new Float64Array(n).fill(1);

    for (let iter = -1; ++iter <= maxiters; ) {
        const interval = [0, bw - 1];

        for (let i = 0; i < n; ++i) {
            const dx = xv[i],
                i0 = interval[0],
                i1 = interval[1],
                edge = (dx - xv[i0]) > (xv[i1] - dx) ? i0 : i1;

            let W = 0, X = 0, Y = 0, XY = 0, X2 = 0,
                denom = 1 / Math.abs(xv[edge] - dx || 1); // Avoid singularity

            for (let k = i0; k <= i1; ++k) {
                const xk = xv[k],
                    yk = yv[k],
                    w = tricube(Math.abs(dx - xk) * denom) * robustWeights[k],
                    xkw = xk * w;

                W += w;
                X += xkw;
                Y += yk * w;
                XY += yk * xkw;
                X2 += xk * xkw;
            }

            // Linear regression fit
            const [a, b] = ols(X / W, Y / W, XY / W, X2 / W);
            yhat[i] = a + b * dx;
            residuals[i] = Math.abs(yv[i] - yhat[i]);

            updateInterval(xv, i + 1, interval);
        }

        if (iter === maxiters) {
            break;
        }

        const medianResidual = median(residuals);
        if (Math.abs(medianResidual) < epsilon) break;

        for (let i = 0, arg, w; i < n; ++i){
            arg = residuals[i] / (6 * medianResidual);
            // Default to epsilon (rather than zero) for large deviations
            // Keeping weights tiny but non-zero prevents singularites
            robustWeights[i] = (arg >= 1) ? epsilon : ((w = 1 - arg * arg) * w);
        }
    }

    return output(xv, yhat, ux, uy);
}

function median(arr) {
    arr.sort((a, b) => a - b);
    let i = arr.length / 2;
    return i % 1 === 0 ? (arr[i - 1] + arr[i]) / 2 : arr[Math.floor(i)];
}

function ols(uX, uY, uXY, uX2) {
    const delta = uX2 - uX * uX,
        slope = Math.abs(delta) < 1e-24 ? 0 : (uXY - uX * uY) / delta,
        intercept = uY - slope * uX;

    return [intercept, slope];
}

function points(data, x, y, sort) {
    data = data.filter((d, i) => {
        let u = x(d, i), v = y(d, i);
        return u != null && isFinite(u) && v != null && isFinite(v);
    });

    if (sort) {
        data.sort((a, b) => x(a) - x(b));
    }

    const n = data.length,
        X = new Float64Array(n),
        Y = new Float64Array(n);

    // extract values, calculate means
    let ux = 0, uy = 0, xv, yv, d;
    for (let i = 0; i < n; ) {
        d = data[i];
        X[i] = xv = +x(d, i, data);
        Y[i] = yv = +y(d, i, data);
        ++i;
        ux += (xv - ux) / i;
        uy += (yv - uy) / i;
    }

    // mean center the data
    for (let i = 0; i < n; ++i) {
        X[i] -= ux;
        Y[i] -= uy;
    }

    return [X, Y, ux, uy];
}

// Weighting kernel for local regression
function tricube(x) {
    return (x = 1 - x * x * x) * x * x;
}

// Advance sliding window interval of nearest neighbors
function updateInterval(xv, i, interval) {
    let val = xv[i],
        left = interval[0],
        right = interval[1] + 1;

    if (right >= xv.length) return;

    // Step right if distance to new right edge is <= distance to old left edge
    // Step when distance is equal to ensure movement over duplicate x values
    while (i > left && (xv[right] - val) <= (val - xv[left])) {
        interval[0] = ++left;
        interval[1] = right;
        ++right;
    }
}

// Generate smoothed output points
// Average points with repeated x values
function output(xv, yhat, ux, uy) {
    const n = xv.length, out = [];
    let i = 0, cnt = 0, prev = [], v;

    for (; i<n; ++i) {
        v = xv[i] + ux;
        if (prev[0] === v) {
            // Average output values via online update
            prev[1] += (yhat[i] - prev[1]) / (++cnt);
        } else {
            // Add new output point
            cnt = 0;
            prev[1] += uy;
            prev = [v, yhat[i]];
            out.push(prev);
        }
    }
    prev[1] += uy;

    return out;
}