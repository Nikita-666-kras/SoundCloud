<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { assetUrl } from '../config';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';
import { useAlbumLikesStore } from '../stores/albumLikes';
import { useTrackFavoritesStore } from '../stores/trackFavorites';
import { useTrackRowActions } from '../composables/useTrackRowActions';
import TrackRowActionsModals from '../components/TrackRowActionsModals.vue';

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
const trackFavorites = useTrackFavoritesStore();

const likedTracks = ref<TrackListItem[]>([]);
const playlists = ref<PlaylistSummary[]>([]);
const loading = ref(true);

const trackActions = useTrackRowActions(likedTracks, { removeOnUnlike: true });

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
    trackFavorites.mergeIds(likedTracks.value.map(t => t.id));
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
                      <div class="track-title-row">
                        <span class="track-number" style="margin-right: 6px">#{{ index + 1 }}</span>
                        <span>{{ track.title }}</span>
                      </div>
                      <div class="track-meta track-artist-line">
                        <span class="track-artist" @click.stop="goToArtist(track.ownerId)">
                          {{ track.ownerUsername }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div v-if="auth.user" class="track-row-actions">
                    <button
                      type="button"
                      class="track-action-icon-btn"
                      :class="{ liked: !!track.likedByMe }"
                      aria-label="Лайк"
                      @click.stop="trackActions.likeTrack(track.id)"
                    >
                      <svg class="track-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                      <span v-if="track.likes > 0" class="track-action-count">{{ track.likes }}</span>
                    </button>
                    <button
                      type="button"
                      class="track-action-icon-btn"
                      aria-label="Ещё"
                      @click.stop="trackActions.openTrackActionsModal(track.id)"
                    >
                      <svg class="track-action-icon" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/></svg>
                    </button>
                  </div>
                </div>
                <div
                  v-if="auth.user && trackActions.reportOpen[track.id]"
                  class="comments-block"
                  style="margin-top: 6px"
                >
                  <div class="section-title">Жалоба на трек</div>
                  <div class="input-group">
                    <select v-model="trackActions.reportReason[track.id]" class="input-control">
                      <option value="Нарушение авторских прав">Нарушение авторских прав</option>
                      <option value="Непристойное содержание">Непристойное содержание</option>
                      <option value="Другое">Другое</option>
                    </select>
                  </div>
                  <div class="input-group">
                    <textarea
                      v-model="trackActions.reportText[track.id]"
                      class="input-control-textarea"
                      rows="3"
                      placeholder="Опиши проблему (опционально)"
                    />
                  </div>
                  <div class="form-footer">
                    <button
                      class="secondary-button"
                      type="button"
                      @click="trackActions.toggleReport(track.id)"
                    >
                      Отмена
                    </button>
                    <button
                      class="primary-button"
                      type="button"
                      :disabled="trackActions.reportSending[track.id]"
                      @click="trackActions.submitReport(track.id)"
                    >
                      {{ trackActions.reportSending[track.id] ? 'Отправляем...' : 'Отправить жалобу' }}
                    </button>
                  </div>
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
                  <img :src="assetUrl(album.coverUrl)" alt="cover" />
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

      <TrackRowActionsModals :api="trackActions" />
    </template>
  </div>
</template>
