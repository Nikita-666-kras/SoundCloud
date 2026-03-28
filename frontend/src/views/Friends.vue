<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';

interface FriendSummary {
  id: string;
  username: string;
  bio: string | null;
  avatarUrl: string | null;
}

interface FriendRequestSummary {
  id: string;
  user: FriendSummary;
  createdAt: string;
}

const auth = useAuthStore();

const activeTab = ref<'friends' | 'incoming' | 'outgoing'>('friends');
const friends = ref<FriendSummary[]>([]);
const incoming = ref<FriendRequestSummary[]>([]);
const outgoing = ref<FriendRequestSummary[]>([]);
const loading = ref(false);
const errorMessage = ref('');

const searchQuery = ref('');
const searchResults = ref<FriendSummary[]>([]);
const searchLoading = ref(false);
const searchError = ref('');

const outgoingUserIds = computed(() => new Set(outgoing.value.map((o) => o.user.id)));

async function loadAll() {
  if (!auth.user) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    const [friendsRes, incomingRes, outgoingRes] = await Promise.all([
      api.get<FriendSummary[]>('/friends/me'),
      api.get<FriendRequestSummary[]>('/friends/me/incoming'),
      api.get<FriendRequestSummary[]>('/friends/me/outgoing')
    ]);
    friends.value = friendsRes.data;
    incoming.value = incomingRes.data;
    outgoing.value = outgoingRes.data;
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось загрузить друзей и заявки.';
  } finally {
    loading.value = false;
  }
}

async function searchUsers() {
  if (!searchQuery.value.trim()) {
    searchResults.value = [];
    return;
  }
  searchLoading.value = true;
  searchError.value = '';
  try {
    const res = await api.get<FriendSummary[]>('/friends/search', {
      params: { q: searchQuery.value.trim() }
    });
    searchResults.value = res.data;
  } catch (e) {
    console.error(e);
    searchError.value = 'Не удалось выполнить поиск.';
  } finally {
    searchLoading.value = false;
  }
}

async function sendFriendRequest(userId: string) {
  if (!auth.user) return;
  searchError.value = '';
  try {
    await api.post('/friends/request', null, { params: { toId: userId } });
    await loadAll();
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'response' in e) {
      const ax = e as { response?: { status?: number; data?: unknown } };
      if (ax.response?.status === 409) {
        searchError.value = 'Заявка уже отправлена этому пользователю.';
        await loadAll();
        return;
      }
    }
    searchError.value = 'Не удалось отправить заявку.';
  }
}

async function acceptRequest(id: string) {
  if (!auth.user) return;
  try {
    await api.post(`/friends/requests/${id}/accept`);
    await loadAll();
  } catch (e) {
    console.error(e);
  }
}

async function rejectRequest(id: string) {
  if (!auth.user) return;
  try {
    await api.post(`/friends/requests/${id}/reject`);
    await loadAll();
  } catch (e) {
    console.error(e);
  }
}

async function removeFriend(friendId: string) {
  if (!auth.user) return;
  try {
    await api.delete(`/friends/me/${friendId}`);
    await loadAll();
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось удалить из друзей.';
  }
}

onMounted(() => {
  loadAll();
});
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Друзья</div>
          <div class="muted">Друзья и заявки в друзья</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">
        Чтобы использовать друзей, войди или зарегистрируйся.
      </div>

      <div v-else>
        <div class="input-group friends-search-row" style="margin-bottom: 16px">
          <div class="friends-search-wrap">
            <svg class="friends-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
            <input
              v-model="searchQuery"
              @keyup.enter="searchUsers"
              type="text"
              class="input-control friends-search-input"
              placeholder="Найти пользователя по нику"
            />
          </div>
          <button class="secondary-button" type="button" @click="searchUsers">
            Найти
          </button>
        </div>
        <div v-if="searchLoading" class="muted">Ищем пользователей...</div>
        <div v-else-if="searchResults.length" class="tracks-list" style="margin-bottom: 16px">
          <div v-for="u in searchResults" :key="u.id" class="track-row">
            <div class="auth-actions">
              <div class="avatar-wrapper" style="width: 40px; height: 40px">
                <img
                  v-if="u.avatarUrl"
                  :src="`http://localhost:8080/api/users/${u.id}/avatar`"
                  alt="avatar"
                  class="avatar-image"
                />
              </div>
              <div>
                <div class="track-title">{{ u.username }}</div>
                <div class="muted" v-if="u.bio">{{ u.bio }}</div>
              </div>
            </div>
            <div class="track-actions">
              <button
                v-if="outgoingUserIds.has(u.id)"
                class="secondary-button"
                type="button"
                disabled
              >
                Заявка отправлена
              </button>
              <button
                v-else
                class="primary-button"
                type="button"
                @click="sendFriendRequest(u.id)"
              >
                В друзья
              </button>
            </div>
          </div>
        </div>
        <div v-else-if="searchError" class="muted" style="margin-bottom: 16px; color: #f97373">
          {{ searchError }}
        </div>

        <div class="auth-tabs">
          <button
            type="button"
            class="auth-tab"
            :class="{ active: activeTab === 'friends' }"
            @click="activeTab = 'friends'"
          >
            Друзья
          </button>
          <button
            type="button"
            class="auth-tab"
            :class="{ active: activeTab === 'incoming' }"
            @click="activeTab = 'incoming'"
          >
            Входящие
          </button>
          <button
            type="button"
            class="auth-tab"
            :class="{ active: activeTab === 'outgoing' }"
            @click="activeTab = 'outgoing'"
          >
            Отправленные
          </button>
        </div>

        <div v-if="loading" class="muted">Загружаем...</div>

        <div v-else>
          <div v-if="activeTab === 'friends'">
            <div v-if="!friends.length" class="muted">Пока нет друзей.</div>
            <div v-else class="tracks-list">
              <div v-for="f in friends" :key="f.id" class="track-row">
                <div class="auth-actions" style="flex: 1">
                  <div class="avatar-wrapper" style="width: 40px; height: 40px">
                    <img
                      v-if="f.avatarUrl"
                      :src="`http://localhost:8080/api/users/${f.id}/avatar`"
                      alt="avatar"
                      class="avatar-image"
                    />
                  </div>
                  <div>
                    <div class="track-title">{{ f.username }}</div>
                    <div class="muted" v-if="f.bio">{{ f.bio }}</div>
                  </div>
                </div>
                <div class="track-actions">
                  <button
                    type="button"
                    class="secondary-button"
                    @click="removeFriend(f.id)"
                  >
                    Удалить из друзей
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="activeTab === 'incoming'">
            <div v-if="!incoming.length" class="muted">Нет входящих заявок.</div>
            <div v-else class="tracks-list">
              <div v-for="r in incoming" :key="r.id" class="track-row">
                <div class="auth-actions">
                  <div class="avatar-wrapper" style="width: 40px; height: 40px">
                    <img
                      v-if="r.user.avatarUrl"
                      :src="`http://localhost:8080/api/users/${r.user.id}/avatar`"
                      alt="avatar"
                      class="avatar-image"
                    />
                  </div>
                  <div>
                    <div class="track-title">{{ r.user.username }}</div>
                    <div class="muted">
                      Заявка в друзья · {{ new Date(r.createdAt).toLocaleString() }}
                    </div>
                  </div>
                </div>
                <div class="track-actions">
                  <button class="secondary-button" type="button" @click="rejectRequest(r.id)">
                    Отклонить
                  </button>
                  <button class="primary-button" type="button" @click="acceptRequest(r.id)">
                    Принять
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div v-else>
            <div v-if="!outgoing.length" class="muted">Нет отправленных заявок.</div>
            <div v-else class="tracks-list">
              <div v-for="r in outgoing" :key="r.id" class="track-row">
                <div class="auth-actions">
                  <div class="avatar-wrapper" style="width: 40px; height: 40px">
                    <img
                      v-if="r.user.avatarUrl"
                      :src="`http://localhost:8080/api/users/${r.user.id}/avatar`"
                      alt="avatar"
                      class="avatar-image"
                    />
                  </div>
                  <div>
                    <div class="track-title">{{ r.user.username }}</div>
                    <div class="muted">
                      Ожидает подтверждения · {{ new Date(r.createdAt).toLocaleString() }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <p v-if="errorMessage" class="muted" style="margin-top: 8px; color: #f97373">
          {{ errorMessage }}
        </p>
      </div>
    </section>
  </div>
</template>

