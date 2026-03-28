<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';
import { useAlbumLikesStore } from '../stores/albumLikes';

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

const likedTracks = ref<TrackListItem[]>([]);
const playlists = ref<PlaylistSummary[]>([]);
const loading = ref(true);

const likedPreview = computed(() => likedTracks.value.slice(0, 3));
const myPlaylists = computed(() =>
  playlists.value.filter((p) => !p.name.startsWith('Послушать от '))
);
const repostPlaylists = computed(() =>
  playlists.value.filter((p) => p.name.startsWith('Послушать от '))
);

async function load() {
  if (!auth.user) {
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    const [tracksRes, playlistsRes] = await Promise.all([
      api.get<TrackListItem[]>(`/users/${auth.user.id}/favorites`),
      api.get<PlaylistSummary[]>('/playlists'),
      albumLikes.loadLiked()
    ]);
    likedTracks.value = tracksRes.data ?? [];
    playlists.value = playlistsRes.data ?? [];
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
}

function playTrack(trackId: string) {
  if (player.currentTrackId === trackId) {
    player.togglePlay();
  } else {
    player.setQueueAndPlay(likedTracks.value, trackId);
  }
}

function goToArtist(artistId: string) {
  router.push(`/artist/${artistId}`);
}

const likedAlbumsPreview = computed(() => albumLikes.likedAlbums.slice(0, 4));

function goToAlbum(ownerId: string, albumName: string) {
  router.push(`/artist/${ownerId}/album/${encodeURIComponent(albumName)}`);
}

onMounted(() => load());
</script>

<template>
  <div class="layout-single">
    <div v-if="!auth.user" class="card" style="padding: 24px">
      <div class="muted">Чтобы увидеть избранное, войди или зарегистрируйся.</div>
    </div>

    <template v-else>
      <div class="favorites-dashboard">
        <div class="favorites-left">
          <section class="card favorites-dashboard-section">
            <div class="card-header">
              <div>
                <div class="card-title">Понравившиеся</div>
                <div class="muted">Треки, которые ты лайкнул</div>
              </div>
              <router-link v-if="likedTracks.length > 3" to="/favorites/tracks" class="primary-button">
                Все
              </router-link>
            </div>
            <div v-if="loading" class="muted">Загружаем...</div>
            <div v-else-if="!likedTracks.length" class="muted">Пока нет понравившихся треков.</div>
            <div v-else class="tracks-list">
              <div
                v-for="(track, index) in likedPreview"
                :key="track.id"
                class="track-row"
                :class="{ active: player.currentTrackId === track.id }"
              >
                <div class="track-index">
                  <div class="track-number">#{{ index + 1 }}</div>
                </div>
                <div class="track-main" @click="playTrack(track.id)">
                  <div class="track-title">{{ track.title }}</div>
                  <div class="track-meta">
                    <span class="track-artist" @click.stop="goToArtist(track.ownerId)">
                      {{ track.ownerUsername }}
                    </span>
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
            <router-link
              v-if="likedTracks.length > 0 && likedTracks.length <= 3"
              to="/favorites/tracks"
              class="secondary-button"
              style="margin-top: 12px; display: inline-block"
            >
              Все
            </router-link>
          </section>

          <section class="card favorites-dashboard-section" v-if="albumLikes.likedAlbums.length > 0">
            <div class="card-header">
              <div>
                <div class="card-title">Понравившиеся альбомы</div>
                <div class="muted">Альбомы, которые ты лайкнул</div>
              </div>
              <router-link to="/albums?liked=1" class="primary-button">Смотреть все</router-link>
            </div>
            <div class="popular-albums-grid favorites-liked-albums">
              <div
                v-for="album in likedAlbumsPreview"
                :key="`${album.ownerId}-${album.album}`"
                class="popular-album-card"
                @click="goToAlbum(album.ownerId, album.album)"
              >
                <button
                  type="button"
                  class="album-like-btn liked"
                  @click.stop="albumLikes.toggle(album.ownerId, album.album, album.ownerUsername, album.coverUrl, album.trackCount)"
                  aria-label="Убрать лайк"
                >
                  ❤
                </button>
                <div class="popular-album-image" v-if="album.coverUrl">
                  <img :src="`http://localhost:8080${album.coverUrl}`" alt="cover" />
                </div>
                <div class="popular-album-overlay">
                  <div class="popular-album-title">{{ album.album }}</div>
                  <div class="popular-album-meta">
                    {{ album.ownerUsername }} · {{ album.trackCount }} трек(ов)
                    <span v-if="album.playCount != null && album.playCount > 0"> · {{ album.playCount }} прослушиваний</span>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>

        <div class="favorites-right">
          <section class="card favorites-dashboard-section">
            <div class="card-header">
              <div>
                <div class="card-title">Мои плейлисты</div>
                <div class="muted">Плейлисты, созданные тобой</div>
              </div>
              <router-link to="/playlists" class="primary-button">Перейти</router-link>
            </div>
            <div v-if="loading" class="muted">Загружаем...</div>
            <div v-else-if="!myPlaylists.length" class="muted">Пока нет своих плейлистов.</div>
            <ul v-else class="favorites-playlist-list">
              <li
                v-for="p in myPlaylists.slice(0, 5)"
                :key="p.id"
                class="favorites-playlist-item"
                @click="router.push(`/playlists/${p.id}`)"
              >
                <span class="track-title">{{ p.name }}</span>
                <span class="muted">{{ p.trackCount }} трек(ов)</span>
              </li>
            </ul>
          </section>

          <section class="card favorites-dashboard-section">
            <div class="card-header">
              <div>
                <div class="card-title">Репосты</div>
                <div class="muted">Треки, которые прислали друзья</div>
              </div>
              <router-link
                v-if="repostPlaylists.length > 3"
                to="/playlists"
                class="primary-button"
              >
                Все
              </router-link>
            </div>
            <div v-if="loading" class="muted">Загружаем...</div>
            <div v-else-if="!repostPlaylists.length" class="muted">Пока нет репостов от друзей.</div>
            <ul v-else class="favorites-playlist-list">
              <li
                v-for="p in repostPlaylists.slice(0, 5)"
                :key="p.id"
                class="favorites-playlist-item"
                @click="router.push(`/playlists/${p.id}`)"
              >
                <span class="track-title">{{ p.name }}</span>
                <span class="muted">{{ p.trackCount }} трек(ов)</span>
              </li>
            </ul>
          </section>
        </div>
      </div>
    </template>
  </div>
</template>
