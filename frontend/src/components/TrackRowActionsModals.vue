<script setup lang="ts">
import { useAuthStore } from '../stores/auth';
import { assetUrl } from '../config';
import type { TrackRowActionsApi } from '../composables/useTrackRowActions';

defineProps<{ api: TrackRowActionsApi }>();

const auth = useAuthStore();
</script>

<template>
  <div
    v-if="auth.user && api.trackActionsModalTrackId"
    class="modal-overlay"
    @click.self="api.closeTrackActionsModal()"
  >
    <div class="modal-card track-actions-modal-card">
      <div class="modal-header">
        <div class="modal-title">Действия с треком</div>
        <button class="modal-close" type="button" @click="api.closeTrackActionsModal()">×</button>
      </div>
      <div class="track-actions-modal-buttons">
        <button
          type="button"
          class="track-menu-item track-actions-modal-item"
          @click="api.trackActionRepost()"
        >
          Репостнуть другу
        </button>
        <button
          type="button"
          class="track-menu-item track-actions-modal-item"
          @click="api.trackActionAddToPlaylist()"
        >
          Добавить в плейлист
        </button>
        <button
          type="button"
          class="track-menu-item track-actions-modal-item"
          @click="api.trackActionReport()"
        >
          Пожаловаться
        </button>
      </div>
    </div>
  </div>

  <div
    v-if="auth.user && api.repostDialogTrackId"
    class="modal-overlay"
    @click.self="api.closeRepostDialog()"
  >
    <div class="modal-card">
      <div class="modal-header">
        <div class="modal-title">Послать трек другу</div>
        <button class="modal-close" type="button" @click="api.closeRepostDialog()">×</button>
      </div>

      <div v-if="api.repostLoading" class="muted">Загружаем друзей...</div>
      <div v-else>
        <div v-if="api.repostError" class="muted" style="color: #f97373">
          {{ api.repostError }}
        </div>
        <div v-else-if="!api.repostFriends.length" class="muted">
          У тебя пока нет друзей.
        </div>
        <div v-else class="tracks-list">
          <div v-for="f in api.repostFriends" :key="f.id" class="track-row">
            <div class="auth-actions">
              <div class="avatar-wrapper" style="width: 40px; height: 40px">
                <img
                  v-if="f.avatarUrl"
                  :src="assetUrl(`/api/users/${f.id}/avatar`)"
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
              <button class="primary-button" type="button" @click="api.sendRepost(f.id)">
                Репостнуть
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="secondary-button" type="button" @click="api.closeRepostDialog()">
          Закрыть
        </button>
      </div>
    </div>
  </div>

  <div
    v-if="auth.user && api.addToPlaylistTrackId"
    class="modal-overlay"
    @click.self="api.closeAddToPlaylistModal()"
  >
    <div class="modal-card">
      <div class="modal-header">
        <div class="modal-title">Добавить в плейлист</div>
        <button class="modal-close" type="button" @click="api.closeAddToPlaylistModal()">×</button>
      </div>

      <div v-if="api.addToPlaylistError" class="muted" style="color: #f97373; margin-bottom: 8px">
        {{ api.addToPlaylistError }}
      </div>

      <div v-if="api.addToPlaylistCreateMode">
        <div class="input-group">
          <label class="input-label">Название</label>
          <input
            v-model="api.newPlaylistName"
            type="text"
            class="input-control"
            placeholder="Название плейлиста"
          />
        </div>
        <div class="input-group">
          <label class="input-label">Описание (необязательно)</label>
          <textarea
            v-model="api.newPlaylistDescription"
            class="input-control-textarea"
            rows="2"
            placeholder="Описание"
          />
        </div>
        <div class="modal-footer" style="margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(148, 163, 184, 0.3)">
          <button
            class="secondary-button"
            type="button"
            @click="api.addToPlaylistCreateMode = false"
          >
            Отмена
          </button>
          <button
            class="primary-button"
            type="button"
            :disabled="api.createPlaylistLoading"
            @click="api.createPlaylistAndAdd()"
          >
            {{ api.createPlaylistLoading ? 'Создаём...' : 'Создать и добавить трек' }}
          </button>
        </div>
      </div>

      <div v-else>
        <div v-if="api.addToPlaylistLoading" class="muted">Загружаем плейлисты...</div>
        <div v-else>
          <div v-if="!api.addToPlaylistPlaylists.length" class="muted">
            У тебя пока нет плейлистов. Создай новый ниже.
          </div>
          <div v-else class="add-to-playlist-list">
            <div
              v-for="p in api.addToPlaylistPlaylists"
              :key="p.id"
              class="add-to-playlist-item"
              @click="api.addTrackToPlaylist(p.id)"
            >
              <div class="add-to-playlist-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16"/></svg>
              </div>
              <div class="add-to-playlist-text">
                <div class="track-title">{{ p.name }}</div>
                <div class="muted" v-if="p.description">{{ p.description }}</div>
                <div class="muted">{{ p.trackCount }} трек(ов)</div>
              </div>
              <div v-if="api.addTrackToPlaylistLoading === p.id" class="add-to-playlist-loading muted">...</div>
            </div>
          </div>
          <button
            type="button"
            class="primary-button"
            style="margin-top: 12px"
            @click="api.showCreatePlaylistForm()"
          >
            Создать плейлист
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
