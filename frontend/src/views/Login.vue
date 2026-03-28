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

async function handleLogin() {
  if (!email.value || !password.value) {
    errorMessage.value = 'Укажи email и пароль.';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    const res = await api.post<{ accessToken: string; refreshToken: string; expiresIn: number; user: typeof auth.user }>('/auth/login', {
      email: email.value,
      password: password.value
    });
    auth.setAuth(res.data.accessToken, res.data.refreshToken, res.data.user!);
    router.push('/');
  } catch (e) {
    errorMessage.value = 'Не удалось войти. Проверь данные.';
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
          <div class="card-title">Вход</div>
          <div class="muted">Вернись к своим трекам и плейлистам</div>
        </div>
      </div>

      <div class="auth-tabs">
        <button type="button" class="auth-tab active">Вход</button>
        <button type="button" class="auth-tab" @click="router.push('/register')">
          Регистрация
        </button>
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
            placeholder="Твой пароль"
          />
        </div>
      </div>

      <div class="form-footer">
        <div class="muted">
          Нет аккаунта?
          <a href="" @click.prevent="router.push('/register')">Зарегистрироваться</a>
        </div>
        <button class="primary-button" :disabled="loading" @click="handleLogin">
          {{ loading ? 'Входим...' : 'Войти' }}
        </button>
      </div>

      <p v-if="errorMessage" class="muted" style="margin-top: 8px; color: #f97373">
        {{ errorMessage }}
      </p>
    </section>
  </div>
</template>

