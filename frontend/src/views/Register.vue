<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();

const email = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');

async function handleRegister() {
  if (!email.value || !password.value) {
    errorMessage.value = 'Укажи email и пароль.';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    const username = email.value.split('@')[0];
    const res = await api.post<{ accessToken: string; refreshToken: string; expiresIn: number; user: typeof auth.user }>('/auth/register', {
      email: email.value,
      password: password.value,
      username
    });
    auth.setAuth(res.data.accessToken, res.data.refreshToken, res.data.user!);
    router.push('/');
  } catch (e) {
    errorMessage.value = 'Регистрация не удалась. Возможно, email уже используется.';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Регистрация</div>
          <div class="muted">Создай аккаунт, чтобы выкладывать треки</div>
        </div>
      </div>

      <div class="auth-tabs">
        <button type="button" class="auth-tab" @click="router.push('/login')">Вход</button>
        <button type="button" class="auth-tab active">Регистрация</button>
      </div>

      <div class="form-grid">
        <div class="input-group">
          <label class="input-label">Email</label>
          <input v-model="email" class="input-control" type="email" placeholder="you@example.com" />
        </div>

        <div class="input-group">
          <label class="input-label">Пароль</label>
          <input
            v-model="password"
            class="input-control"
            type="password"
            placeholder="Минимум 6 символов"
          />
        </div>
      </div>

      <div class="form-footer">
        <div class="muted">
          Уже есть аккаунт?
          <a href="" @click.prevent="router.push('/login')">Войти</a>
        </div>
        <button class="primary-button" :disabled="loading" @click="handleRegister">
          {{ loading ? 'Создаём...' : 'Зарегистрироваться' }}
        </button>
      </div>

      <p v-if="errorMessage" class="muted" style="margin-top: 8px; color: #f97373">
        {{ errorMessage }}
      </p>
    </section>
  </div>
</template>

