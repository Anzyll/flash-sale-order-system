import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        retry_storm: {
            executor: 'constant-vus',

            vus: 500,

            duration: '1m',
        },
    },

    thresholds: {
        checks: ['rate>0.95'],
        http_req_duration: ['p(95)<3000'],
    },

    tags: {
        test_type: 'retry_storm_test'
    }
};

export default function () {

    const payload = JSON.stringify({
        productId: 10
    });

    // Same users continuously retrying
    const userId = `user-${__VU}`;

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-USER-ID': userId
        },
    };

    const response = http.post(
        'http://localhost:8000/api/v1/sales/27/purchase',
        payload,
        params
    );

    check(response, {
        'status is valid': (r) =>
            r.status === 202 ||
            r.status === 409 ||
            r.status === 429,
    });

    sleep(0.2);
}