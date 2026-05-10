import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 1000,
    iterations: 1000,
};

export default function () {

    const payload = JSON.stringify({
        productId: 1
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',

            // Simulated unique user
            'X-USER-ID': `user-${__VU}`
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

    console.log(
        `VU=${__VU}, USER=user-${__VU}, STATUS=${response.status}`
    );
}