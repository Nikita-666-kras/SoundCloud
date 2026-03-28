import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { api } from '../api';
import { useAuthStore } from './auth';

export interface AlbumSummary {
  ownerId: string;
  ownerUsername: string;
  album: string;
  coverUrl: string | null;
  trackCount: number;
  playCount?: number;
}

function albumKey(ownerId: string, albumName: string) {
  return `${ownerId}|${albumName}`;
}

export const useAlbumLikesStore = defineStore('albumLikes', () => {
  const auth = useAuthStore();
  const likedAlbums = ref<AlbumSummary[]>([]);
  const loaded = ref(false);

  const likedSet = computed(() => {
    const set = new Set<string>();
    for (const a of likedAlbums.value) {
      set.add(albumKey(a.ownerId, a.album));
    }
    return set;
  });

  function isLiked(ownerId: string, albumName: string) {
    return likedSet.value.has(albumKey(ownerId, albumName));
  }

  async function loadLiked() {
    if (!auth.user) {
      likedAlbums.value = [];
      loaded.value = true;
      return;
    }
    try {
      const res = await api.get<AlbumSummary[]>('/albums/liked');
      likedAlbums.value = res.data ?? [];
    } catch (e) {
      console.error(e);
      likedAlbums.value = [];
    } finally {
      loaded.value = true;
    }
  }

  async function like(ownerId: string, albumName: string) {
    if (!auth.user) return;
    try {
      await api.post('/albums/like', null, {
        params: { ownerId, albumName }
      });
      if (!likedSet.value.has(albumKey(ownerId, albumName))) {
        likedAlbums.value = [
          { ownerId, ownerUsername: '', album: albumName, coverUrl: null, trackCount: 0 },
          ...likedAlbums.value
        ];
      }
    } catch (e) {
      console.error(e);
    }
  }

  async function unlike(ownerId: string, albumName: string) {
    if (!auth.user) return;
    try {
      await api.delete('/albums/like', {
        params: { ownerId, albumName }
      });
      likedAlbums.value = likedAlbums.value.filter(
        (a) => a.ownerId !== ownerId || a.album !== albumName
      );
    } catch (e) {
      console.error(e);
    }
  }

  async function toggle(ownerId: string, albumName: string, ownerUsername?: string, coverUrl?: string | null, trackCount?: number) {
    if (isLiked(ownerId, albumName)) {
      await unlike(ownerId, albumName);
    } else {
      await like(ownerId, albumName);
      const idx = likedAlbums.value.findIndex((a) => a.ownerId === ownerId && a.album === albumName);
      if (idx >= 0 && ownerUsername !== undefined) {
        likedAlbums.value[idx] = {
          ...likedAlbums.value[idx],
          ownerUsername: ownerUsername ?? likedAlbums.value[idx].ownerUsername,
          coverUrl: coverUrl ?? likedAlbums.value[idx].coverUrl,
          trackCount: trackCount ?? likedAlbums.value[idx].trackCount
        };
      }
    }
  }

  return {
    likedAlbums,
    likedSet,
    loaded,
    isLiked,
    loadLiked,
    like,
    unlike,
    toggle
  };
});
