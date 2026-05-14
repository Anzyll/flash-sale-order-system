import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        purchase_spike: {
            executor: 'shared-iterations',

            vus:6000,
            iterations: 6000,

            maxDuration: '3m'
        },
    },
    thresholds: {
        checks: ['rate>0.95'],
        http_req_duration: ['p(95)<1500'],
    },
    tags: {
        test_type: 'burst_test'
    }
};
export default function () {

    const payload = JSON.stringify({
        productId: 9
    });

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
            r.status === 202 || r.status === 409,
    });
}