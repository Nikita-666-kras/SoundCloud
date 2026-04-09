import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api';

/** ID треков в «Понравившихся» — для плеера, если в очереди нет likedByMe */
export const useTrackFavoritesStore = defineStore('trackFavorites', () => {
  const likedTrackIds = ref<string[]>([]);
  const loaded = ref(false);

  function has(trackId: string | null | undefined) {
    if (!trackId) return false;
    return likedTrackIds.value.includes(trackId);
  }

  function setLiked(trackId: string, liked: boolean) {
    const without = likedTrackIds.value.filter(id => id !== trackId);
    likedTrackIds.value = liked ? [...without, trackId] : without;
  }

  function mergeIds(ids: string[]) {
    if (!ids.length) return;
    const s = new Set(likedTrackIds.value);
    for (const id of ids) s.add(id);
    likedTrackIds.value = [...s];
  }

  function replaceAll(ids: string[]) {
    likedTrackIds.value = [...ids];
  }

  function clear() {
    likedTrackIds.value = [];
    loaded.value = false;
  }

  async function fetchForUser(userId: string) {
    try {
      const res = await api.get<{ id: string }[]>(`/users/${userId}/favorites`);
      const rows = res.data ?? [];
      replaceAll(rows.map(t => String(t.id)));
      loaded.value = true;
    } catch (e) {
      console.error(e);
      replaceAll([]);
      loaded.value = true;
    }
  }

  return {
    likedTrackIds,
    loaded,
    has,
    setLiked,
    mergeIds,
    replaceAll,
    clear,
    fetchForUser,
  };
});
