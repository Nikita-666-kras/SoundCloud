<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';

interface PlaylistSummary {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
  trackCount: number;
}

const router = useRouter();
const auth = useAuthStore();
const playlists = ref<PlaylistSummary[]>([]);
const loading = ref(false);
const showCreateForm = ref(false);
const newName = ref('');
const newDescription = ref('');
const createLoading = ref(false);
const createError = ref('');

async function load() {
  if (!auth.user) return;
  loading.value = true;
  try {
    const res = await api.get<PlaylistSummary[]>('/playlists');
    playlists.value = res.data ?? [];
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
}

function openPlaylist(id: string) {
  router.push(`/playlists/${id}`);
}

function openCreateForm() {
  showCreateForm.value = true;
  newName.value = '';
  newDescription.value = '';
  createError.value = '';
}

function closeCreateForm() {
  showCreateForm.value = false;
}

async function createPlaylist() {
  const name = newName.value?.trim();
  if (!name) {
    createError.value = 'Введите название плейлиста.';
    return;
  }
  createLoading.value = true;
  createError.value = '';
  try {
    await api.post<PlaylistSummary>('/playlists', null, {
      params: { name, description: newDescription.value?.trim() || '' }
    });
    closeCreateForm();
    await load();
  } catch (e) {
    console.error(e);
    createError.value = 'Не удалось создать плейлист.';
  } finally {
    createLoading.value = false;
  }
}

onMounted(() => load());
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header" style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px">
        <div>
          <div class="card-title">Плейлисты</div>
          <div class="muted">В том числе «Послушать от …» — репосты от друзей</div>
        </div>
        <button
          v-if="auth.user && !showCreateForm"
          type="button"
          class="primary-button"
          @click="openCreateForm"
        >
          Создать плейлист
        </button>
      </div>

      <div v-if="auth.user && showCreateForm" class="card" style="margin-bottom: 16px; padding: 16px">
        <div class="section-title">Новый плейлист</div>
        <div v-if="createError" class="muted" style="color: #f97373; margin-bottom: 8px">{{ createError }}</div>
        <div class="input-group">
          <label class="input-label">Название</label>
          <input v-model="newName" type="text" class="input-control" placeholder="Название плейлиста" />
        </div>
        <div class="input-group">
          <label class="input-label">Описание (необязательно)</label>
          <textarea
            v-model="newDescription"
            class="input-control-textarea"
            rows="2"
            placeholder="Описание"
          />
        </div>
        <div style="display: flex; gap: 8px; margin-top: 12px">
          <button type="button" class="secondary-button" @click="closeCreateForm">Отмена</button>
          <button type="button" class="primary-button" :disabled="createLoading" @click="createPlaylist">
            {{ createLoading ? 'Создаём...' : 'Создать' }}
          </button>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">
        Чтобы увидеть плейлисты, войди или зарегистрируйся.
      </div>
      <div v-else-if="loading" class="muted">Загружаем плейлисты...</div>
      <div v-else-if="!playlists.length" class="muted">
        У тебя пока нет плейлистов. Репосты от друзей появятся здесь как «Послушать от …».
      </div>
      <div v-else class="tracks-list">
        <div
          v-for="p in playlists"
          :key="p.id"
          class="track-row"
          style="cursor: pointer"
          @click="openPlaylist(p.id)"
        >
          <div class="auth-actions" style="flex: 1">
            <div
              class="album-cover album-cover-placeholder"
              style="width: 48px; height: 48px; border-radius: 10px"
            >
              {{ p.name.charAt(0) }}
            </div>
            <div style="min-width: 0">
              <div class="track-title">{{ p.name }}</div>
              <div class="muted" v-if="p.description">{{ p.description }}</div>
              <div class="muted">{{ p.trackCount }} трек(ов)</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
