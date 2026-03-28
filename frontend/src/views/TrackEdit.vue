<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';

interface TrackDetails {
  id: string;
  title: string;
  description: string | null;
  genre: string | null;
  album: string | null;
}

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const track = ref<TrackDetails | null>(null);
const loading = ref(true);
const saving = ref(false);
const errorMessage = ref('');

const title = ref('');
const description = ref('');
const genre = ref('');
const album = ref('');

const trackId = route.params.trackId as string;
const artistId = route.params.artistId as string;

onMounted(async () => {
  loading.value = true;
  try {
    // Берем трек из списка артиста и находим по id
    const res = await api.get<any[]>(`/users/${artistId}/tracks`);
    const found = res.data.find(t => t.id === trackId);
    if (!found) {
      errorMessage.value = 'Трек не найден.';
      return;
    }
    track.value = {
      id: found.id,
      title: found.title,
      description: '', // подробного описания в списке нет
      genre: found.genre || null,
      album: found.album || null
    };
    title.value = track.value.title;
    description.value = track.value.description || '';
    genre.value = track.value.genre || '';
    album.value = track.value.album || '';
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось загрузить трек.';
  } finally {
    loading.value = false;
  }
});

async function save() {
  if (!auth.user || !track.value) return;
  saving.value = true;
  errorMessage.value = '';
  try {
    await api.put(`/tracks/${track.value.id}`, {
      title: title.value,
      description: description.value || null,
      genre: genre.value || null,
      album: album.value || null
    });
    router.back();
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось сохранить трек.';
  } finally {
    saving.value = false;
  }
}

async function changeCover(e: Event) {
  if (!auth.user || !track.value) return;
  const target = e.target as HTMLInputElement;
  if (!target.files || !target.files[0]) return;
  try {
    const form = new FormData();
    form.append('file', target.files[0]);
    await api.post(`/tracks/${track.value.id}/cover`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  } catch (err) {
    console.error(err);
    errorMessage.value = 'Не удалось обновить обложку.';
  }
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Редактирование трека</div>
          <div class="muted">Название, жанр, альбом и обложка</div>
        </div>
      </div>

      <div v-if="loading" class="muted">Загружаем трек...</div>
      <div v-else-if="!track" class="muted">Трек не найден.</div>
      <div v-else>
        <div class="form-grid" style="margin-top: 4px">
          <div class="input-group">
            <label class="input-label">Название трека</label>
            <input v-model="title" class="input-control" type="text" />
          </div>
          <div class="input-group">
            <label class="input-label">Жанр</label>
            <input v-model="genre" class="input-control" type="text" />
          </div>
          <div class="input-group" style="grid-column: span 2">
            <label class="input-label">Альбом</label>
            <input v-model="album" class="input-control" type="text" />
          </div>
          <div class="input-group" style="grid-column: span 2">
            <label class="input-label">Описание</label>
            <textarea
              v-model="description"
              class="input-control-textarea"
              rows="3"
              placeholder="Описание трека (опционально)"
            />
          </div>
          <div class="input-group" style="grid-column: span 2">
            <label class="input-label">Обложка</label>
            <input class="input-control" type="file" accept="image/*" @change="changeCover" />
          </div>
        </div>

        <div class="form-footer">
          <div class="muted">Эти изменения увидят все слушатели трека.</div>
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

