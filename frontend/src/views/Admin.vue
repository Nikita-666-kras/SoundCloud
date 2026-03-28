<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
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

const auth = useAuthStore();

const reports = ref<TrackReport[]>([]);
const loading = ref(false);
const errorMessage = ref('');

const isAdmin = computed(() => !!auth.user?.admin);

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

async function resolveReport(id: string) {
  if (!auth.user) return;
  try {
    await api.post(`/admin/reports/${id}/resolve`);
    reports.value = reports.value.filter(r => r.id !== id);
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось закрыть жалобу.';
  }
}

onMounted(() => {
  if (isAdmin.value) {
    loadReports();
  }
});
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Админка · Жалобы на треки</div>
          <div class="muted">Авторское право, непристойное содержание и другое</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">
        Войдите под админским аккаунтом.
      </div>
      <div v-else-if="!isAdmin" class="muted">
        У этого аккаунта нет прав администратора.
      </div>
      <div v-else>
        <div v-if="loading" class="muted">Загружаем жалобы...</div>
        <div v-else-if="!reports.length" class="muted">Пока нет открытых жалоб.</div>
        <div v-else class="tracks-list">
          <div
            v-for="report in reports"
            :key="report.id"
            class="track-row"
          >
            <div class="track-main">
              <div class="track-title">
                {{ report.trackTitle }}
              </div>
              <div class="track-meta">
                <span>Жалоба: {{ report.reason }}</span>
                <span>Автор жалобы: {{ report.reporterUsername }}</span>
                <span>{{ new Date(report.createdAt).toLocaleString() }}</span>
              </div>
              <div
                v-if="report.details"
                class="comment-text"
                style="margin-top: 4px"
              >
                {{ report.details }}
              </div>
            </div>
            <div class="track-actions">
              <button class="primary-button" type="button" @click="resolveReport(report.id)">
                Закрыть жалобу
              </button>
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

