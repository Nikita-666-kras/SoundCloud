<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue';
import { api } from '../api';
import { assetUrl } from '../config';
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

const nickQuery = ref('');
const nickSuggestions = ref<ArtistSummary[]>([]);
const nickSearchLoading = ref(false);
const nickDropdownOpen = ref(false);
let nickDebounce: ReturnType<typeof setTimeout> | null = null;

const followLoadingId = ref<string | null>(null);
const nickError = ref('');

async function loadArtists() {
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
}

function isFollowing(id: string) {
  return artists.value.some((a) => a.id === id);
}

function scheduleNickSearch() {
  if (nickDebounce) clearTimeout(nickDebounce);
  const q = nickQuery.value.trim();
  if (q.length < 1) {
    nickSuggestions.value = [];
    nickSearchLoading.value = false;
    return;
  }
  nickSearchLoading.value = true;
  nickDebounce = setTimeout(async () => {
    nickDebounce = null;
    try {
      const res = await api.get<ArtistSummary[]>('/users/search', { params: { q } });
      nickSuggestions.value = Array.isArray(res.data) ? res.data : [];
    } catch (e) {
      console.error(e);
      nickSuggestions.value = [];
    } finally {
      nickSearchLoading.value = false;
    }
  }, 280);
}

function onNickFocus() {
  nickDropdownOpen.value = true;
  if (nickQuery.value.trim().length >= 1) scheduleNickSearch();
}

function onNickInput() {
  nickDropdownOpen.value = true;
  nickError.value = '';
  scheduleNickSearch();
}

function closeNickDropdown() {
  nickDropdownOpen.value = false;
}

function onDocClick(e: MouseEvent) {
  const el = e.target as Node;
  const wrap = document.querySelector('.artist-nick-search-wrap');
  if (wrap && !wrap.contains(el)) closeNickDropdown();
}

function openArtist(id: string) {
  closeNickDropdown();
  router.push(`/artist/${id}`);
}

async function followById(id: string) {
  if (!auth.user || followLoadingId.value) return;
  nickError.value = '';
  followLoadingId.value = id;
  try {
    await api.post(`/users/${id}/follow`);
    await loadArtists();
    nickQuery.value = '';
    nickSuggestions.value = [];
    closeNickDropdown();
  } catch (e: unknown) {
    console.error(e);
    nickError.value = 'Не удалось подписаться (возможно, это ваш аккаунт).';
  } finally {
    followLoadingId.value = null;
  }
}

onMounted(async () => {
  await loadArtists();
  document.addEventListener('click', onDocClick);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick);
  if (nickDebounce) clearTimeout(nickDebounce);
});
</script>

<template>
  <div class="home-layout">
    <section class="card tracks-card">
      <div class="card-header">
        <div>
          <div class="card-title">Любимые артисты</div>
          <!-- <div class="muted">Артисты, на которых ты подписан</div> -->
        </div>
      </div>

      <div v-if="!auth.user" class="muted">
        Чтобы видеть любимых артистов, войди или зарегистрируйся.
      </div>

      <template v-else>
        <div class="input-group artist-nick-block">
          <!-- <label class="input-label" for="artist-nick-input">Найти артиста по нику</label> -->
          <div class="artist-nick-search-wrap">
            <input
              id="artist-nick-input"
              v-model="nickQuery"
              type="search"
              autocomplete="off"
              class="input-control"
              placeholder="Начните вводить ник артиста"
              @focus="onNickFocus"
              @input="onNickInput"
            />
            <div v-if="nickDropdownOpen && nickQuery.trim().length >= 1" class="artist-nick-dropdown" @mousedown.prevent>
              <div v-if="nickSearchLoading" class="artist-nick-dropdown-status muted">Ищем…</div>
              <template v-else-if="!nickSuggestions.length">
                <div class="artist-nick-dropdown-status muted">Никого не найдено</div>
              </template>
              <div
                v-for="a in nickSuggestions"
                :key="a.id"
                class="artist-nick-suggestion"
              >
                <div class="artist-nick-suggestion-main">
                  <div class="avatar-wrapper artist-nick-avatar">
                    <img
                      v-if="a.avatarUrl"
                      :src="assetUrl(`/api/users/${a.id}/avatar`)"
                      alt=""
                      class="avatar-image"
                    />
                  </div>
                  <div class="artist-nick-suggestion-text">
                    <div class="track-title">{{ a.username }}</div>
                    <div v-if="a.bio" class="muted artist-nick-bio">{{ a.bio }}</div>
                  </div>
                </div>
                <div class="artist-nick-suggestion-actions">
                  <button type="button" class="secondary-button artist-nick-mini-btn" @click="openArtist(a.id)">
                    Профиль
                  </button>
                  <button
                    v-if="!isFollowing(a.id)"
                    type="button"
                    class="primary-button artist-nick-mini-btn"
                    :disabled="followLoadingId === a.id"
                    @click="followById(a.id)"
                  >
                    {{ followLoadingId === a.id ? '…' : 'Подписаться' }}
                  </button>
                  <span v-else class="muted artist-nick-subscribed">Уже в списке</span>
                </div>
              </div>
            </div>
          </div>
          <p v-if="nickError" class="muted" style="margin-top: 6px; color: #f97373">{{ nickError }}</p>
        </div>

        <div v-if="loading" class="muted">Загружаем любимых артистов...</div>
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
                  :src="assetUrl(`/api/users/${artist.id}/avatar`)"
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
      </template>
    </section>
  </div>
</template>
