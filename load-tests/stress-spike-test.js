import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {

    scenarios: {

        purchase_ramp_test: {

            executor: 'ramping-vus',

            startVUs: 100,

            stages: [

                { duration: '30s', target: 500 },
                { duration: '30s', target: 1000 },
                { duration: '30s', target: 1500 },
                { duration: '30s', target: 2000 },
                { duration: '30s', target: 2500 },
                { duration: '30s', target: 3000 },

                { duration: '30s', target: 3000 },

                { duration: '20s', target: 0 },
            ],

            gracefulRampDown: '10s',
        },
    },

    thresholds: {

        // p95 latency target
        http_req_duration: ['p(95)<=3000'],

        // check success rate
        checks: ['rate>0.95'],
    },

    tags: {
        test_type: 'ramping_vus_test'
    },
};

const BASE_URL = 'http://localhost:8000';

const SALE_ID = 25;
const PRODUCT_ID = 14;

export default function () {

    const payload = JSON.stringify({
        productId: PRODUCT_ID
    });

    // unique simulated user
    const userId = `user-${__VU}`;

    const params = {

        headers: {
            'Content-Type': 'application/json',
            'X-USER-ID': userId
        },

        // fail faster instead of hanging forever
        timeout: '5s',
    };

    const response = http.post(
        `${BASE_URL}/api/v1/sales/${SALE_ID}/purchase`,
        payload,
        params
    );

    check(response, {

        'valid response status': (r) =>
            r.status === 202 ||
            r.status === 409 ||
            r.status === 400,

    });

    // realistic user pacing
    sleep(1);
}