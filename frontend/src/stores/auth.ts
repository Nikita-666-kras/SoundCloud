import { defineStore } from 'pinia';

export interface User {
  id: string;
  email: string;
  username: string;
  bio?: string | null;
  avatarUrl?: string | null;
  privateAccount?: boolean | null;
  admin?: boolean | null;
}

const AUTH_USER = 'auth_user';
const AUTH_ACCESS = 'auth_access_token';
const AUTH_REFRESH = 'auth_refresh_token';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    accessToken: null,
    refreshToken: null
  }),
  actions: {
    setUser(user: User | null) {
      this.user = user;
      if (user) localStorage.setItem(AUTH_USER, JSON.stringify(user));
      else localStorage.removeItem(AUTH_USER);
    },
    setTokens(accessToken: string, refreshToken: string) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      localStorage.setItem(AUTH_ACCESS, accessToken);
      localStorage.setItem(AUTH_REFRESH, refreshToken);
    },
    setAuth(accessToken: string, refreshToken: string, user: User) {
      this.setTokens(accessToken, refreshToken);
      this.setUser(user);
    },
    hydrateFromStorage() {
      const rawUser = localStorage.getItem(AUTH_USER);
      const access = localStorage.getItem(AUTH_ACCESS);
      const refresh = localStorage.getItem(AUTH_REFRESH);
      if (rawUser && access && refresh) {
        try {
          this.user = JSON.parse(rawUser) as User;
          this.accessToken = access;
          this.refreshToken = refresh;
        } catch {
          this.clearAuth();
        }
      } else {
        this.user = null;
        this.accessToken = null;
        this.refreshToken = null;
      }
    },
    clearAuth() {
      this.user = null;
      this.accessToken = null;
      this.refreshToken = null;
      localStorage.removeItem(AUTH_USER);
      localStorage.removeItem(AUTH_ACCESS);
      localStorage.removeItem(AUTH_REFRESH);
    },
    async logout() {
      const refresh = this.refreshToken;
      this.clearAuth();
      if (refresh) {
        try {
          await fetch('http://localhost:8080/api/auth/logout', {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: refresh })
          });
        } catch (_) {}
      }
    }
  }
});

