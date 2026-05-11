import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 5000,
    iterations: 5000,
};

export default function () {

    const payload = JSON.stringify({
        productId: 5
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',

            // unique simulated user
            'X-USER-ID': `user-${__VU}`
        },
    };

    const response = http.post(
        'http://localhost:8000/api/v1/sales/22/purchase',
        payload,
        params
    );

    check(response, {
        'status is 202 or 409': (r) =>
            r.status === 202 || r.status === 409,
    });

    console.log(
        `VU=${__VU}, USER=user-${__VU}, STATUS=${response.status}`
    );
}