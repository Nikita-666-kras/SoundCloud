<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api';
import { assetUrl } from '../config';
import { useAuthStore } from '../stores/auth';
import { usePlayerStore, type TrackListItem } from '../stores/player';
import { useTrackFavoritesStore } from '../stores/trackFavorites';
import { useTrackRowActions } from '../composables/useTrackRowActions';
import TrackRowActionsModals from '../components/TrackRowActionsModals.vue';

const router = useRouter();
const auth = useAuthStore();
const player = usePlayerStore();
const trackFavorites = useTrackFavoritesStore();
const tracks = ref<TrackListItem[]>([]);
const loading = ref(false);

const trackActions = useTrackRowActions(tracks, { removeOnUnlike: true });

async function loadFavorites() {
  if (!auth.user) return;
  loading.value = true;
  try {
    const res = await api.get<TrackListItem[]>(`/users/${auth.user.id}/favorites`);
    tracks.value = res.data;
    trackFavorites.mergeIds(tracks.value.map(t => t.id));
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
}

function playTrack(trackId: string) {
  if (player.currentTrackId === trackId) {
    player.togglePlay();
  } else {
    player.setQueueAndPlay(tracks.value, trackId);
  }
}

onMounted(() => loadFavorites());

function goToArtist(artistId: string) {
  router.push(`/artist/${artistId}`);
}
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header">
        <div>
          <div class="card-title">Понравившиеся</div>
          <div class="muted">Все треки, которые ты лайкнул</div>
        </div>
      </div>

      <div v-if="!auth.user" class="muted">
        Чтобы увидеть избранное, войди или зарегистрируйся.
      </div>
      <div v-else-if="loading" class="muted">Загружаем избранные треки...</div>
      <div v-else-if="!tracks.length" class="muted">У тебя пока нет избранных треков.</div>
      <div v-else class="tracks-list">
        <div
          v-for="(track, index) in tracks"
          :key="track.id"
          class="track-row"
          :class="{ active: player.currentTrackId === track.id }"
        >
          <div
            v-if="track.coverUrl && player.currentTrackId === track.id"
            class="track-post-cover cover-visible"
            @click="playTrack(track.id)"
          >
            <img :src="assetUrl(track.coverUrl)" alt="cover" />
          </div>
          <div class="track-row-body">
            <div class="track-index">
              <div class="track-number">#{{ index + 1 }}</div>
            </div>
            <div class="track-main">
              <div class="track-title">{{ track.title }}</div>
              <div class="track-meta">
                <span class="track-artist" @click.stop="goToArtist(track.ownerId)">
                  {{ track.ownerUsername }}
                </span>
                <span v-if="track.genre">{{ track.genre }}</span>
                <span v-if="track.album">Альбом: {{ track.album }}</span>
              </div>
            </div>
            <div class="track-actions">
              <button
                type="button"
                class="track-action-play"
                :aria-label="player.currentTrackId === track.id && player.isPlaying ? 'Пауза' : 'Воспроизвести'"
                @click.stop="playTrack(track.id)"
              >
                <svg
                  v-if="player.currentTrackId === track.id && player.isPlaying"
                  class="track-action-play-svg"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path fill="currentColor" d="M6 5h4v14H6V5zm8 0h4v14h-4V5z" />
                </svg>
                <svg v-else class="track-action-play-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <path fill="currentColor" d="M8 5.14v13.72L19 12 8 5.14z" />
                </svg>
              </button>
              <button
                v-if="auth.user"
                type="button"
                class="track-action-icon-btn"
                :class="{ liked: !!track.likedByMe }"
                aria-label="Лайк"
                @click.stop="trackActions.likeTrack(track.id)"
              >
                <svg class="track-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                <span v-if="track.likes > 0" class="track-action-count">{{ track.likes }}</span>
              </button>
              <button
                type="button"
                class="track-action-icon-btn"
                aria-label="Комментарии"
                @click.stop="trackActions.toggleComments(track.id)"
              >
                <svg class="track-action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              </button>
              <button
                v-if="auth.user"
                type="button"
                class="track-action-icon-btn"
                aria-label="Ещё"
                @click.stop="trackActions.openTrackActionsModal(track.id)"
              >
                <svg class="track-action-icon" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="12" cy="19" r="1.5"/></svg>
              </button>
            </div>
          </div>

          <div
            v-if="trackActions.commentsOpen[track.id]"
            class="comments-block"
            style="margin-top: 6px"
          >
            <div v-if="trackActions.commentsLoading[track.id]" class="muted">Загружаем комментарии...</div>
            <div v-else>
              <div
                v-if="!(trackActions.commentsByTrack[track.id] && trackActions.commentsByTrack[track.id].length)"
                class="muted"
              >
                Пока нет комментариев. Будь первым.
              </div>
              <div v-else class="comments-list">
                <div
                  v-for="comment in trackActions.commentsByTrack[track.id]"
                  :key="comment.id"
                  class="comment-row"
                >
                  <div class="comment-author">{{ comment.username }}</div>
                  <div class="comment-text">{{ comment.text }}</div>
                </div>
              </div>
            </div>
            <div v-if="auth.user" class="input-group" style="margin-top: 6px">
              <textarea
                v-model="trackActions.newCommentText[track.id]"
                class="input-control-textarea"
                rows="2"
                placeholder="Напиши комментарий"
              />
              <button
                class="secondary-button"
                type="button"
                style="align-self: flex-end; margin-top: 4px"
                @click="trackActions.submitComment(track.id)"
              >
                Отправить
              </button>
            </div>
            <div v-else class="muted" style="margin-top: 4px">
              Чтобы комментировать, войди или зарегистрируйся.
            </div>
          </div>

          <div
            v-if="auth.user && trackActions.reportOpen[track.id]"
            class="comments-block"
            style="margin-top: 6px"
          >
            <div class="section-title">Жалоба на трек</div>
            <div class="input-group">
              <select v-model="trackActions.reportReason[track.id]" class="input-control">
                <option value="Нарушение авторских прав">Нарушение авторских прав</option>
                <option value="Непристойное содержание">Непристойное содержание</option>
                <option value="Другое">Другое</option>
              </select>
            </div>
            <div class="input-group">
              <textarea
                v-model="trackActions.reportText[track.id]"
                class="input-control-textarea"
                rows="3"
                placeholder="Опиши проблему (опционально)"
              />
            </div>
            <div class="form-footer">
              <button class="secondary-button" type="button" @click="trackActions.toggleReport(track.id)">
                Отмена
              </button>
              <button
                class="primary-button"
                type="button"
                :disabled="trackActions.reportSending[track.id]"
                @click="trackActions.submitReport(track.id)"
              >
                {{ trackActions.reportSending[track.id] ? 'Отправляем...' : 'Отправить жалобу' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <TrackRowActionsModals v-if="auth.user" :api="trackActions" />
  </div>
</template>
