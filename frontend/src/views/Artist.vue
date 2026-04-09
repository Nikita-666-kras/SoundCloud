<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api';
import { assetUrl } from '../config';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';

interface Artist {
  id: string;
  email: string;
  username: string;
  bio: string | null;
  avatarUrl: string | null;
  playCount?: number;
}

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const player = usePlayerStore();

const artist = ref<Artist | null>(null);
const tracks = ref<TrackListItem[]>([]);
const loading = ref(true);
const following = ref(false);
const friendRequestSent = ref(false);

const artistId = computed(() => route.params.id as string);

const sortedTracks = computed<TrackListItem[]>(() =>
  [...tracks.value].sort((a, b) => b.playCount - a.playCount)
);

const albums = computed(() => {
  const map = new Map<
    string,
    { name: string; coverUrl: string | null; tracks: TrackListItem[] }
  >();

  for (const track of tracks.value) {
    if (!track.album) continue;
    const key = track.album;
    let entry = map.get(key);
    if (!entry) {
      entry = { name: key, coverUrl: track.coverUrl ?? null, tracks: [] };
      map.set(key, entry);
    }
    if (!entry.coverUrl && track.coverUrl) {
      entry.coverUrl = track.coverUrl;
    }
    entry.tracks.push(track);
  }

  return Array.from(map.values()).sort(
    (a, b) => b.tracks.length - a.tracks.length
  );
});

function albumPlayCount(album: { tracks: TrackListItem[] }) {
  return album.tracks.reduce((sum, t) => sum + (t.playCount ?? 0), 0);
}

async function sendFriendRequest() {
  if (!auth.user || !artist.value || friendRequestSent.value) return;
  try {
    await api.post('/friends/request', null, {
      params: { toId: artist.value.id }
    });
    friendRequestSent.value = true;
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'response' in e) {
      const ax = e as { response?: { status?: number } };
      if (ax.response?.status === 409) friendRequestSent.value = true;
    }
  }
}

onMounted(async () => {
  loading.value = true;
  try {
    const [artistRes, tracksRes] = await Promise.all([
      api.get<Artist>(`/users/${artistId.value}`),
      api.get<TrackListItem[]>(`/users/${artistId.value}/tracks`)
    ]);
    artist.value = artistRes.data;
    tracks.value = tracksRes.data;

    if (auth.user) {
      const [favRes, outgoingRes] = await Promise.all([
        api.get<Artist[]>(`/users/${auth.user.id}/favorite-artists`),
        api.get<{ user: { id: string } }[]>('/friends/me/outgoing')
      ]);
      following.value = favRes.data.some(a => a.id === artistId.value);
      friendRequestSent.value = outgoingRes.data.some(r => r.user.id === artistId.value);
    }
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
});

async function toggleFollow() {
  if (!auth.user || !artist.value) return;
  try {
    if (following.value) {
      await api.delete(`/users/${artistId.value}/follow`);
      following.value = false;
    } else {
      await api.post(`/users/${artistId.value}/follow`);
      following.value = true;
    }
  } catch (e) {
    console.error(e);
  }
}

function goToTrackList() {
  router.push('/');
}

function playTrack(trackId: string) {
  if (!sortedTracks.value.length) return;
  if (player.currentTrackId === trackId) {
    player.togglePlay();
  } else {
    player.setQueueAndPlay(sortedTracks.value, trackId);
  }
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div v-if="loading" class="muted">Загружаем артиста...</div>
      <div v-else-if="!artist" class="muted">Артист не найден.</div>
      <div v-else>
        <div class="card-header">
          <div class="auth-actions">
            <div class="avatar-wrapper" style="width: 56px; height: 56px">
              <img
                v-if="artist.avatarUrl"
                :src="assetUrl(`/api/users/${artist.id}/avatar`)"
                alt="avatar"
                class="avatar-image"
              />
            </div>
            <div>
              <div class="card-title">{{ artist.username }}</div>
              <div class="muted">
                Артист slapshous
                <span v-if="artist.playCount != null && artist.playCount > 0"> · {{ artist.playCount }} прослушиваний</span>
              </div>
            </div>
          </div>
          <div v-if="auth.user" class="auth-actions">
            <button class="secondary-button" @click="toggleFollow">
              {{ following ? 'Отписаться' : 'Подписаться' }}
            </button>
            <button
              class="secondary-button"
              :disabled="friendRequestSent"
              @click="sendFriendRequest"
            >
              {{ friendRequestSent ? 'Заявка отправлена' : 'В друзья' }}
            </button>
          </div>
        </div>

        <div v-if="artist.bio" class="muted" style="margin-bottom: 12px">
          {{ artist.bio }}
        </div>

        <div class="section-title">Популярные треки</div>
        <div v-if="!tracks.length" class="muted">У артиста пока нет треков.</div>
        <div v-else class="tracks-list">
          <div
            v-for="(track, index) in sortedTracks"
            :key="track.id"
            class="track-row"
            :class="{ active: player.currentTrackId === track.id }"
          >
            <div class="track-index">
              <div v-if="track.coverUrl" class="track-cover">
                <img :src="assetUrl(track.coverUrl)" alt="cover" />
              </div>
              <div class="track-number">#{{ index + 1 }}</div>
            </div>
            <div class="track-main" @click="playTrack(track.id)">
              <div class="track-title">{{ track.title }}</div>
              <div class="track-meta">
                <span v-if="track.album">Альбом: {{ track.album }}</span>
                <span v-if="track.genre">{{ track.genre }}</span>
                <span v-if="track.playCount != null && track.playCount > 0">{{ track.playCount }} прослушиваний</span>
              </div>
            </div>
            <div class="track-actions">
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
            </div>
          </div>
        </div>

        <div v-if="albums.length" class="section-title" style="margin-top: 16px">
          Альбомы
        </div>
        <div v-if="albums.length" class="album-carousel">
          <div
            v-for="album in albums"
            :key="album.name"
            class="album-card"
            @click="router.push(`/artist/${artistId}/album/${encodeURIComponent(album.name)}`)"
          >
            <div v-if="album.coverUrl" class="album-cover">
              <img :src="assetUrl(album.coverUrl)" alt="cover" />
            </div>
            <div v-else class="album-cover album-cover-placeholder">
              {{ album.name.charAt(0).toUpperCase() }}
            </div>
            <div class="album-info">
              <div class="album-title">{{ album.name }}</div>
              <div class="album-meta">
                {{ album.tracks.length }} трек(ов)
                <span v-if="albumPlayCount(album) > 0"> · {{ albumPlayCount(album) }} прослушиваний</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

