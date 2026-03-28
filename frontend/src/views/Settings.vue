<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();

const isPrivate = ref(false);
const theme = ref<'light' | 'dark'>('light');
const saving = ref(false);
const errorMessage = ref('');
const deleteAccountConfirm = ref(false);
const deletingAccount = ref(false);

onMounted(async () => {
  const storedTheme = (localStorage.getItem('theme') as 'light' | 'dark' | null) || 'light';
  theme.value = storedTheme;
  applyTheme(storedTheme);

  if (!auth.user) return;
  try {
    const res = await api.get(`/users/${auth.user.id}`);
    isPrivate.value = !!res.data.privateAccount;
  } catch (e) {
    console.error(e);
  }
});

function applyTheme(value: 'light' | 'dark') {
  if (value === 'dark') {
    document.body.classList.add('dark-theme');
  } else {
    document.body.classList.remove('dark-theme');
  }
  localStorage.setItem('theme', value);
}

function setTheme(value: 'light' | 'dark') {
  theme.value = value;
  applyTheme(value);
}

async function savePrivacy() {
  if (!auth.user) return;
  saving.value = true;
  errorMessage.value = '';
  try {
    const res = await api.put(`/users/${auth.user.id}`, {
      username: null,
      bio: null,
      privateAccount: isPrivate.value
    });
    auth.setUser(res.data);
  } catch (err) {
    console.error(err);
    errorMessage.value = 'Не удалось сохранить настройки приватности.';
  } finally {
    saving.value = false;
  }
}

async function deleteAccount() {
  if (!auth.user || !deleteAccountConfirm.value) return;
  deletingAccount.value = true;
  errorMessage.value = '';
  try {
    await api.delete('/users/me');
    auth.clearAuth();
    router.push('/');
  } catch (err) {
    console.error(err);
    errorMessage.value = 'Не удалось удалить аккаунт.';
  } finally {
    deletingAccount.value = false;
  }
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Настройки аккаунта</div>
          <div class="muted">Тема и приватность профиля</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">
        Чтобы менять настройки, войди или зарегистрируйся.
      </div>

      <div v-else>
        <div class="section-title">Тема</div>
        <div class="auth-tabs">
          <button
            type="button"
            class="auth-tab"
            :class="{ active: theme === 'light' }"
            @click="setTheme('light')"
          >
            Светлая
          </button>
          <button
            type="button"
            class="auth-tab"
            :class="{ active: theme === 'dark' }"
            @click="setTheme('dark')"
          >
            Тёмная
          </button>
        </div>

        <div class="section-title" style="margin-top: 16px">Приватность профиля</div>
        <div class="auth-tabs">
          <button
            type="button"
            class="auth-tab"
            :class="{ active: !isPrivate }"
            @click="isPrivate = false"
          >
            Публичный
          </button>
          <button
            type="button"
            class="auth-tab"
            :class="{ active: isPrivate }"
            @click="isPrivate = true"
          >
            Закрытый
          </button>
          
        </div>
        <div class="muted">Закрытый аккаунт не показывает треки в общей ленте.</div>
        <div class="form-footer">
          
          <button class="primary-button" :disabled="saving" @click="savePrivacy">
            {{ saving ? 'Сохраняем...' : 'Сохранить' }}
          </button>
        </div>

        <p v-if="errorMessage" class="muted" style="margin-top: 8px; color: #f97373">
          {{ errorMessage }}
        </p>

        <div class="section-title" style="margin-top: 32px">Удаление аккаунта</div>
       
        <div v-if="!deleteAccountConfirm">
          <button type="button" class="secondary-button" style="border-color: #f87171; color: #dc2626" @click="deleteAccountConfirm = true">
            Удалить аккаунт
          </button>
        </div>
        
        <div v-else>
          <p class="muted" style="margin-bottom: 8px">Вы уверены? Все данные будут безвозвратно удалены.</p>
          <button
            type="button"
            class="secondary-button"
            style="border-color: #f87171; color: #dc2626; margin-right: 8px"
            :disabled="deletingAccount"
            @click="deleteAccount"
          >
            {{ deletingAccount ? 'Удаляем...' : 'Да, удалить аккаунт' }}
          </button>
          <button type="button" class="secondary-button" :disabled="deletingAccount" @click="deleteAccountConfirm = false">
            Отмена
          </button>
        </div>
        <div class="muted" style="margin-bottom: 8px">
          Удаляются профиль, все треки, альбомы и плейлисты. Это нельзя отменить.
        </div>
      </div>
    </section>
  </div>
</template>

