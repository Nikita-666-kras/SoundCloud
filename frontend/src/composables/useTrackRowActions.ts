import { reactive, ref, type Ref } from 'vue';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';
import { useTrackFavoritesStore } from '../stores/trackFavorites';

export interface FriendSummary {
  id: string;
  username: string;
  bio: string | null;
  avatarUrl: string | null;
}

export interface PlaylistSummary {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
  trackCount: number;
}

export interface TrackComment {
  id: string;
  userId: string;
  username: string;
  text: string;
  createdAt: string;
}

export interface UseTrackRowActionsOptions {
  /** Убрать трек из списка при снятии лайка (избранное) */
  removeOnUnlike?: boolean;
}

export function useTrackRowActions(
  tracks: Ref<TrackListItem[]>,
  options: UseTrackRowActionsOptions = {}
) {
  const auth = useAuthStore();
  const player = usePlayerStore();
  const trackFavorites = useTrackFavoritesStore();
  const { removeOnUnlike = false } = options;

  const reportOpen = ref<Record<string, boolean>>({});
  const trackActionsModalTrackId = ref<string | null>(null);
  const reportReason = ref<Record<string, string>>({});
  const reportText = ref<Record<string, string>>({});
  const reportSending = ref<Record<string, boolean>>({});

  const commentsByTrack = ref<Record<string, TrackComment[]>>({});
  const commentsOpen = ref<Record<string, boolean>>({});
  const commentsLoading = ref<Record<string, boolean>>({});
  const newCommentText = ref<Record<string, string>>({});

  const repostDialogTrackId = ref<string | null>(null);
  const repostFriends = ref<FriendSummary[]>([]);
  const repostLoading = ref(false);
  const repostError = ref('');

  const addToPlaylistTrackId = ref<string | null>(null);
  const addToPlaylistPlaylists = ref<PlaylistSummary[]>([]);
  const addToPlaylistLoading = ref(false);
  const addToPlaylistCreateMode = ref(false);
  const newPlaylistName = ref('');
  const newPlaylistDescription = ref('');
  const addToPlaylistError = ref('');
  const addTrackToPlaylistLoading = ref<string | null>(null);
  const createPlaylistLoading = ref(false);

  function patchTrackInList(trackId: string, patch: Partial<TrackListItem>) {
    tracks.value = tracks.value.map(t => (t.id === trackId ? { ...t, ...patch } : t));
  }

  async function likeTrack(trackId: string) {
    if (!auth.user) return;
    try {
      const res = await api.post<{ likes: number; likedByMe: boolean }>(`/tracks/${trackId}/like`);
      const likes = res.data.likes;
      const likedByMe = Boolean(res.data.likedByMe);
      trackFavorites.setLiked(trackId, likedByMe);
      if (removeOnUnlike && !likedByMe) {
        tracks.value = tracks.value.filter(t => t.id !== trackId);
      } else {
        patchTrackInList(trackId, { likes, likedByMe });
      }
      if (player.currentTrackId === trackId) {
        player.patchTrackInQueue(trackId, { likes, likedByMe });
      }
    } catch (e) {
      console.error(e);
    }
  }

  async function loadComments(trackId: string) {
    if (commentsLoading.value[trackId]) return;
    commentsLoading.value[trackId] = true;
    try {
      const res = await api.get<TrackComment[]>(`/tracks/${trackId}/comments`);
      commentsByTrack.value[trackId] = res.data;
    } catch (e) {
      console.error(e);
    } finally {
      commentsLoading.value[trackId] = false;
    }
  }

  async function toggleComments(trackId: string) {
    const currentlyOpen = !!commentsOpen.value[trackId];
    commentsOpen.value[trackId] = !currentlyOpen;
    if (!currentlyOpen && !commentsByTrack.value[trackId]) {
      await loadComments(trackId);
    }
  }

  async function submitComment(trackId: string) {
    if (!auth.user) return;
    const text = (newCommentText.value[trackId] || '').trim();
    if (!text) return;
    try {
      const params = new URLSearchParams();
      params.append('text', text);
      const res = await api.post<TrackComment>(`/tracks/${trackId}/comments?${params.toString()}`);
      const list = commentsByTrack.value[trackId] || [];
      commentsByTrack.value[trackId] = [...list, res.data];
      newCommentText.value[trackId] = '';
    } catch (e) {
      console.error(e);
    }
  }

  function toggleReport(trackId: string) {
    reportOpen.value[trackId] = !reportOpen.value[trackId];
  }

  async function submitReport(trackId: string) {
    if (!auth.user) return;
    const reason = (reportReason.value[trackId] || '').trim() || 'Другое';
    const details = (reportText.value[trackId] || '').trim();
    reportSending.value[trackId] = true;
    try {
      await api.post(`/tracks/${trackId}/report`, { reason, details });
      reportOpen.value[trackId] = false;
      reportText.value[trackId] = '';
    } catch (e) {
      console.error(e);
    } finally {
      reportSending.value[trackId] = false;
    }
  }

  function openTrackActionsModal(trackId: string) {
    trackActionsModalTrackId.value = trackId;
  }

  function closeTrackActionsModal() {
    trackActionsModalTrackId.value = null;
  }

  function trackActionRepost() {
    const id = trackActionsModalTrackId.value;
    closeTrackActionsModal();
    if (id) void openRepostDialog(id);
  }

  function trackActionAddToPlaylist() {
    const id = trackActionsModalTrackId.value;
    closeTrackActionsModal();
    if (id) void openAddToPlaylistModal(id);
  }

  function trackActionReport() {
    const id = trackActionsModalTrackId.value;
    closeTrackActionsModal();
    if (id) toggleReport(id);
  }

  async function openRepostDialog(trackId: string) {
    if (!auth.user) return;
    repostDialogTrackId.value = trackId;
    repostError.value = '';
    repostFriends.value = [];
    repostLoading.value = true;
    try {
      const res = await api.get<FriendSummary[]>('/friends/me');
      repostFriends.value = Array.isArray(res.data) ? res.data : [];
    } catch (e) {
      console.error(e);
      repostError.value = 'Не удалось загрузить друзей.';
    } finally {
      repostLoading.value = false;
    }
  }

  function closeRepostDialog() {
    repostDialogTrackId.value = null;
  }

  async function openAddToPlaylistModal(trackId: string) {
    addToPlaylistTrackId.value = trackId;
    addToPlaylistCreateMode.value = false;
    newPlaylistName.value = '';
    newPlaylistDescription.value = '';
    addToPlaylistError.value = '';
    addToPlaylistLoading.value = true;
    try {
      const res = await api.get<PlaylistSummary[]>('/playlists');
      addToPlaylistPlaylists.value = res.data ?? [];
    } catch (e) {
      console.error(e);
      addToPlaylistError.value = 'Не удалось загрузить плейлисты.';
      addToPlaylistPlaylists.value = [];
    } finally {
      addToPlaylistLoading.value = false;
    }
  }

  function closeAddToPlaylistModal() {
    addToPlaylistTrackId.value = null;
    addToPlaylistCreateMode.value = false;
    addTrackToPlaylistLoading.value = null;
    createPlaylistLoading.value = false;
  }

  async function addTrackToPlaylist(playlistId: string) {
    if (!addToPlaylistTrackId.value) return;
    addTrackToPlaylistLoading.value = playlistId;
    addToPlaylistError.value = '';
    try {
      await api.post(`/playlists/${playlistId}/tracks`, null, {
        params: { trackId: addToPlaylistTrackId.value }
      });
      closeAddToPlaylistModal();
    } catch (e) {
      console.error(e);
      addToPlaylistError.value = 'Не удалось добавить трек в плейлист.';
    } finally {
      addTrackToPlaylistLoading.value = null;
    }
  }

  function showCreatePlaylistForm() {
    addToPlaylistCreateMode.value = true;
    addToPlaylistError.value = '';
  }

  async function createPlaylistAndAdd() {
    const name = newPlaylistName.value?.trim();
    if (!name) {
      addToPlaylistError.value = 'Введите название плейлиста.';
      return;
    }
    if (!addToPlaylistTrackId.value) return;
    createPlaylistLoading.value = true;
    addToPlaylistError.value = '';
    try {
      const res = await api.post<PlaylistSummary>('/playlists', null, {
        params: { name, description: newPlaylistDescription.value?.trim() || '' }
      });
      const newId = res.data?.id;
      if (newId) {
        await api.post(`/playlists/${newId}/tracks`, null, {
          params: { trackId: addToPlaylistTrackId.value }
        });
      }
      closeAddToPlaylistModal();
    } catch (e) {
      console.error(e);
      addToPlaylistError.value = 'Не удалось создать плейлист.';
    } finally {
      createPlaylistLoading.value = false;
    }
  }

  async function sendRepost(friendId: string) {
    if (!auth.user || !repostDialogTrackId.value) return;
    try {
      await api.post(`/tracks/${repostDialogTrackId.value}/repost`, null, {
        params: { toUserId: friendId }
      });
      closeRepostDialog();
    } catch (e) {
      console.error(e);
      repostError.value = 'Не удалось отправить репост.';
    }
  }

  return reactive({
    reportOpen,
    trackActionsModalTrackId,
    reportReason,
    reportText,
    reportSending,
    commentsByTrack,
    commentsOpen,
    commentsLoading,
    newCommentText,
    repostDialogTrackId,
    repostFriends,
    repostLoading,
    repostError,
    addToPlaylistTrackId,
    addToPlaylistPlaylists,
    addToPlaylistLoading,
    addToPlaylistCreateMode,
    newPlaylistName,
    newPlaylistDescription,
    addToPlaylistError,
    addTrackToPlaylistLoading,
    createPlaylistLoading,
    likeTrack,
    toggleComments,
    submitComment,
    toggleReport,
    submitReport,
    openTrackActionsModal,
    closeTrackActionsModal,
    trackActionRepost,
    trackActionAddToPlaylist,
    trackActionReport,
    openRepostDialog,
    closeRepostDialog,
    openAddToPlaylistModal,
    closeAddToPlaylistModal,
    addTrackToPlaylist,
    showCreatePlaylistForm,
    createPlaylistAndAdd,
    sendRepost
  });
}

export type TrackRowActionsApi = ReturnType<typeof useTrackRowActions>;
