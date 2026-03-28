<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';

interface TrackReport {
  id: string;
  trackId: string;
  trackTitle: string;
  reporterId: string;
  reporterUsername: string;
  reason: string;
  details: string | null;
  createdAt: string;
  resolved: boolean;
}

interface SupportThread {
  userId: string;
  username: string;
  lastMessagePreview: string;
  lastMessageAt: string;
}

interface SupportMessage {
  id: string;
  sender: 'USER' | 'STAFF';
  body: string;
  createdAt: string;
}

const auth = useAuthStore();
const isAdmin = computed(() => !!auth.user?.admin);

const reports = ref<TrackReport[]>([]);
const loading = ref(false);
const errorMessage = ref('');

const supportThreads = ref<SupportThread[]>([]);
const supportThreadsLoading = ref(false);
const supportError = ref('');
const selectedUserId = ref<string | null>(null);
const threadMessages = ref<SupportMessage[]>([]);
const threadLoading = ref(false);
const replyText = ref('');
const replySending = ref(false);

async function loadReports() {
  if (!auth.user) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    const res = await api.get<TrackReport[]>('/admin/reports');
    reports.value = res.data;
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось загрузить жалобы.';
  } finally {
    loading.value = false;
  }
}

async function loadSupportThreads() {
  if (!auth.user) return;
  supportThreadsLoading.value = true;
  supportError.value = '';
  try {
    const res = await api.get<SupportThread[]>('/admin/support/threads');
    supportThreads.value = Array.isArray(res.data) ? res.data : [];
  } catch (e) {
    console.error(e);
    supportError.value = 'Не удалось загрузить диалоги поддержки.';
  } finally {
    supportThreadsLoading.value = false;
  }
}

async function loadThreadMessages(userId: string) {
  threadLoading.value = true;
  supportError.value = '';
  try {
    const res = await api.get<SupportMessage[]>('/admin/support/messages', { params: { userId } });
    threadMessages.value = Array.isArray(res.data) ? res.data : [];
  } catch (e) {
    console.error(e);
    supportError.value = 'Не удалось загрузить сообщения.';
    threadMessages.value = [];
  } finally {
    threadLoading.value = false;
  }
}

function selectThread(userId: string) {
  selectedUserId.value = userId;
  loadThreadMessages(userId);
}

async function sendSupportReply() {
  if (!selectedUserId.value || !replyText.value.trim() || replySending.value) return;
  replySending.value = true;
  supportError.value = '';
  try {
    await api.post('/admin/support/reply', {
      userId: selectedUserId.value,
      text: replyText.value.trim()
    });
    replyText.value = '';
    await loadThreadMessages(selectedUserId.value);
    await loadSupportThreads();
  } catch (e) {
    console.error(e);
    supportError.value = 'Не удалось отправить ответ.';
  } finally {
    replySending.value = false;
  }
}

async function resolveReport(id: string) {
  if (!auth.user) return;
  try {
    await api.post(`/admin/reports/${id}/resolve`);
    reports.value = reports.value.filter((r) => r.id !== id);
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось закрыть жалобу.';
  }
}

function formatDt(iso: string) {
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

watch(selectedUserId, (id) => {
  if (!id) threadMessages.value = [];
});

onMounted(() => {
  if (isAdmin.value) {
    loadReports();
    loadSupportThreads();
  }
});
</script>

<template>
  <div class="layout-single admin-page">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Админка · Жалобы на треки</div>
          <div class="muted">Авторское право, непристойное содержание и другое</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">Войдите под админским аккаунтом.</div>
      <div v-else-if="!isAdmin" class="muted">У этого аккаунта нет прав администратора.</div>
      <div v-else>
        <div v-if="loading" class="muted">Загружаем жалобы...</div>
        <div v-else-if="!reports.length" class="muted">Пока нет открытых жалоб.</div>
        <div v-else class="tracks-list">
          <div v-for="report in reports" :key="report.id" class="track-row">
            <div class="track-main">
              <div class="track-title">{{ report.trackTitle }}</div>
              <div class="track-meta">
                <span>Жалоба: {{ report.reason }}</span>
                <span>Автор жалобы: {{ report.reporterUsername }}</span>
                <span>{{ new Date(report.createdAt).toLocaleString() }}</span>
              </div>
              <div v-if="report.details" class="comment-text" style="margin-top: 4px">{{ report.details }}</div>
            </div>
            <div class="track-actions">
              <button class="primary-button" type="button" @click="resolveReport(report.id)">Закрыть жалобу</button>
            </div>
          </div>
        </div>

        <p v-if="errorMessage" class="muted" style="margin-top: 8px; color: #f97373">{{ errorMessage }}</p>
      </div>
    </section>

    <section v-if="auth.user && isAdmin" class="card admin-support-card">
      <div class="card-header">
        <div>
          <div class="card-title">Поддержка · диалоги</div>
          <div class="muted">Пользователи, у которых есть переписка с поддержкой</div>
        </div>
        <button type="button" class="secondary-button" @click="loadSupportThreads">Обновить список</button>
      </div>

      <p v-if="supportError" class="muted admin-support-banner-error">{{ supportError }}</p>

      <div v-if="supportThreadsLoading" class="muted">Загружаем диалоги...</div>
      <div v-else-if="!supportThreads.length" class="muted">Пока нет ни одного диалога.</div>
      <div v-else class="admin-support-layout">
        <div class="admin-support-thread-list">
          <button
            v-for="t in supportThreads"
            :key="t.userId"
            type="button"
            class="admin-support-thread-item"
            :class="{ active: selectedUserId === t.userId }"
            @click="selectThread(t.userId)"
          >
            <div class="admin-support-thread-name">{{ t.username }}</div>
            <div class="admin-support-thread-preview muted">{{ t.lastMessagePreview }}</div>
            <div class="admin-support-thread-time muted">{{ formatDt(t.lastMessageAt) }}</div>
          </button>
        </div>
        <div class="admin-support-thread-pane">
          <template v-if="!selectedUserId">
            <div class="muted">Выберите пользователя слева, чтобы открыть чат.</div>
          </template>
          <template v-else>
            <div v-if="threadLoading" class="muted">Загружаем сообщения...</div>
            <div v-else class="admin-support-messages">
              <div
                v-for="m in threadMessages"
                :key="m.id"
                class="admin-support-msg"
                :class="m.sender === 'USER' ? 'admin-support-msg--user' : 'admin-support-msg--staff'"
              >
                <span class="admin-support-msg-label">{{ m.sender === 'USER' ? 'Пользователь' : 'Поддержка' }}</span>
                <div class="admin-support-msg-body">{{ m.body }}</div>
                <span class="admin-support-msg-time muted">{{ formatDt(m.createdAt) }}</span>
              </div>
            </div>
            <div class="admin-support-reply">
              <textarea
                v-model="replyText"
                class="input-control admin-support-reply-text"
                rows="3"
                placeholder="Ответ пользователю..."
                maxlength="4000"
              />
              <button
                type="button"
                class="primary-button"
                :disabled="replySending || !replyText.trim()"
                @click="sendSupportReply"
              >
                {{ replySending ? 'Отправляем…' : 'Отправить ответ' }}
              </button>
            </div>
          </template>
        </div>
      </div>
    </section>
  </div>
</template>
