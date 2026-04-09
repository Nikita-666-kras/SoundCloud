<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api';
import { assetUrl } from '../config';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';
import { useTrackRowActions } from '../composables/useTrackRowActions';
import TrackRowActionsModals from '../components/TrackRowActionsModals.vue';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const player = usePlayerStore();

const playlistId = computed(() => route.params.id as string);
const name = ref('');
const description = ref('');
const tracks = ref<TrackListItem[]>([]);
const loading = ref(true);

const trackActions = useTrackRowActions(tracks, { removeOnUnlike: false });

async function load() {
  loading.value = true;
  try {
    const res = await api.get<{
      id: string;
      name: string;
      description: string | null;
      createdAt: string;
      tracks: TrackListItem[];
    }>(`/playlists/${playlistId.value}`);
    name.value = res.data.name;
    description.value = res.data.description ?? '';
    tracks.value = res.data.tracks ?? [];
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
}

function playTrack(trackId: string) {
  if (tracks.value.length === 0) return;
  if (player.currentTrackId === trackId) {
    player.togglePlay();
  } else {
    player.setQueueAndPlay(tracks.value, trackId);
  }
}

function goToArtist(artistId: string) {
  router.push(`/artist/${artistId}`);
}

const deleteConfirm = ref(false);
const deleting = ref(false);

async function deletePlaylist() {
  if (!deleteConfirm.value) return;
  deleting.value = true;
  try {
    await api.delete(`/playlists/${playlistId.value}`);
    router.push('/playlists');
  } catch (e) {
    console.error(e);
  } finally {
    deleting.value = false;
  }
}

onMounted(() => load());
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">{{ name }}</div>
          <div class="muted" v-if="description">{{ description }}</div>
        </div>
        <div v-if="auth.user">
          <div v-if="!deleteConfirm">
            <button type="button" class="secondary-button" style="border-color: #f87171; color: #dc2626" @click="deleteConfirm = true">
              Удалить плейлист
            </button>
          </div>
          <div v-else>
            <button
              type="button"
              class="secondary-button"
              style="border-color: #f87171; color: #dc2626; margin-right: 8px"
              :disabled="deleting"
              @click="deletePlaylist"
            >
              {{ deleting ? 'Удаляем...' : 'Да, удалить' }}
            </button>
            <button type="button" class="secondary-button" :disabled="deleting" @click="deleteConfirm = false">
              Отмена
            </button>
          </div>
        </div>
      </div>

      <div v-if="loading" class="muted">Загружаем плейлист...</div>
      <div v-else-if="!tracks.length" class="muted">В плейлисте пока нет треков.</div>
      <div v-else class="tracks-list">
        <div
          v-for="(track, index) in tracks"
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
              <span class="track-artist" @click.stop="goToArtist(track.ownerId)">
                {{ track.ownerUsername }}
              </span>
              <span v-if="track.genre">{{ track.genre }}</span>
              <span v-if="track.album">Альбом: {{ track.album }}</span>
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
            <button
              v-if="auth.user"
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
              v-if="auth.user"
              type="button"
              class="track-action-icon-btn"
              aria-label="Ещё"
              @click.stop="trackActions.openTrackActionsModal(track.id)"
            >
              <svg class="track-action-icon" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/></svg>
            </button>
          </div>
          <div
            v-if="auth.user && trackActions.reportOpen[track.id]"
            class="comments-block"
            style="margin-top: 8px; width: 100%"
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
              <button class="secondary-button" type="button" @click="trackActions.toggleReport(track.id)">
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
    </section>

    <TrackRowActionsModals v-if="auth.user" :api="trackActions" />
  </div>
</template>
