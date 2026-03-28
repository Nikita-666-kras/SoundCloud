<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';

const router = useRouter();
const auth = useAuthStore();
const player = usePlayerStore();
const tracks = ref<TrackListItem[]>([]);
const loading = ref(false);

async function loadFavorites() {
  if (!auth.user) return;
  loading.value = true;
  try {
    const res = await api.get<TrackListItem[]>(`/users/${auth.user.id}/favorites`);
    tracks.value = res.data;
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
    player.setQueueAndPlay(tracks.value, trackId);
  }
}

onMounted(() => loadFavorites());

function goToArtist(artistId: string) {
  router.push(`/artist/${artistId}`);
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Понравившиеся</div>
          <div class="muted">Все треки, которые ты лайкнул</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">
        Чтобы увидеть избранное, войди или зарегистрируйся.
      </div>
      <div v-else-if="loading" class="muted">Загружаем избранные треки...</div>
      <div v-else-if="!tracks.length" class="muted">У тебя пока нет избранных треков.</div>
      <div v-else class="tracks-list">
        <div
          v-for="(track, index) in tracks"
          :key="track.id"
          class="track-row"
          :class="{ active: player.currentTrackId === track.id }"
        >
          <div
            v-if="track.coverUrl && player.currentTrackId === track.id"
            class="track-post-cover cover-visible"
            @click="playTrack(track.id)"
          >
            <img :src="`http://localhost:8080${track.coverUrl}`" alt="cover" />
          </div>
          <div class="track-row-body">
            <div class="track-index">
              <div class="track-number">#{{ index + 1 }}</div>
            </div>
            <div class="track-main">
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
      </div>
    </section>
  </div>
</template>
