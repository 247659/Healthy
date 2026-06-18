import axios from 'axios';

export const apiClient = axios.create({
  baseURL: 'http://localhost:8087/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});
