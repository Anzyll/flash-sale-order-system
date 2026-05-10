import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        purchase_spike: {
            executor: 'shared-iterations',

            vus: 9000,
            iterations: 9000,

            maxDuration: '3m',
        },
    },
};

export default function () {

    const payload = JSON.stringify({
        productId: 12
    });

    const userId = `user-${__VU}`;

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-USER-ID': userId
        },
    };

    const response = http.post(
        'http://localhost:8000/api/v1/sales/17/purchase',
        payload,
        params
    );

    check(response, {
        'status is valid': (r) =>
            r.status === 202 || r.status === 409,
    });
}