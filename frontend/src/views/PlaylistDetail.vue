<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const player = usePlayerStore();

const playlistId = computed(() => route.params.id as string);
const name = ref('');
const description = ref('');
const tracks = ref<TrackListItem[]>([]);
const loading = ref(true);

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
              <img :src="`http://localhost:8080${track.coverUrl}`" alt="cover" />
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
            <button class="primary-button" @click.stop="playTrack(track.id)">
              <span v-if="player.currentTrackId === track.id && player.isPlaying">❚❚</span>
              <span v-else>▶</span>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
