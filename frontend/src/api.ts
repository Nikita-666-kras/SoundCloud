import axios, { type AxiosError } from 'axios';
import { useAuthStore } from './stores/auth';
import { getApiBaseUrl } from './config';

const BASE_URL = getApiBaseUrl();

export const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
});

let isRefreshing = false;
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: AxiosError) => void }> = [];

function processQueue(token: string | null, err: AxiosError | null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (token) resolve(token);
    else reject(err!);
  });
  failedQueue = [];
}

api.interceptors.request.use((config) => {
  const auth = useAuthStore();
  const token = auth.accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as typeof error.config & { _retry?: boolean };
    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }
    const auth = useAuthStore();
    if (!auth.refreshToken) {
      auth.logout();
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({
          resolve: (token: string) => {
            if (originalRequest.headers) originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(api(originalRequest));
          },
          reject
        });
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const res = await axios.post<{ accessToken: string; refreshToken: string; expiresIn: number; user: unknown }>(
        `${BASE_URL}/auth/refresh`,
        { refreshToken: auth.refreshToken },
        { withCredentials: true }
      );
      const { accessToken, refreshToken, user } = res.data;
      auth.setTokens(accessToken, refreshToken);
      auth.setUser(user as Parameters<typeof auth.setUser>[0]);
      processQueue(accessToken, null);
      if (originalRequest.headers) originalRequest.headers.Authorization = `Bearer ${accessToken}`;
      return api(originalRequest);
    } catch (refreshError) {
      processQueue(null, refreshError as AxiosError);
      auth.logout();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);
