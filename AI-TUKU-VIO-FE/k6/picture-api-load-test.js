import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const pictureListDuration = new Trend('picture_list_duration');

export const options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '3m', target: 200 },
    { duration: '5m', target: 500 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<2000'],
    errors: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  group('图片列表查询', () => {
    const page = Math.floor(Math.random() * 5) + 1;
    const res = http.get(`${BASE_URL}/api/picture/list/page/vo?current=${page}&size=20`);
    const ok = check(res, {
      'status is 200': (r) => r.status === 200,
      'response time < 300ms': (r) => r.timings.duration < 300,
    });
    errorRate.add(!ok);
    pictureListDuration.add(res.timings.duration);
    sleep(1);
  });

  group('健康检查', () => {
    const res = http.get(`${BASE_URL}/api/health`);
    check(res, { 'health check ok': (r) => r.status === 200 });
    sleep(5);
  });
}
