import { defineStore } from 'pinia';
import { useAuthStore } from './auth';

export interface NotificationItem {
  id: string;
  type: string;
  message: string;
  trackId?: string | null;
  fromUserId?: string | null;
  createdAt: string;
  read: boolean;
}

interface NotificationState {
  items: NotificationItem[];
  unreadCount: number;
  open: boolean;
}

import { api } from '../api';

export const useNotificationStore = defineStore('notifications', {
  state: (): NotificationState => ({
    items: [],
    unreadCount: 0,
    open: false
  }),
  actions: {
    async load() {
      const auth = useAuthStore();
      if (!auth.user) return;
      try {
        const [listRes, countRes] = await Promise.all([
          api.get<NotificationItem[]>('/notifications'),
          api.get<number>('/notifications/count')
        ]);
        this.items = listRes.data;
        this.unreadCount = countRes.data;
      } catch (e) {
        console.error(e);
      }
    },
    toggleOpen() {
      this.open = !this.open;
      if (this.open) this.load();
    },
    close() {
      this.open = false;
    },
    async markRead(id: string) {
      try {
        await api.post(`/notifications/${id}/read`);
        this.items = this.items.map(n =>
          n.id === id ? { ...n, read: true } : n
        );
        this.unreadCount = Math.max(
          0,
          this.items.filter(n => !n.read).length
        );
      } catch (e) {
        console.error(e);
      }
    }
  }
});

