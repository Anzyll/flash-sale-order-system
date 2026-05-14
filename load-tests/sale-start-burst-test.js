import http from 'k6/http';
import { check } from 'k6';


export const options = {
    scenarios: {

        sale_start_burst: {

            executor: 'constant-arrival-rate',
            rate: 1000,
            timeUnit: '1s',
            duration: '1m',
            preAllocatedVUs: 500,
            maxVUs: 2000,
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.10'],
        checks: ['rate>0.95'],
    },

    tags: {
        test_type: 'constant_arrival_rate_burst'
    },
};

const BASE_URL = 'http://localhost:8000';

const SALE_ID = 22;
const PRODUCT_ID = 1;

export default function () {

    // unique simulated user
    const userId = `user-${__VU}`;

    const payload = JSON.stringify({
        productId: PRODUCT_ID
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-USER-ID': userId
        },
    };

    const response = http.post(
        `${BASE_URL}/api/v1/sales/${SALE_ID}/purchase`,
        payload,
        params
    );

    check(response, {
        'valid response status': (r) =>
            r.status === 202 ||
            r.status === 400 ||
            r.status === 409,
    });

}