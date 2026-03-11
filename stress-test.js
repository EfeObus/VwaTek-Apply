import { check, group, sleep } from 'k6';
import http from 'k6/http';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const checkoutLatency = new Trend('checkout_latency');

// Test configuration
const BASE_URL = 'https://vwatek-apply-production.up.railway.app';

// Test scenarios - ramp up gradually
export const options = {
    stages: [
        { duration: '30s', target: 10 },   // Ramp up to 10 users over 30s
        { duration: '1m', target: 25 },    // Ramp up to 25 users over 1m
        { duration: '2m', target: 50 },    // Stay at 50 users for 2m (stress)
        { duration: '1m', target: 100 },   // Spike to 100 users (peak load)
        { duration: '30s', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],  // 95% of requests under 2s
        http_req_failed: ['rate<0.1'],      // Less than 10% errors
        errors: ['rate<0.1'],
    },
};

// Helper to get auth headers (if you have a test token)
function getAuthHeaders(token) {
    return token ? { 
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    } : {
        'Content-Type': 'application/json'
    };
}

export default function () {
    // Health check (always)
    group('Health Check', () => {
        const healthRes = http.get(`${BASE_URL}/health`);
        check(healthRes, {
            'health status is 200': (r) => r.status === 200,
        }) || errorRate.add(1);
    });

    sleep(0.5);

    // Public endpoints - no auth required
    group('Public API - NOC Codes', () => {
        const nocRes = http.get(`${BASE_URL}/api/v1/noc/categories`);
        check(nocRes, {
            'NOC categories status is 200': (r) => r.status === 200,
            'NOC categories has data': (r) => r.body.length > 0,
        }) || errorRate.add(1);
    });

    sleep(0.3);

    group('Public API - NOC TEER', () => {
        const teerRes = http.get(`${BASE_URL}/api/v1/noc/teer`);
        check(teerRes, {
            'NOC TEER status is 200': (r) => r.status === 200,
        }) || errorRate.add(1);
    });

    sleep(0.3);

    group('Public API - Subscription Pricing', () => {
        const pricingRes = http.get(`${BASE_URL}/api/v1/subscriptions/pricing`);
        check(pricingRes, {
            'pricing status is 200': (r) => r.status === 200,
            'pricing has tiers': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return body.pricing && body.pricing.length > 0;
                } catch {
                    return false;
                }
            },
        }) || errorRate.add(1);
    });

    sleep(0.3);

    // Job Bank search (public)
    group('Public API - Job Bank Search', () => {
        const jobSearchRes = http.get(`${BASE_URL}/api/v1/jobbank/jobs?keyword=software&location=Toronto&page=1`);
        check(jobSearchRes, {
            'job search returns response': (r) => r.status === 200 || r.status === 404,
        }) || errorRate.add(1);
    });

    sleep(0.5);

    // Simulate browsing behavior
    group('Simulated User Journey', () => {
        // User lands on app - health check
        http.get(`${BASE_URL}/health`);
        sleep(0.2);
        
        // User browses pricing
        http.get(`${BASE_URL}/api/v1/subscriptions/pricing`);
        sleep(0.3);
        
        // User searches for NOC codes
        http.get(`${BASE_URL}/api/v1/noc/search?query=software`);
        sleep(0.2);
    });

    // Random delay between iterations (simulates real user behavior)
    sleep(Math.random() * 2 + 1);
}

// Separate scenario for authenticated users (optional)
export function authenticatedUser() {
    // This would require a valid test token
    // const token = __ENV.TEST_AUTH_TOKEN;
    // if (!token) return;
    
    // group('Authenticated - Get Profile', () => {
    //     const res = http.get(`${BASE_URL}/api/v1/profile`, {
    //         headers: getAuthHeaders(token)
    //     });
    //     check(res, {
    //         'profile status is 200': (r) => r.status === 200,
    //     });
    // });
}
