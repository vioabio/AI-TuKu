// 封装全局自定义请求
import { message } from 'ant-design-vue';
import axios from 'axios';

const myAxios = axios.create({
  baseURL: 'http://localhost:8123',
//   baseURL:" ",
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
    const {data}=response
    if(data.code===40100){
        if(
            !response.request.responseURL.includes('user/get/login')&&
            !window.location.pathname.includes('/user/login')
        ){
            message.warning('请先登录')
            window.location.href='/user/login?redirect=${window.location.herf}'
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