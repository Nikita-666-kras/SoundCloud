<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { api } from '../api';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const router = useRouter();

const username = ref('');
const bio = ref('');

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

function goToEditProfile() {
  router.push('/profile/edit');
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Профиль музыканта</div>
          <div class="muted">Предпросмотр твоей карточки музыканта</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">
        Чтобы увидеть профиль, войди или зарегистрируйся.
      </div>

      <div v-else class="artist-preview">
        <div class="auth-actions" style="margin-bottom: 8px">
          <div v-if="auth.user.avatarUrl" class="avatar-wrapper" style="width: 56px; height: 56px">
            <img
              :src="`http://localhost:8080/api/users/${auth.user.id}/avatar`"
              alt="avatar"
              class="avatar-image"
            />
          </div>
          <div>
            <div class="card-title">{{ username || auth.user.username }}</div>
            <div class="muted">Артист slapshous</div>
          </div>
        </div>

        <div class="muted" style="margin-bottom: 12px" v-if="bio">
          {{ bio }}
        </div>

        <button class="secondary-button" type="button" @click="goToEditProfile">
          Изменить профиль
        </button>
      </div>
    </section>
  </div>
</template>

