<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue';
import { useAuthStore } from '../stores/auth';
import { api } from '../api';

interface SupportMessage {
  id: string;
  sender: 'USER' | 'STAFF';
  body: string;
  createdAt: string;
}

const auth = useAuthStore();
const messages = ref<SupportMessage[]>([]);
const loading = ref(false);
const sending = ref(false);
const inputText = ref('');
const errorMessage = ref('');
const listRef = ref<HTMLElement | null>(null);

async function loadMessages() {
  if (!auth.user) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    const res = await api.get<SupportMessage[]>('/support/messages');
    messages.value = Array.isArray(res.data) ? res.data : [];
    await nextTick();
    scrollToBottom();
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось загрузить переписку.';
  } finally {
    loading.value = false;
  }
}

function scrollToBottom() {
  const el = listRef.value;
  if (el) el.scrollTop = el.scrollHeight;
}

async function sendMessage() {
  if (!auth.user || !inputText.value.trim() || sending.value) return;
  sending.value = true;
  errorMessage.value = '';
  try {
    const res = await api.post<SupportMessage>('/support/messages', { text: inputText.value.trim() });
    messages.value.push(res.data);
    inputText.value = '';
    await nextTick();
    scrollToBottom();
  } catch (e) {
    console.error(e);
    errorMessage.value = 'Не удалось отправить сообщение.';
  } finally {
    sending.value = false;
  }
}

function formatTime(iso: string) {
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return '';
  }
}

onMounted(() => {
  loadMessages();
});
</script>

<template>
  <div class="layout-single">
    <section class="card support-chat-card">
      <div class="card-header">
        <div>
          <div class="card-title">Поддержка</div>
          <div class="muted">Напишите вопрос — ответ команды появится в этом чате.</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">Войдите, чтобы написать в поддержку.</div>

      <template v-else>
        <div v-if="loading" class="muted">Загружаем сообщения...</div>
        <div v-else ref="listRef" class="support-chat-list">
          <div
            v-for="m in messages"
            :key="m.id"
            class="support-chat-bubble-wrap"
            :class="m.sender === 'USER' ? 'support-chat-bubble-wrap--user' : 'support-chat-bubble-wrap--staff'"
          >
            <div class="support-chat-bubble" :class="m.sender === 'USER' ? 'support-chat-bubble--user' : 'support-chat-bubble--staff'">
              <div class="support-chat-bubble-label">{{ m.sender === 'USER' ? 'Вы' : 'Поддержка' }}</div>
              <div class="support-chat-bubble-text">{{ m.body }}</div>
              <div class="support-chat-bubble-time muted">{{ formatTime(m.createdAt) }}</div>
            </div>
          </div>
        </div>

        <p v-if="errorMessage" class="muted support-chat-error">{{ errorMessage }}</p>

        <div class="support-chat-input-row">
          <input
            v-model="inputText"
            type="text"
            class="input-control support-chat-input"
            placeholder="Ваше сообщение..."
            maxlength="4000"
            @keydown.enter.prevent="sendMessage"
          />
          <button type="button" class="primary-button" :disabled="sending || !inputText.trim()" @click="sendMessage">
            {{ sending ? '…' : 'Отправить' }}
          </button>
        </div>
      </template>
    </section>
  </div>
</template>
