<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';

interface ArtistSummary {
  id: string;
  username: string;
  bio: string | null;
  avatarUrl: string | null;
  playCount?: number;
}

const auth = useAuthStore();
const router = useRouter();

const artists = ref<ArtistSummary[]>([]);
const loading = ref(false);

onMounted(async () => {
  if (!auth.user) return;
  loading.value = true;
  try {
    const res = await api.get<ArtistSummary[]>(`/users/${auth.user.id}/favorite-artists`);
    artists.value = res.data;
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
});

function openArtist(id: string) {
  router.push(`/artist/${id}`);
}
</script>

<template>
  <div class="home-layout">
    <section class="card tracks-card">
      <div class="card-header">
        <div>
          <div class="card-title">Любимые артисты</div>
          <div class="muted">Артисты, на которых ты подписан</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">
        Чтобы видеть любимых артистов, войди или зарегистрируйся.
      </div>

      <div v-else-if="loading" class="muted">Загружаем любимых артистов...</div>
      <div v-else-if="!artists.length" class="muted">Ты ещё ни на кого не подписан.</div>
      <div v-else class="tracks-list">
        <div
          v-for="artist in artists"
          :key="artist.id"
          class="track-row"
          @click="openArtist(artist.id)"
        >
          <div class="track-index">
            <div class="avatar-wrapper">
              <img
                v-if="artist.avatarUrl"
                :src="`http://localhost:8080/api/users/${artist.id}/avatar`"
                alt="avatar"
                class="avatar-image"
              />
            </div>
          </div>
          <div class="track-main">
            <div class="track-title">{{ artist.username }}</div>
            <div class="track-meta">
              <span v-if="artist.bio">{{ artist.bio }}</span>
              <span v-if="artist.playCount != null && artist.playCount > 0">{{ artist.playCount }} прослушиваний</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

