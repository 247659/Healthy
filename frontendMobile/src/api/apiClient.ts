import axios from 'axios';

export const apiClient = axios.create({
  baseURL: 'http://10.0.2.2:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});
