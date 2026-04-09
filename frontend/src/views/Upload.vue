<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';
import { useUploadStore } from '../stores/upload';
import { storeToRefs } from 'pinia';

const auth = useAuthStore();
const uploadStore = useUploadStore();

onMounted(() => {
  uploadStore.loadFromStorage();
});

const { title, description, genre, album, mode, albumTracks } = storeToRefs(uploadStore);

const files = ref<File[]>([]);
const cover = ref<File | null>(null);
const coverPreviewUrl = ref<string | null>(null);
const coverInputKey = ref(0);
const uploading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const albumTracksReadyCount = computed(
  () => albumTracks.value.filter(t => t.file).length
);

const singleFileLabel = computed(() => files.value[0]?.name ?? '');

watch(
  () => uploadStore.$state,
  () => {
    uploadStore.saveToStorage();
  },
  { deep: true }
);

watch(cover, f => {
  if (coverPreviewUrl.value) {
    URL.revokeObjectURL(coverPreviewUrl.value);
    coverPreviewUrl.value = null;
  }
  if (f) coverPreviewUrl.value = URL.createObjectURL(f);
});

onBeforeUnmount(() => {
  if (coverPreviewUrl.value) URL.revokeObjectURL(coverPreviewUrl.value);
});

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement;
  if (mode.value === 'single') {
    if (target.files && target.files.length) {
      files.value = [target.files[0]];
    } else {
      files.value = [];
    }
  } else {
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
  } else {
    cover.value = null;
  }
}

function clearCover() {
  cover.value = null;
  coverInputKey.value += 1;
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

async function handleUpload() {
  if (!auth.user) {
    errorMessage.value = 'Сначала войди или зарегистрируйся — тогда появится твой userId.';
    return;
  }
  if (mode.value === 'album' && !album.value.trim()) {
    errorMessage.value = 'Укажи название альбома.';
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
      errorMessage.value = 'Добавь хотя бы один трек с аудиофайлом.';
      return;
    }
  }
  uploading.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  const wasAlbum = mode.value === 'album';
  let uploadedTracks = 0;
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
      uploadedTracks = 1;
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
        uploadedTracks++;
      }
    }

    uploadStore.resetAfterUpload();
    files.value = [];
    cover.value = null;
    successMessage.value = wasAlbum
      ? `Готово. Загружено треков: ${uploadedTracks}. Они появятся в профиле и ленте.`
      : 'Трек загружен — скоро будет в профиле и ленте.';
    window.setTimeout(() => {
      successMessage.value = '';
    }, 8000);
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Ошибка при загрузке. Проверь формат файла и попробуй снова.';
  } finally {
    uploading.value = false;
  }
}
</script>

<template>
  <div class="layout-single upload-page">
    <section class="card upload-card">
      <div class="upload-hero">
        <div class="upload-hero-text">
          <div class="card-title">Выложить релиз</div>
          <p class="muted upload-hero-sub">
            Один трек или целый альбом — одна обложка на релиз, к каждому треку свой файл.
          </p>
        </div>
      </div>

      <div v-if="!auth.user" class="upload-auth-hint muted">
        Чтобы выложить трек, сначала войди или зарегистрируйся.
      </div>

      <div class="upload-mode-wrap">
        <div class="upload-mode-tabs" role="tablist" aria-label="Тип релиза">
          <button
            type="button"
            class="upload-mode-tab"
            role="tab"
            :class="{ active: mode === 'single' }"
            :aria-selected="mode === 'single'"
            @click="mode = 'single'"
          >
            Один трек
          </button>
          <button
            type="button"
            class="upload-mode-tab"
            role="tab"
            :class="{ active: mode === 'album' }"
            :aria-selected="mode === 'album'"
            @click="mode = 'album'"
          >
            Альбом
          </button>
        </div>
      </div>

      <!-- Общие поля -->
      <div class="upload-section">
        <h2 class="upload-section-title">
          {{ mode === 'single' ? 'Трек' : 'Альбом' }}
        </h2>
        <p v-if="mode === 'album'" class="upload-section-hint muted">
          Сначала укажи название релиза и описание — потом добавь треки ниже.
        </p>
        <div class="form-grid">
          <div class="input-group">
            <label class="input-label" for="upload-main-title">
              {{ mode === 'single' ? 'Название трека' : 'Название альбома' }}
              <span class="input-label-optional" v-if="mode === 'album'">обязательно</span>
            </label>
            <input
              v-if="mode === 'single'"
              id="upload-main-title"
              v-model="title"
              class="input-control"
              type="text"
              placeholder="Например, Midnight Drive"
              autocomplete="off"
            />
            <input
              v-else
              id="upload-main-title"
              v-model="album"
              class="input-control"
              type="text"
              placeholder="Например, Utopia"
              autocomplete="off"
            />
          </div>

          <div class="input-group">
            <label class="input-label" for="upload-genre">
              Жанр
              <span class="input-label-optional">необязательно</span>
            </label>
            <input
              id="upload-genre"
              v-model="genre"
              class="input-control"
              type="text"
              placeholder="Например, Hip-Hop, Electronic"
              autocomplete="off"
            />
          </div>

          <div class="input-group upload-span-2">
            <label class="input-label" for="upload-desc">Описание</label>
            <textarea
              id="upload-desc"
              v-model="description"
              class="input-control-textarea"
              rows="3"
              placeholder="О чём релиз, кто продакшн — по желанию"
            />
          </div>
        </div>
      </div>

      <!-- Альбом: треки -->
      <div v-if="mode === 'album'" class="upload-section upload-section--tracks">
        <div class="upload-section-head">
          <div>
            <h2 class="upload-section-title upload-section-title--inline">Треки альбома</h2>
            <p class="muted upload-section-hint" style="margin-top: 4px; margin-bottom: 0">
              У каждого трека — свой файл. Название можно оставить пустым: возьмём имя файла.
            </p>
          </div>
          <span
            v-if="albumTracksReadyCount > 0"
            class="upload-ready-badge"
          >
            {{ albumTracksReadyCount }} с файлом
          </span>
        </div>

        <div class="upload-track-list">
          <article
            v-for="(track, index) in albumTracks"
            :key="index"
            class="upload-track-card"
          >
            <div class="upload-track-card-top">
              <span class="upload-track-num" aria-hidden="true">{{ index + 1 }}</span>
              <span class="upload-track-label muted">Трек {{ index + 1 }}</span>
              <button
                v-if="albumTracks.length > 1"
                type="button"
                class="upload-track-remove secondary-button"
                @click="removeAlbumTrack(index)"
              >
                Удалить
              </button>
            </div>

            <div class="form-grid upload-track-grid">
              <div class="input-group">
                <label class="input-label">Название</label>
                <input
                  v-model="track.title"
                  class="input-control"
                  type="text"
                  placeholder="Из имени файла, если пусто"
                />
              </div>
              <div class="input-group">
                <label class="input-label">Жанр трека</label>
                <input
                  v-model="track.genre"
                  class="input-control"
                  type="text"
                  placeholder="Или как у альбома"
                />
              </div>
              <div class="input-group upload-span-2">
                <label class="input-label">Фиты</label>
                <input
                  v-model="track.features"
                  class="input-control"
                  type="text"
                  placeholder="Артисты через запятую"
                />
              </div>
              <div class="input-group upload-span-2">
                <label class="input-label">Аудиофайл</label>
                <div class="upload-file-row">
                  <label class="upload-file-btn">
                    <input
                      class="upload-file-input"
                      type="file"
                      accept="audio/*"
                      @change="handleAlbumTrackFileChange(index, $event)"
                    />
                    Выбрать файл
                  </label>
                  <div v-if="track.file" class="upload-file-meta">
                    <span class="upload-file-name">{{ track.file.name }}</span>
                    <span class="muted upload-file-size">{{ formatFileSize(track.file.size) }}</span>
                  </div>
                  <span v-else class="muted upload-file-placeholder">Файл не выбран</span>
                </div>
              </div>
            </div>
          </article>
        </div>

        <button type="button" class="secondary-button upload-add-track" @click="addAlbumTrack">
          + Добавить трек
        </button>
      </div>

      <!-- Один трек: файл -->
      <div v-if="mode === 'single'" class="upload-section">
        <h2 class="upload-section-title">Файл</h2>
        <div class="form-grid">
          <div class="input-group upload-span-2">
            <label class="input-label">Аудио</label>
            <div class="upload-file-row">
              <label class="upload-file-btn">
                <input
                  class="upload-file-input"
                  type="file"
                  accept="audio/*"
                  @change="handleFileChange"
                />
                Выбрать файл
              </label>
              <div v-if="singleFileLabel" class="upload-file-meta">
                <span class="upload-file-name">{{ singleFileLabel }}</span>
                <span v-if="files[0]" class="muted upload-file-size">{{
                  formatFileSize(files[0].size)
                }}</span>
              </div>
              <span v-else class="muted upload-file-placeholder">MP3, WAV, FLAC…</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Обложка -->
      <div class="upload-section">
        <h2 class="upload-section-title">Обложка</h2>
        <p class="muted upload-section-hint">Одна на весь релиз. Квадрат или близко к квадрату — смотрится лучше.</p>
        <div class="upload-cover-block">
          <div
            class="upload-cover-preview"
            :class="{ 'upload-cover-preview--empty': !coverPreviewUrl }"
          >
            <img v-if="coverPreviewUrl" :src="coverPreviewUrl" alt="Превью обложки" />
            <div v-else class="upload-cover-placeholder muted">Нет превью</div>
          </div>
          <div class="upload-cover-actions">
            <label class="upload-file-btn">
              <input
                :key="coverInputKey"
                class="upload-file-input"
                type="file"
                accept="image/*"
                @change="handleCoverChange"
              />
              {{ cover ? 'Заменить' : 'Загрузить обложку' }}
            </label>
            <button
              v-if="cover"
              type="button"
              class="secondary-button upload-cover-clear"
              @click="clearCover"
            >
              Убрать
            </button>
          </div>
        </div>
      </div>

      <div class="upload-footer">
        <p class="muted upload-footer-note">
          После публикации релиз появится в твоём профиле и в общей ленте.
        </p>
        <button
          class="primary-button upload-submit"
          type="button"
          :disabled="uploading || !auth.user"
          @click="handleUpload"
        >
          {{ uploading ? 'Загружаем…' : 'Опубликовать' }}
        </button>
      </div>

      <div v-if="successMessage" class="upload-flash upload-flash--success" role="status">
        {{ successMessage }}
      </div>
      <div v-if="errorMessage" class="upload-flash upload-flash--error" role="alert">
        {{ errorMessage }}
      </div>
    </section>
  </div>
</template>

<style scoped>
.upload-page {
  max-width: 720px;
  margin: 0 auto;
}

.upload-card {
  padding: 22px 22px 24px;
}

.upload-hero-sub {
  margin: 6px 0 0;
  line-height: 1.45;
  max-width: 42em;
}

.upload-auth-hint {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(148, 163, 184, 0.12);
  margin-bottom: 16px;
}

.upload-mode-wrap {
  margin-bottom: 22px;
}

.upload-mode-tabs {
  display: flex;
  width: 100%;
  max-width: 100%;
  padding: 4px;
  border-radius: 14px;
  background: rgba(148, 163, 184, 0.14);
  gap: 4px;
}

.upload-mode-tab {
  flex: 1;
  border: none;
  padding: 10px 14px;
  border-radius: 11px;
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    background 0.15s ease,
    color 0.15s ease,
    box-shadow 0.15s ease;
}

.upload-mode-tab.active {
  background: rgba(255, 255, 255, 0.95);
  color: var(--text-primary);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.1);
}

body.dark-theme .upload-mode-tabs {
  background: rgba(30, 41, 59, 0.85);
}

body.dark-theme .upload-mode-tab.active {
  background: rgba(51, 65, 85, 0.95);
  color: #f1f5f9;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.35);
}

.upload-section {
  margin-bottom: 24px;
}

.upload-section--tracks {
  padding-top: 4px;
}

.upload-section-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 8px;
  color: var(--text-primary);
  letter-spacing: 0.02em;
}

.upload-section-title--inline {
  margin-bottom: 0;
}

.upload-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.upload-section-hint {
  font-size: 13px;
  line-height: 1.4;
  margin-bottom: 12px;
}

.upload-ready-badge {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.18), rgba(99, 102, 241, 0.14));
  color: var(--text-primary);
  border: 1px solid rgba(14, 165, 233, 0.35);
}

.upload-span-2 {
  grid-column: 1 / -1;
}

@media (min-width: 701px) {
  .upload-span-2 {
    grid-column: span 2;
  }
}

.input-label-optional {
  font-weight: 400;
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 6px;
}

.upload-track-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.upload-track-card {
  border-radius: 16px;
  padding: 14px 14px 16px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(248, 250, 252, 0.55);
  transition: border-color 0.15s ease;
}

body.dark-theme .upload-track-card {
  background: rgba(15, 23, 42, 0.45);
  border-color: rgba(71, 85, 105, 0.65);
}

.upload-track-card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.upload-track-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.25), rgba(99, 102, 241, 0.2));
  color: var(--text-primary);
}

.upload-track-label {
  flex: 1;
  font-size: 12px;
}

.upload-track-remove {
  padding: 6px 12px !important;
  font-size: 12px !important;
  min-height: 36px !important;
}

.upload-track-grid {
  gap: 10px 14px;
}

.upload-file-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
  min-height: 40px;
}

.upload-file-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  overflow: hidden;
  background: rgba(14, 165, 233, 0.12);
  border: 1px solid rgba(14, 165, 233, 0.35);
  color: #0369a1;
  transition: background 0.15s ease;
}

body.dark-theme .upload-file-btn {
  color: #7dd3fc;
  background: rgba(56, 189, 248, 0.12);
  border-color: rgba(56, 189, 248, 0.35);
}

.upload-file-btn:hover {
  background: rgba(14, 165, 233, 0.2);
}

.upload-file-input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
  font-size: 0;
}

.upload-file-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.upload-file-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  word-break: break-word;
}

.upload-file-size {
  font-size: 12px;
}

.upload-file-placeholder {
  font-size: 13px;
}

.upload-add-track {
  width: 100%;
  margin-top: 4px;
  padding: 12px 16px !important;
}

.upload-cover-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

@media (min-width: 520px) {
  .upload-cover-block {
    flex-direction: row;
    align-items: flex-start;
  }
}

.upload-cover-preview {
  width: 120px;
  height: 120px;
  border-radius: 14px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid rgba(148, 163, 184, 0.4);
  background: rgba(148, 163, 184, 0.12);
}

.upload-cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-cover-preview--empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-cover-placeholder {
  font-size: 12px;
  text-align: center;
  padding: 8px;
}

.upload-cover-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.upload-cover-clear {
  padding: 8px 14px !important;
  font-size: 13px !important;
}

.upload-footer {
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid rgba(148, 163, 184, 0.25);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

@media (min-width: 560px) {
  .upload-footer {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

.upload-footer-note {
  margin: 0;
  max-width: 36em;
  line-height: 1.45;
}

.upload-submit {
  min-width: 200px;
  padding: 12px 24px !important;
  font-size: 15px !important;
}

.upload-flash {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.45;
}

.upload-flash--success {
  background: rgba(34, 197, 94, 0.12);
  border: 1px solid rgba(34, 197, 94, 0.35);
  color: #166534;
}

body.dark-theme .upload-flash--success {
  color: #86efac;
  background: rgba(34, 197, 94, 0.15);
  border-color: rgba(34, 197, 94, 0.4);
}

.upload-flash--error {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.35);
  color: #b91c1c;
}

body.dark-theme .upload-flash--error {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(248, 113, 113, 0.4);
}
</style>
