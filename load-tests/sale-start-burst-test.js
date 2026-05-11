import http from 'k6/http';
import { check } from 'k6';


export const options = {
    scenarios: {

        sale_start_burst: {

            executor: 'constant-arrival-rate',

            rate: 1000,

            timeUnit: '1s',

            duration: '20s',

            preAllocatedVUs: 500,
            maxVUs: 2000,
        },
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