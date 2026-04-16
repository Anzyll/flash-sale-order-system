import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {

};

const BASE_URL = 'http://localhost:8000';
const TOKEN = 'eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJjRVZFNTdMNW13Y3RBa0ttZUk2dU40dmNiQ3otb1E1c2U4ZndOblAxeXdRIn0.eyJleHAiOjE3NzU5OTEyMzAsImlhdCI6MTc3NTk5MDkzMCwianRpIjoib25ydHJvOmZjNzRhZGYzLTIyNGUtNDJmMy1mNWU0LWZlZGFmODcxY2U3OCIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MS9yZWFsbXMvZmxhc2gtc2FsZSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI2NTFiYmRiOS1iZDk5LTRkY2UtODAyMC1lNGVlZjZkZWJjZWQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJmbGFzaC1zYWxlLWNsaWVudCIsInNpZCI6IlBRMkg4MEFCVFdHeUhKVFAtWVE4ZndjdCIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iLCJVU0VSIiwiZGVmYXVsdC1yb2xlcy1mbGFzaC1zYWxlIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJlbWFpbCBwcm9maWxlIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInByZWZlcnJlZF91c2VybmFtZSI6ImFuemlsIiwiZW1haWwiOiJhbnppbEBnbWFpbC5jb20ifQ.MI54p9rpbb4r5Jyq6FUyISIGqkJUknM2OtvOiMtzsIHosq820Vs0dfOqZodjkToadOpN5ArZY2xH-tQMZ_he_ePNjUaWbC3wrd9L8zfhDagHWpJNi_OCl7zPse0c4LyUfKx2YIs8tnwa2HEPvKEpTO78HFS2yKa93YH_zp7UJsDxjAL5_bnbLoxbwOTj-YRI4KxvokRETM0w84xuzfx5xQRcOIM-QsV1n6280rPjcwmNLr0hczQefO1F1IzCM3Ha_VH5J_U0CSjsT6UCb5kivvzs5Nzbfl_GFCZOJJUZ1g95dmn00t46sJpexj4lsSByjT4yCyLyPpdG0iwTMPwBcw'; // single token

export default function () {

    const url = `${BASE_URL}/api/v1/sales/3/purchase`;

    const payload = JSON.stringify({
        productId: 1,
        userId: __VU   // 🔥 KEY: each VU = unique user
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${TOKEN}`,
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'status is 200/201': (r) => r.status === 200 || r.status === 201,
        'no server error': (r) => r.status < 500,
    });

    sleep(1);
}