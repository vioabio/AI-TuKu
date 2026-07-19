// 封装全局自定义请求
import { message } from 'ant-design-vue';
import axios from 'axios';

// baseURL 留空，让 Vite proxy 转发 /api 请求至后端（避免跨域问题）
// 生产环境通过 VITE_API_BASE_URL 环境变量覆盖
const myAxios = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 60000,
  withCredentials: true,
});

// Add a request interceptor
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config;
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error);
  }
);

// Add a response interceptor
myAxios.interceptors.response.use(
  function (response) {
    // Any status code that lie within the range of 2xx cause this function to trigger
    // Do something with response data
    const { data } = response;
    if (data.code === 40100) {
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录');
        window.location.href = `/user/login?redirect=${window.location.href}`;
      }
    }
    return response;
  },
  function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    return Promise.reject(error);
  }
);

export default myAxios;
