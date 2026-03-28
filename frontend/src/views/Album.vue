<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';

interface Artist {
  id: string;
  username: string;
}

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const player = usePlayerStore();

const artist = ref<Artist | null>(null);
const tracks = ref<TrackListItem[]>([]);
const loading = ref(true);

const artistId = computed(() => route.params.artistId as string);
const albumName = computed(() => decodeURIComponent(route.params.albumName as string));
const totalPlayCount = computed(() =>
  tracks.value.reduce((sum, t) => sum + (t.playCount ?? 0), 0)
);
const isOwnAlbum = computed(() => auth.user && artist.value && auth.user.id === artist.value.id);

onMounted(async () => {
  loading.value = true;
  try {
    const [artistRes, tracksRes] = await Promise.all([
      api.get<Artist>(`/users/${artistId.value}`),
      api.get<TrackListItem[]>(`/users/${artistId.value}/albums/${encodeURIComponent(albumName.value)}`)
    ]);
    artist.value = artistRes.data;
    tracks.value = tracksRes.data;
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
});

function playTrack(trackId: string) {
  if (!tracks.value.length) return;
  if (player.currentTrackId === trackId) {
    player.togglePlay();
  } else {
    player.setQueueAndPlay(tracks.value, trackId);
  }
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div v-if="loading" class="muted">Загружаем альбом...</div>
      <div v-else-if="!tracks.length" class="muted">В этом альбоме пока нет треков.</div>
      <div v-else>
        <div class="card-header">
          <div>
            <div class="card-title">
              {{ albumName }}
            </div>
            <div class="muted" v-if="artist">
              Альбом · {{ artist.username }}
              <span v-if="totalPlayCount > 0"> · {{ totalPlayCount }} прослушиваний</span>
            </div>
          </div>
          <button
            v-if="isOwnAlbum"
            class="secondary-button"
            type="button"
            @click="router.push(`/artist/${artistId}/album/${encodeURIComponent(albumName)}/edit`)"
          >
            Редактировать альбом
          </button>
        </div>

        <div class="tracks-list">
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
                <span v-if="track.genre">{{ track.genre }}</span>
                <span v-if="track.playCount != null && track.playCount > 0">{{ track.playCount }} прослушиваний</span>
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

