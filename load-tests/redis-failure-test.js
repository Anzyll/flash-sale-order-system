import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {

        redis_failure_test: {

            executor: 'constant-arrival-rate',

            rate: 200,

            timeUnit: '1s',

            duration: '20s',

            preAllocatedVUs: 100,
            maxVUs: 500,
        },
    },
};

const BASE_URL = 'http://localhost:8000';

export default function () {

    const userId = `user-${__VU}`;

    const payload = JSON.stringify({
        productId: 4
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-USER-ID': userId
        },
    };

    const response = http.post(
        `${BASE_URL}/api/v1/sales/22/purchase`,
        payload,
        params
    );

    check(response, {
        'valid graceful response': (r) =>
            r.status === 500 || r.status === 503,
    });

    sleep(1);
}