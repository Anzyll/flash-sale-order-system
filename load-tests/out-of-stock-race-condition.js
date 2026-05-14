import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 5000,
    iterations: 5000,
};

export default function () {

    const payload = JSON.stringify({
        productId: 2,
        thresholds: {
            http_req_failed: ['rate<0.20'],
            http_req_duration: ['p(95)<2000'],
        },
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-USER-ID': `user-${__VU}`
        },
        timeout: '5s'
    };

    const response = http.post(
        'http://localhost:8000/api/v1/sales/25/purchase',
        payload,
        params
    );

    check(response, {
        'status is 202 or 409': (r) =>
            r.status === 202 || r.status === 409,
    });

}