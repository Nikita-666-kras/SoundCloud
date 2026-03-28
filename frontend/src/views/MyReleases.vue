<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';

const router = useRouter();
const auth = useAuthStore();
const player = usePlayerStore();

const tracks = ref<TrackListItem[]>([]);
const loading = ref(false);
const editingId = ref<string | null>(null);
const editTitle = ref('');
const editDescription = ref('');
const editGenre = ref('');
const editAlbum = ref('');
const saving = ref(false);
const errorMessage = ref('');
const deleteTrackConfirm = ref<string | null>(null);
const deletingTrackId = ref<string | null>(null);
const deleteAlbumConfirm = ref<string | null>(null);
const deletingAlbumName = ref<string | null>(null);

interface AlbumGroup {
  name: string;
  displayName: string;
  tracks: TrackListItem[];
  coverUrl: string | null;
}

const albums = computed<AlbumGroup[]>(() => {
  const map = new Map<string, TrackListItem[]>();
  for (const t of tracks.value) {
    const key = t.album?.trim() ?? '';
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(t);
  }
  return Array.from(map.entries()).map(([name, list]) => ({
    name,
    displayName: name || 'Без альбома',
    tracks: list,
    coverUrl: list[0]?.coverUrl ?? null
  }));
});

const albumsWithName = computed(() => albums.value.filter(a => a.name));

const flatTracks = computed(() => tracks.value);

onMounted(async () => {
  if (!auth.user) return;
  loading.value = true;
  try {
    const res = await api.get<TrackListItem[]>(`/users/${auth.user.id}/tracks`);
    tracks.value = res.data;
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
});

function startEdit(track: TrackListItem) {
  editingId.value = track.id;
  editTitle.value = track.title;
  editGenre.value = track.genre || '';
  editAlbum.value = track.album || '';
  editDescription.value = '';
}

function cancelEdit() {
  editingId.value = null;
  editTitle.value = '';
  editGenre.value = '';
  editAlbum.value = '';
  editDescription.value = '';
}

async function saveEdit() {
  if (!auth.user || !editingId.value) return;
  saving.value = true;
  errorMessage.value = '';
  try {
    const res = await api.put<TrackListItem>(`/tracks/${editingId.value}`, {
      title: editTitle.value,
      description: editDescription.value || null,
      genre: editGenre.value || null,
      album: editAlbum.value || null
    });
    const updated = res.data;
    tracks.value = tracks.value.map(t => (t.id === updated.id ? updated : t));
    cancelEdit();
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось сохранить изменения трека.';
  } finally {
    saving.value = false;
  }
}

async function changeCover(trackId: string, event: Event) {
  if (!auth.user) return;
  const target = event.target as HTMLInputElement;
  if (!target.files || !target.files[0]) return;
  try {
    const form = new FormData();
    form.append('file', target.files[0]);
    await api.post(`/tracks/${trackId}/cover`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    const res = await api.get<TrackListItem[]>(`/users/${auth.user.id}/tracks`);
    tracks.value = res.data;
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось обновить обложку.';
  }
  target.value = '';
}

function playTrack(trackId: string) {
  if (!flatTracks.value.length) return;
  if (player.currentTrackId === trackId) {
    player.togglePlay();
  } else {
    player.setQueueAndPlay(flatTracks.value, trackId);
  }
}

async function deleteTrack(trackId: string) {
  if (!auth.user || deleteTrackConfirm.value !== trackId) return;
  deletingTrackId.value = trackId;
  errorMessage.value = '';
  try {
    await api.delete(`/tracks/${trackId}`);
    tracks.value = tracks.value.filter(t => t.id !== trackId);
    deleteTrackConfirm.value = null;
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось удалить трек.';
  } finally {
    deletingTrackId.value = null;
  }
}

async function deleteAlbum(albumName: string) {
  if (!auth.user || deleteAlbumConfirm.value !== albumName) return;
  deletingAlbumName.value = albumName;
  errorMessage.value = '';
  try {
    await api.delete(`/users/${auth.user.id}/albums/${encodeURIComponent(albumName)}`);
    const key = albumName.trim();
    tracks.value = tracks.value.filter(t => (t.album?.trim() ?? '') !== key);
    deleteAlbumConfirm.value = null;
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось удалить альбом.';
  } finally {
    deletingAlbumName.value = null;
  }
}

function goToAlbumEdit(albumName: string) {
  if (!auth.user) return;
  router.push(`/artist/${auth.user.id}/album/${encodeURIComponent(albumName)}/edit`);
}
</script>

<template>
  <div class="home-layout">
    <div v-if="!auth.user" class="layout-single">
      <section class="card">
        <div class="muted">Чтобы управлять релизами, войди или зарегистрируйся.</div>
      </section>
    </div>

    <template v-else>
      <!-- Мои альбомы: горизонтальная полоса сверху -->
      <section v-if="albumsWithName.length" class="card albums-strip-card my-releases-albums-strip">
        <div class="albums-strip-header">
          <div>
            <div class="card-title">Мои альбомы</div>
            <div class="muted">Редактирование и удаление</div>
          </div>
        </div>
        <div class="albums-strip-scroll">
          <div
            v-for="album in albumsWithName"
            :key="album.name"
            class="albums-strip-item my-releases-album-card-wrap"
          >
            <div
              class="popular-album-card my-releases-album-card"
              @click="goToAlbumEdit(album.name)"
            >
              <div class="popular-album-image" v-if="album.coverUrl">
                <img :src="`http://localhost:8080${album.coverUrl}`" alt="cover" />
              </div>
              <div v-else class="popular-album-image popular-album-placeholder">⌘</div>
              <div class="popular-album-overlay">
                <div class="popular-album-title">{{ album.displayName }}</div>
                <div class="popular-album-meta">{{ album.tracks.length }} трек(ов)</div>
              </div>
            </div>
            <div class="my-releases-album-actions">
              <button type="button" class="secondary-button" @click.stop="goToAlbumEdit(album.name)">
                Редактировать
              </button>
              <template v-if="deleteAlbumConfirm !== album.name">
                <button type="button" class="secondary-button my-releases-delete-btn" @click.stop="deleteAlbumConfirm = album.name">
                  Удалить
                </button>
              </template>
              <template v-else>
                <button type="button" class="secondary-button my-releases-delete-btn" :disabled="!!deletingAlbumName" @click.stop="deleteAlbum(album.name)">
                  {{ deletingAlbumName === album.name ? 'Удаляем...' : 'Да' }}
                </button>
                <button type="button" class="secondary-button" :disabled="!!deletingAlbumName" @click.stop="deleteAlbumConfirm = null">
                  Отмена
                </button>
              </template>
            </div>
          </div>
        </div>
      </section>

      <div class="home-feed-row my-releases-feed">
        <section class="card tracks-card">
          <div class="card-header">
            <div>
              <div class="card-title">Мои треки</div>
              <div class="muted">Редактируй названия, альбомы и обложки</div>
            </div>
            <span v-if="tracks.length" class="pill">{{ tracks.length }} трек(ов)</span>
          </div>

          <div v-if="loading" class="muted">Загружаем твои треки...</div>
          <div v-else-if="!tracks.length" class="muted">У тебя пока нет треков.</div>
          <div v-else class="tracks-list my-releases-tracks-list">
            <div
              v-for="(track, index) in tracks"
              :key="track.id"
              class="track-row my-releases-track-row"
              :class="{ active: player.currentTrackId === track.id }"
            >
              <div
                class="track-post-cover"
                :class="{ 'track-post-cover-placeholder': !track.coverUrl }"
                @click="playTrack(track.id)"
              >
                <img v-if="track.coverUrl" :src="`http://localhost:8080${track.coverUrl}`" alt="cover" />
                <span v-else class="track-post-cover-icon">♪</span>
              </div>
              <div class="track-row-body">
                <div class="track-index">
                  <div class="track-number">#{{ index + 1 }}</div>
                </div>
                <div class="track-main" @click="playTrack(track.id)">
                  <div class="track-title">{{ track.title }}</div>
                  <div class="track-meta">
                    <span v-if="track.genre">{{ track.genre }}</span>
                    <span v-if="track.album">Альбом: {{ track.album }}</span>
                    <span v-if="track.playCount != null && track.playCount > 0">{{ track.playCount }} прослушиваний</span>
                  </div>
                </div>
                <div class="track-actions my-releases-track-actions">
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
                  <button class="secondary-button" type="button" @click.stop="startEdit(track)">
                    Редактировать
                  </button>
                  <div class="cover-upload-wrap">
                    <input
                      type="file"
                      accept="image/*"
                      class="cover-upload-input"
                      @change="changeCover(track.id, $event)"
                    />
                    <button class="secondary-button" type="button" tabindex="-1">Обложка</button>
                  </div>
                  <template v-if="deleteTrackConfirm !== track.id">
                    <button type="button" class="secondary-button my-releases-delete-btn" @click.stop="deleteTrackConfirm = track.id">
                      Удалить
                    </button>
                  </template>
                  <template v-else>
                    <button
                      type="button"
                      class="secondary-button my-releases-delete-btn"
                      :disabled="!!deletingTrackId"
                      @click.stop="deleteTrack(track.id)"
                    >
                      {{ deletingTrackId === track.id ? 'Удаляем...' : 'Да, удалить' }}
                    </button>
                    <button type="button" class="secondary-button" :disabled="!!deletingTrackId" @click.stop="deleteTrackConfirm = null">
                      Отмена
                    </button>
                  </template>
                </div>
              </div>

              <div v-if="editingId === track.id" class="comments-block my-releases-edit-form">
                <div class="form-grid">
                  <div class="input-group">
                    <input v-model="editTitle" class="input-control" type="text" placeholder="Название трека" />
                  </div>
                  <div class="input-group">
                    <input v-model="editGenre" class="input-control" type="text" placeholder="Жанр" />
                  </div>
                  <div class="input-group" style="grid-column: span 2">
                    <input v-model="editAlbum" class="input-control" type="text" placeholder="Альбом" />
                  </div>
                  <div class="input-group" style="grid-column: span 2">
                    <textarea
                      v-model="editDescription"
                      class="input-control-textarea"
                      rows="3"
                      placeholder="Описание (опционально)"
                    />
                  </div>
                </div>
                <div class="form-footer">
                  <button class="secondary-button" type="button" @click="cancelEdit">Отмена</button>
                  <button class="primary-button" type="button" :disabled="saving" @click="saveEdit">
                    {{ saving ? 'Сохраняем...' : 'Сохранить' }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <p v-if="errorMessage" class="muted" style="margin-top: 12px; color: #f97373">{{ errorMessage }}</p>
        </section>
      </div>
    </template>
  </div>
</template>

