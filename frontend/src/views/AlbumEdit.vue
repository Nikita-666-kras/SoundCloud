<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import type { TrackListItem } from '../stores/player';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const artistId = route.params.artistId as string;
const albumName = decodeURIComponent(route.params.albumName as string);

const tracks = ref<TrackListItem[]>([]);
const albumTitle = ref(albumName);
const loading = ref(true);
const saving = ref(false);
const errorMessage = ref('');

onMounted(async () => {
  loading.value = true;
  try {
    const res = await api.get<TrackListItem[]>(
      `/users/${artistId}/albums/${encodeURIComponent(albumName)}`
    );
    tracks.value = res.data;
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось загрузить альбом.';
  } finally {
    loading.value = false;
  }
});

async function save() {
  if (!auth.user || !tracks.value.length) return;
  saving.value = true;
  errorMessage.value = '';
  try {
    await Promise.all(
      tracks.value.map(t =>
        api.put(`/tracks/${t.id}`, {
          title: t.title,
          description: null,
          genre: t.genre || null,
          album: albumTitle.value
        })
      )
    );
    router.back();
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось сохранить изменения альбома.';
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Редактирование альбома</div>
          <div class="muted">Название альбома и треки внутри</div>
        </div>
      </div>

      <div v-if="loading" class="muted">Загружаем альбом...</div>
      <div v-else-if="!tracks.length" class="muted">В этом альбоме пока нет треков.</div>
      <div v-else>
        <div class="input-group" style="margin-bottom: 12px">
          <label class="input-label">Название альбома</label>
          <input v-model="albumTitle" class="input-control" type="text" />
        </div>

        <div class="tracks-list">
          <div
            v-for="(track, index) in tracks"
            :key="track.id"
            class="track-row"
          >
            <div class="track-index">
              <div class="track-number">#{{ index + 1 }}</div>
            </div>
            <div class="track-main">
              <div class="form-grid">
                <div class="input-group">
                  <label class="input-label">Название трека</label>
                  <input v-model="track.title" class="input-control" type="text" />
                </div>
                <div class="input-group">
                  <label class="input-label">Жанр</label>
                  <input
                    v-model="track.genre"
                    class="input-control"
                    type="text"
                    placeholder="Жанр (необязательно)"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="form-footer">
          <div class="muted">Все треки альбома будут обновлены.</div>
          <button class="primary-button" type="button" :disabled="saving" @click="save">
            {{ saving ? 'Сохраняем...' : 'Сохранить' }}
          </button>
        </div>

        <p v-if="errorMessage" class="muted" style="margin-top: 8px; color: #f97373">
          {{ errorMessage }}
        </p>
      </div>
    </section>
  </div>
</template>

