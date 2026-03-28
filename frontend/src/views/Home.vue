<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';
import { useAlbumLikesStore } from '../stores/albumLikes';

interface Comment {
  id: string;
  userId: string;
  username: string;
  text: string;
  createdAt: string;
}

interface FriendSummary {
  id: string;
  username: string;
  bio: string | null;
  avatarUrl: string | null;
}

interface PopularAlbum {
  key: string;
  name: string;
  ownerId: string;
  ownerUsername: string;
  coverUrl: string | null;
  playCount: number;
  likes: number;
}

interface PlaylistSummary {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
  trackCount: number;
}

const router = useRouter();
const auth = useAuthStore();
const player = usePlayerStore();
const albumLikes = useAlbumLikesStore();
const tracks = ref<TrackListItem[]>([]);
const loadingTracks = ref(false);
const page = ref(0);
const pageSize = 20;
const hasMore = ref(true);

const commentsByTrack = ref<Record<string, Comment[]>>({});
const commentsOpen = ref<Record<string, boolean>>({});
const commentsLoading = ref<Record<string, boolean>>({});
const newCommentText = ref<Record<string, string>>({});

const reportOpen = ref<Record<string, boolean>>({});
const trackActionsModalTrackId = ref<string | null>(null);
const reportReason = ref<Record<string, string>>({});
const reportText = ref<Record<string, string>>({});
const reportSending = ref<Record<string, boolean>>({});

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

type HomeTab = 'popular' | 'following' | 'recent';
const homeTab = ref<HomeTab>('popular');

const popularAlbums = computed<PopularAlbum[]>(() => {
  const map = new Map<string, PopularAlbum>();
  for (const t of tracks.value) {
    if (!t.album) continue;
    const key = `${t.ownerId}|${t.album}`;
    const existing = map.get(key);
    if (existing) {
      existing.playCount += t.playCount;
      existing.likes += t.likes;
      if (!existing.coverUrl && t.coverUrl) {
        existing.coverUrl = t.coverUrl;
      }
    } else {
      map.set(key, {
        key,
        name: t.album,
        ownerId: t.ownerId,
        ownerUsername: t.ownerUsername,
        coverUrl: t.coverUrl ?? null,
        playCount: t.playCount,
        likes: t.likes
      });
    }
  }
  return Array.from(map.values()).sort((a, b) => {
    if (b.likes !== a.likes) return b.likes - a.likes;
    return b.playCount - a.playCount;
  }).slice(0, 4);
});

async function loadTracks() {
  if (loadingTracks.value || !hasMore.value) return;
  if (homeTab.value === 'following' && !auth.user) return;
  loadingTracks.value = true;
  try {
    let res: { data: TrackListItem[] };
    if (homeTab.value === 'following') {
      res = await api.get<TrackListItem[]>('/tracks/from-following', {
        params: { page: page.value, size: pageSize }
      });
    } else {
      res = await api.get<TrackListItem[]>('/tracks', {
        params: {
          page: page.value,
          size: pageSize,
          sort: homeTab.value === 'recent' ? 'recent' : 'popular'
        }
      });
    }

    if (page.value === 0) {
      tracks.value = res.data;
    } else {
      tracks.value = [...tracks.value, ...res.data];
    }

    if (!res.data.length || res.data.length < pageSize) {
      hasMore.value = false;
    } else {
      page.value += 1;
    }
  } catch (e) {
    console.error(e);
  } finally {
    loadingTracks.value = false;
  }
}

function setHomeTab(tab: HomeTab) {
  if (homeTab.value === tab) return;
  homeTab.value = tab;
  page.value = 0;
  hasMore.value = true;
  tracks.value = [];
  loadTracks();
}

function handleScroll() {
  if (loadingTracks.value || !hasMore.value) return;
  const scrollPosition = window.innerHeight + window.scrollY;
  const threshold = document.body.offsetHeight - 400;
  if (scrollPosition >= threshold) {
    loadTracks();
  }
}

function playTrack(trackId: string) {
  if (player.currentTrackId === trackId) {
    // тот же трек — просто пауза/продолжение
    player.togglePlay();
  } else {
    // новый трек — ставим очередь и запускаем
    player.setQueueAndPlay(tracks.value, trackId);
  }
}

async function likeTrack(trackId: string) {
  if (!auth.user) {
    return;
  }
  try {
    const res = await api.post<number>(`/tracks/${trackId}/like`);
    tracks.value = tracks.value.map(t =>
      t.id === trackId ? { ...t, likes: res.data } : t
    );
  } catch (e) {
    console.error(e);
  }
}

async function loadComments(trackId: string) {
  if (commentsLoading.value[trackId]) return;
  commentsLoading.value[trackId] = true;
  try {
    const res = await api.get<Comment[]>(`/tracks/${trackId}/comments`);
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
    const res = await api.post<Comment>(`/tracks/${trackId}/comments?${params.toString()}`);
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
  if (id) openRepostDialog(id);
}

function trackActionAddToPlaylist() {
  const id = trackActionsModalTrackId.value;
  closeTrackActionsModal();
  if (id) openAddToPlaylistModal(id);
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

function goToArtist(artistId: string) {
  router.push(`/artist/${artistId}`);
}

function changeVolume(event: Event) {
  player.changeVolume(event);
}

onMounted(() => {
  page.value = 0;
  hasMore.value = true;
  loadTracks();
  window.addEventListener('scroll', handleScroll);
  if (auth.user && (homeTab.value === 'popular' || homeTab.value === 'recent')) {
    albumLikes.loadLiked();
  }
});
watch(
  () => [auth.user, homeTab.value],
  () => {
    if (auth.user && (homeTab.value === 'popular' || homeTab.value === 'recent')) {
      albumLikes.loadLiked();
    }
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <div class="home-layout">
    <div class="home-tabs">
      <button
        type="button"
        class="home-tab"
        :class="{ active: homeTab === 'popular' }"
        @click="setHomeTab('popular')"
      >
        Популярное
      </button>
      <button
        type="button"
        class="home-tab"
        :class="{ active: homeTab === 'following' }"
        @click="setHomeTab('following')"
      >
        Любимые артисты
      </button>
      <button
        type="button"
        class="home-tab"
        :class="{ active: homeTab === 'recent' }"
        @click="setHomeTab('recent')"
      >
        Новое
      </button>
    </div>

    <!-- Альбомы: горизонтальная полоса сверху -->
    <section v-if="(homeTab === 'popular' || homeTab === 'recent') && popularAlbums.length" class="card albums-strip-card">
      <div class="albums-strip-header">
        <div>
          <div class="card-title">{{ homeTab === 'recent' ? 'Новые альбомы' : 'Популярные альбомы' }}</div>
          <div class="muted">Из текущей ленты</div>
        </div>
        <button type="button" class="secondary-button" @click="router.push('/albums')">
          Смотреть больше
        </button>
      </div>
      <div class="albums-strip-scroll">
        <div
          v-for="album in popularAlbums"
          :key="album.key"
          class="albums-strip-item popular-album-card"
          @click="router.push(`/artist/${album.ownerId}/album/${encodeURIComponent(album.name)}`)"
        >
          <button
            v-if="auth.user"
            type="button"
            class="album-like-btn"
            :class="{ liked: albumLikes.isLiked(album.ownerId, album.name) }"
            @click.stop="albumLikes.toggle(album.ownerId, album.name, album.ownerUsername, album.coverUrl)"
            aria-label="Лайк"
          >
            ❤
          </button>
          <div class="popular-album-image" v-if="album.coverUrl">
            <img :src="`http://localhost:8080${album.coverUrl}`" alt="cover" />
          </div>
          <div class="popular-album-overlay">
            <div class="popular-album-title">{{ album.name }}</div>
            <div class="popular-album-meta">
              {{ album.ownerUsername }}
              <span v-if="album.playCount > 0"> · {{ album.playCount }} прослушиваний</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="home-feed-row">
    <section class="card tracks-card">
      <div class="card-header">
        <div>
          <div class="card-title">Лента треков</div>
          <div class="muted">Слушай загруженные треки и ставь лайки</div>
        </div>
        <span class="pill">{{ tracks.length }} трек(ов)</span>
      </div>

      <div v-if="homeTab === 'following' && !auth.user" class="muted">
        Войди, чтобы видеть треки любимых артистов.
      </div>
      <div v-else-if="loadingTracks" class="muted">Загружаем треки...</div>
      <div v-else-if="!tracks.length" class="muted">
        {{ homeTab === 'following' ? 'Подпишись на артистов в разделе «Любимые артисты», чтобы видеть их треки здесь.' : 'Пока нет ни одного трека. Залей первый.' }}
      </div>
      <div v-else class="tracks-list">
        <div
          v-for="(track, index) in tracks"
          :key="track.id"
          class="track-row"
          :class="{ active: player.currentTrackId === track.id }"
        >
          <div v-if="track.coverUrl" class="track-post-cover" @click="playTrack(track.id)">
            <img :src="`http://localhost:8080${track.coverUrl}`" alt="cover" />
          </div>
          <div class="track-row-body-feed">
            <div class="track-row-left">
              <button
                type="button"
                class="track-action-play"
                :aria-label="player.currentTrackId === track.id && player.isPlaying ? 'Пауза' : 'Воспроизвести'"
                @click.stop="playTrack(track.id)"
              >
                <svg
                  v-if="player.currentTrackId === track.id && player.isPlaying"
                  class="track-action-play-svg"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path fill="currentColor" d="M6 5h4v14H6V5zm8 0h4v14h-4V5z" />
                </svg>
                <svg v-else class="track-action-play-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <path fill="currentColor" d="M8 5.14v13.72L19 12 8 5.14z" />
                </svg>
              </button>
              <div class="track-main" @click="playTrack(track.id)">
                <div class="track-title">{{ track.title }}</div>
                <div class="track-meta track-artist-line">
                  <span class="track-artist" @click.stop="goToArtist(track.ownerId)">
                    {{ track.ownerUsername }}
                  </span>
                </div>
              </div>
            </div>
            <div class="track-row-actions">
              <button
                type="button"
                class="track-action-icon-btn"
                :class="{ liked: track.likes > 0 }"
                aria-label="Лайк"
                @click.stop="likeTrack(track.id)"
              >
                <svg class="track-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                <span v-if="track.likes > 0" class="track-action-count">{{ track.likes }}</span>
              </button>
              <button
                type="button"
                class="track-action-icon-btn"
                aria-label="Комментарии"
                @click.stop="toggleComments(track.id)"
              >
                <svg class="track-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              </button>
              <button
                v-if="auth.user"
                type="button"
                class="track-action-icon-btn"
                aria-label="Ещё"
                @click.stop="openTrackActionsModal(track.id)"
              >
                <svg class="track-action-icon" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/></svg>
              </button>
            </div>
          </div>

          <div v-if="commentsOpen[track.id]" class="comments-block">
            <div v-if="commentsLoading[track.id]" class="muted">Загружаем комментарии...</div>
            <div v-else>
              <div v-if="!(commentsByTrack[track.id] && commentsByTrack[track.id].length)" class="muted">
                Пока нет комментариев. Будь первым.
              </div>
              <div v-else class="comments-list">
                <div
                  v-for="comment in commentsByTrack[track.id]"
                  :key="comment.id"
                  class="comment-row"
                >
                  <div class="comment-author">{{ comment.username }}</div>
                  <div class="comment-text">{{ comment.text }}</div>
                </div>
              </div>
            </div>

            <div v-if="auth.user" class="input-group" style="margin-top: 6px">
              <textarea
                v-model="newCommentText[track.id]"
                class="input-control-textarea"
                rows="2"
                placeholder="Напиши комментарий"
              />
              <button
                class="secondary-button"
                type="button"
                style="align-self: flex-end; margin-top: 4px"
                @click="submitComment(track.id)"
              >
                Отправить
              </button>
            </div>
            <div v-else class="muted" style="margin-top: 4px">
              Чтобы комментировать, войди или зарегистрируйся.
            </div>
          </div>

          <div v-if="reportOpen[track.id]" class="comments-block" style="margin-top: 6px">
            <div class="section-title">Жалоба на трек</div>
            <div class="input-group">
              <select
                v-model="reportReason[track.id]"
                class="input-control"
              >
                <option value="Нарушение авторских прав">Нарушение авторских прав</option>
                <option value="Непристойное содержание">Непристойное содержание</option>
                <option value="Другое">Другое</option>
              </select>
            </div>
            <div class="input-group">
              <textarea
                v-model="reportText[track.id]"
                class="input-control-textarea"
                rows="3"
                placeholder="Опиши проблему (опционально)"
              />
            </div>
            <div class="form-footer">
              <button
                class="secondary-button"
                type="button"
                @click="toggleReport(track.id)"
              >
                Отмена
              </button>
              <button
                class="primary-button"
                type="button"
                :disabled="reportSending[track.id]"
                @click="submitReport(track.id)"
              >
                {{ reportSending[track.id] ? 'Отправляем...' : 'Отправить жалобу' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Глобальный плеер теперь в App.vue -->
    </section>
    </div>

    <div
      v-if="auth.user && trackActionsModalTrackId"
      class="modal-overlay"
      @click.self="closeTrackActionsModal"
    >
      <div class="modal-card track-actions-modal-card">
        <div class="modal-header">
          <div class="modal-title">Действия с треком</div>
          <button class="modal-close" type="button" @click="closeTrackActionsModal">×</button>
        </div>
        <div class="track-actions-modal-buttons">
          <button
            type="button"
            class="track-menu-item track-actions-modal-item"
            @click="trackActionRepost"
          >
            Репостнуть другу
          </button>
          <button
            type="button"
            class="track-menu-item track-actions-modal-item"
            @click="trackActionAddToPlaylist"
          >
            Добавить в плейлист
          </button>
          <button
            type="button"
            class="track-menu-item track-actions-modal-item"
            @click="trackActionReport"
          >
            Пожаловаться
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="auth.user && repostDialogTrackId"
      class="modal-overlay"
      @click.self="closeRepostDialog"
    >
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title">Послать трек другу</div>
          <button class="modal-close" type="button" @click="closeRepostDialog">×</button>
        </div>

        <div v-if="repostLoading" class="muted">Загружаем друзей...</div>
        <div v-else>
          <div v-if="repostError" class="muted" style="color: #f97373">
            {{ repostError }}
          </div>
          <div v-else-if="!repostFriends.length" class="muted">
            У тебя пока нет друзей.
          </div>
          <div v-else class="tracks-list">
            <div v-for="f in repostFriends" :key="f.id" class="track-row">
              <div class="auth-actions">
                <div class="avatar-wrapper" style="width: 40px; height: 40px">
                  <img
                    v-if="f.avatarUrl"
                    :src="`http://localhost:8080/api/users/${f.id}/avatar`"
                    alt="avatar"
                    class="avatar-image"
                  />
                </div>
                <div>
                  <div class="track-title">{{ f.username }}</div>
                  <div class="muted" v-if="f.bio">{{ f.bio }}</div>
                </div>
              </div>
              <div class="track-actions">
                <button class="primary-button" type="button" @click="sendRepost(f.id)">
                  Репостнуть
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="secondary-button" type="button" @click="closeRepostDialog">
            Закрыть
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="auth.user && addToPlaylistTrackId"
      class="modal-overlay"
      @click.self="closeAddToPlaylistModal"
    >
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title">Добавить в плейлист</div>
          <button class="modal-close" type="button" @click="closeAddToPlaylistModal">×</button>
        </div>

        <div v-if="addToPlaylistError" class="muted" style="color: #f97373; margin-bottom: 8px">
          {{ addToPlaylistError }}
        </div>

        <div v-if="addToPlaylistCreateMode">
          <div class="input-group">
            <label class="input-label">Название</label>
            <input
              v-model="newPlaylistName"
              type="text"
              class="input-control"
              placeholder="Название плейлиста"
            />
          </div>
          <div class="input-group">
            <label class="input-label">Описание (необязательно)</label>
            <textarea
              v-model="newPlaylistDescription"
              class="input-control-textarea"
              rows="2"
              placeholder="Описание"
            />
          </div>
          <div class="modal-footer" style="margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(148, 163, 184, 0.3)">
            <button
              class="secondary-button"
              type="button"
              @click="addToPlaylistCreateMode = false"
            >
              Отмена
            </button>
            <button
              class="primary-button"
              type="button"
              :disabled="createPlaylistLoading"
              @click="createPlaylistAndAdd"
            >
              {{ createPlaylistLoading ? 'Создаём...' : 'Создать и добавить трек' }}
            </button>
          </div>
        </div>

        <div v-else>
          <div v-if="addToPlaylistLoading" class="muted">Загружаем плейлисты...</div>
          <div v-else>
            <div v-if="!addToPlaylistPlaylists.length" class="muted">
              У тебя пока нет плейлистов. Создай новый ниже.
            </div>
            <div v-else class="add-to-playlist-list">
              <div
                v-for="p in addToPlaylistPlaylists"
                :key="p.id"
                class="add-to-playlist-item"
                @click="addTrackToPlaylist(p.id)"
              >
                <div class="add-to-playlist-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16"/></svg>
                </div>
                <div class="add-to-playlist-text">
                  <div class="track-title">{{ p.name }}</div>
                  <div class="muted" v-if="p.description">{{ p.description }}</div>
                  <div class="muted">{{ p.trackCount }} трек(ов)</div>
                </div>
                <div v-if="addTrackToPlaylistLoading === p.id" class="add-to-playlist-loading muted">...</div>
              </div>
            </div>
            <button
              type="button"
              class="primary-button"
              style="margin-top: 12px"
              @click="showCreatePlaylistForm"
            >
              Создать плейлист
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

