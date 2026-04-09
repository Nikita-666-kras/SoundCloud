<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../api';
import { assetUrl } from '../config';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const router = useRouter();

const username = ref('');
const bio = ref('');
const saving = ref(false);
const errorMessage = ref('');

onMounted(async () => {
  if (!auth.user) return;
  try {
    const res = await api.get(`/users/${auth.user.id}`);
    username.value = res.data.username ?? '';
    bio.value = res.data.bio ?? '';
  } catch (e) {
    console.error(e);
  }
});

async function handleAvatarChange(e: Event) {
  if (!auth.user) return;
  const target = e.target as HTMLInputElement;
  if (!target.files || !target.files[0]) return;

  try {
    const form = new FormData();
    form.append('file', target.files[0]);
    await api.post(`/users/${auth.user.id}/avatar`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    const res = await api.get(`/users/${auth.user.id}`);
    auth.setUser(res.data);
  } catch (err) {
    console.error(err);
    errorMessage.value = 'Не удалось загрузить аватар.';
  }
}

async function saveProfile() {
  if (!auth.user) return;
  saving.value = true;
  errorMessage.value = '';
  try {
    const res = await api.put(`/users/${auth.user.id}`, {
      username: username.value,
      bio: bio.value,
      privateAccount: null
    });
    auth.setUser(res.data);
    router.push('/profile');
  } catch (err) {
    console.error(err);
    errorMessage.value = 'Не удалось сохранить профиль.';
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Редактирование профиля</div>
          <div class="muted">Имя, био и аватар артиста</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">
        Чтобы редактировать профиль, войди или зарегистрируйся.
      </div>

      <div v-else>
        <div class="section-title">Аватар</div>
        <div class="input-group">
          <div class="auth-actions">
            <div v-if="auth.user.avatarUrl" class="avatar-wrapper">
              <img
                :src="assetUrl(`/api/users/${auth.user.id}/avatar`)"
                alt="avatar"
                class="avatar-image"
              />
            </div>
            <input class="input-control" type="file" accept="image/*" @change="handleAvatarChange" />
          </div>
        </div>

        <div class="input-group">
          <label class="input-label">Имя музыканта</label>
          <input v-model="username" class="input-control" type="text" />
        </div>

        <div class="input-group">
          <label class="input-label">Био</label>
          <textarea
            v-model="bio"
            class="input-control-textarea"
            rows="3"
            placeholder="Пара слов о себе, жанры, ссылки..."
          />
        </div>

        <div class="form-footer">
          <div class="muted">Эти данные видны в карточке музыканта.</div>
          <button class="primary-button" :disabled="saving" @click="saveProfile">
            {{ saving ? 'Сохраняем...' : 'Сохранить' }}
          </button>
        </div>

        <p v-if="errorMessage" class="muted" style="margin-top: 8px; color: #f97373">
          {{ errorMessage }}
        </p>
      </div>
    </section>
  </div>
</template>

