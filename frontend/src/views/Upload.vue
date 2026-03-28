<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import { useUploadStore, type AlbumTrackForm } from '../stores/upload';
import { storeToRefs } from 'pinia';

const auth = useAuthStore();
const uploadStore = useUploadStore();

onMounted(() => {
  uploadStore.loadFromStorage();
});

const { title, description, genre, album, mode, albumTracks } = storeToRefs(uploadStore);

const files = ref<File[]>([]);
const cover = ref<File | null>(null);
const uploading = ref(false);
const errorMessage = ref('');

watch(
  () => uploadStore.$state,
  () => {
    uploadStore.saveToStorage();
  },
  { deep: true }
);

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement;
  if (mode.value === 'single') {
    if (target.files && target.files.length) {
      files.value = [target.files[0]];
    } else {
      files.value = [];
    }
  } else {
    // для альбома файлы задаются на каждой строке отдельно
    files.value = [];
  }
}

function handleAlbumTrackFileChange(index: number, e: Event) {
  const target = e.target as HTMLInputElement;
  const track = albumTracks.value[index];
  if (!track) return;
  track.file = target.files && target.files[0] ? target.files[0] : null;
}

function addAlbumTrack() {
  albumTracks.value.push({ title: '', genre: '', features: '', file: null });
}

function removeAlbumTrack(index: number) {
  if (albumTracks.value.length <= 1) return;
  albumTracks.value.splice(index, 1);
}

function handleCoverChange(e: Event) {
  const target = e.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    cover.value = target.files[0];
  }
}

async function handleUpload() {
  if (!auth.user) {
    errorMessage.value = 'Сначала войди или зарегистрируйся — тогда появится твой userId.';
    return;
  }
  if (mode.value === 'album' && !album.value.trim()) {
    errorMessage.value = 'Укажи название альбома или выбери режим одиночного трека.';
    return;
  }
  if (mode.value === 'single') {
    if (!files.value.length) {
      errorMessage.value = 'Выбери аудиофайл.';
      return;
    }
    if (!title.value.trim()) {
      errorMessage.value = 'Укажи название трека.';
      return;
    }
  } else {
    const withFiles = albumTracks.value.filter(t => t.file);
    if (!withFiles.length) {
      errorMessage.value = 'Добавь хотя бы один трек с файлом в альбом.';
      return;
    }
  }
  uploading.value = true;
  errorMessage.value = '';
  try {
    const commonAlbum = mode.value === 'album' && album.value.trim() ? album.value.trim() : '';

    if (mode.value === 'single') {
      const file = files.value[0];
      const form = new FormData();
      form.append('title', title.value.trim());
      if (description.value) form.append('description', description.value);
      if (genre.value) form.append('genre', genre.value);
      if (commonAlbum) form.append('album', commonAlbum);
      form.append('file', file);
      if (cover.value) form.append('cover', cover.value);
      await api.post('/tracks/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
    } else {
      for (const track of albumTracks.value) {
        if (!track.file) continue;
        const form = new FormData();
        const baseTitle =
          track.title.trim() ||
          (track.file.name ? track.file.name.replace(/\.[^/.]+$/, '') : 'Untitled');
        form.append('title', baseTitle);

        let fullDescription = description.value || '';
        if (track.features.trim()) {
          fullDescription += (fullDescription ? '\n' : '') + `Фиты: ${track.features.trim()}`;
        }
        if (fullDescription) {
          form.append('description', fullDescription);
        }

        if (track.genre.trim()) {
          form.append('genre', track.genre.trim());
        }
        if (commonAlbum) form.append('album', commonAlbum);
        form.append('file', track.file);
        if (cover.value) form.append('cover', cover.value);
        await api.post('/tracks/upload', form, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
      }
    }

    uploadStore.resetAfterUpload();
    files.value = [];
    cover.value = null;
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Ошибка при загрузке трека.';
  } finally {
    uploading.value = false;
  }
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Выложить трек</div>
          <div class="muted">Заполни данные релиза и выбери файл(ы)</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted" style="margin-bottom: 12px">
        Чтобы выложить трек, сначала войди или зарегистрируйся.
      </div>

      <div class="auth-tabs">
        <button
          type="button"
          class="auth-tab"
          :class="{ active: mode === 'single' }"
          @click="mode = 'single'"
        >
          дроп трека
        </button>
        <button
          type="button"
          class="auth-tab"
          :class="{ active: mode === 'album' }"
          @click="mode = 'album'"
        >
           релиз Альбома
        </button>
      </div>

      <div class="form-grid" style="margin-top: 4px">
        <div class="input-group">
          <label class="input-label">
            {{ mode === 'single' ? 'Название трека' : 'Название альбома' }}
          </label>
          <input
            v-if="mode === 'single'"
            v-model="title"
            class="input-control"
            type="text"
            placeholder="Например, Paper"
          />
          <input
            v-else
            v-model="album"
            class="input-control"
            type="text"
            placeholder="Например, Utopia"
          />
        </div>

        <div class="input-group">
          <label class="input-label">Жанр(необязательно)</label>
          <input
            v-model="genre"
            class="input-control"
            type="text"
            placeholder="Например, Hip-Hop"
          />
        </div>

        <div class="input-group" style="grid-column: span 2">
          <label class="input-label">Описание релиза</label>
          <textarea
            v-model="description"
            class="input-control-textarea"
            rows="3"
            placeholder="Короткое описание"
          />
        </div>
      </div>

      <div v-if="mode === 'album'" style="margin-top: 12px">
        <div class="section-title">Треки альбома</div>
        <div
          v-for="(track, index) in albumTracks"
          :key="index"
          class="input-group"
          style="padding: 10px 12px; border-radius: 14px; border: 1px solid rgba(148, 163, 184, 0.5); margin-bottom: 8px"
        >
          <div class="form-grid">
            <div class="input-group">
              <label class="input-label">Название трека</label>
              <input
                v-model="track.title"
                class="input-control"
                type="text"
                placeholder="Название трека"
              />
            </div>
            <div class="input-group">
              <label class="input-label">Жанр (необязательно)</label>
              <input
                v-model="track.genre"
                class="input-control"
                type="text"
                placeholder="Например, Hip-Hop"
              />
            </div>
            <div class="input-group" style="grid-column: span 2">
              <label class="input-label">Фиты (необязательно)</label>
              <input
                v-model="track.features"
                class="input-control"
                type="text"
                placeholder="Например, Lil Baby, Drake"
              />
            </div>
            <div class="input-group" style="grid-column: span 2">
              <label class="input-label">Файл трека</label>
              <input
                class="input-control"
                type="file"
                accept="audio/*"
                @change="handleAlbumTrackFileChange(index, $event)"
              />
            </div>
          </div>
          <div class="form-footer" style="margin-top: 4px">
            <span class="muted">Трек {{ index + 1 }}</span>
            <button
              v-if="albumTracks.length > 1"
              type="button"
              class="secondary-button"
              @click="removeAlbumTrack(index)"
            >
              Удалить
            </button>
          </div>
        </div>

        <div class="input-group" style="margin-top: 4px">
          <button type="button" class="secondary-button" @click="addAlbumTrack">
            + Добавить ещё трек
          </button>
        </div>
      </div>

      <div class="form-grid" style="margin-top: 12px">
        <div v-if="mode === 'single'" class="input-group">
          <label class="input-label">Файл трека</label>
          <input
            class="input-control"
            type="file"
            accept="audio/*"
            @change="handleFileChange"
          />
          <div class="muted">Выбери аудиофайл для одиночного трека.</div>
        </div>

        <div class="input-group" :style="mode === 'album' ? 'grid-column: span 2' : ''">
          <label class="input-label">Обложка релиза</label>
          <input class="input-control" type="file" accept="image/*" @change="handleCoverChange" />
          <div class="muted">Обложка для ленты и плеера (необязательно).</div>
        </div>
      </div>

      <div class="form-footer">
        <div class="muted">После загрузки треки появятся в твоём профиле и общей ленте.</div>
        <button class="primary-button" :disabled="uploading" @click="handleUpload">
          {{ uploading ? 'Загружаем...' : 'Выложить' }}
        </button>
      </div>

      <p v-if="errorMessage" class="muted" style="margin-top: 8px; color: #f97373">
        {{ errorMessage }}
      </p>
    </section>
  </div>
</template>

