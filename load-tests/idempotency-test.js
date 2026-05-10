import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 20,
    iterations: 20,
};

export default function () {

    const payload = JSON.stringify({
        productId: 3
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',

            // SAME USER for all requests
            'X-USER-ID': 'user-1'
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
        `STATUS=${response.status}`
    );
}