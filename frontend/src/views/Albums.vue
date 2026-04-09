<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { api } from '../api';
import { assetUrl } from '../config';
import { useAuthStore } from '../stores/auth';
import { useAlbumLikesStore } from '../stores/albumLikes';
import type { AlbumSummary } from '../stores/albumLikes';

interface AlbumItem {
  ownerId: string;
  ownerUsername: string;
  album: string;
  coverUrl: string | null;
  trackCount: number;
  playCount?: number;
}

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const albumLikes = useAlbumLikesStore();
const albums = ref<AlbumItem[]>([]);
const loading = ref(false);
const sort = computed(() => {
  if (route.query.liked === '1') return 'liked';
  return route.query.sort === 'recent' ? 'recent' : 'popular';
});

const displayAlbums = computed<AlbumItem[]>(() => {
  if (sort.value === 'liked') {
    return albumLikes.likedAlbums.map((a) => ({
      ownerId: a.ownerId,
      ownerUsername: a.ownerUsername,
      album: a.album,
      coverUrl: a.coverUrl,
      trackCount: a.trackCount,
      playCount: a.playCount ?? 0
    }));
  }
  return albums.value;
});

async function load() {
  if (sort.value === 'liked') {
    await albumLikes.loadLiked();
    return;
  }
  loading.value = true;
  try {
    const res = await api.get<AlbumItem[]>('/tracks/albums', {
      params: { sort: sort.value, limit: 50 }
    });
    albums.value = res.data ?? [];
  } catch (e) {
    console.error(e);
    albums.value = [];
  } finally {
    loading.value = false;
  }
}

function goToAlbum(album: AlbumItem | AlbumSummary) {
  router.push(`/artist/${album.ownerId}/album/${encodeURIComponent(album.album)}`);
}

function setSort(s: 'popular' | 'recent' | 'liked') {
  if (s === 'liked') {
    router.replace({ query: { ...route.query, liked: '1' } });
  } else {
    const { liked, ...rest } = route.query;
    router.replace({ query: { ...rest, sort: s } });
  }
}

onMounted(() => {
  if (auth.user && (route.query.liked === '1' || sort.value === 'liked')) {
    albumLikes.loadLiked();
  }
  if (sort.value !== 'liked') load();
});
watch(sort, () => {
  if (sort.value === 'liked') {
    if (auth.user) albumLikes.loadLiked();
  } else {
    load();
  }
});
</script>

<template>
  <div class="layout-single">
    <section class="card">
      <div class="card-header albums-card-header">
        <div class="albums-card-header-text">
          <div class="card-title">Альбомы</div>
          <div class="muted">Популярные и новые альбомы</div>
        </div>
        <div class="albums-sort-tabs" role="tablist" aria-label="Сортировка альбомов">
          <button
            type="button"
            class="home-tab"
            :class="{ active: sort === 'popular' }"
            @click="setSort('popular')"
          >
            Популярные
          </button>
          <button
            type="button"
            class="home-tab"
            :class="{ active: sort === 'recent' }"
            @click="setSort('recent')"
          >
            Новые
          </button>
          <button
            v-if="auth.user"
            type="button"
            class="home-tab"
            :class="{ active: sort === 'liked' }"
            @click="setSort('liked')"
          >
            Понравившиеся
          </button>
        </div>
      </div>

      <div v-if="sort === 'liked' && !albumLikes.loaded" class="muted" style="padding: 24px">Загружаем...</div>
      <div v-else-if="loading" class="muted" style="padding: 24px">Загружаем альбомы...</div>
      <div v-else-if="!displayAlbums.length" class="muted" style="padding: 24px">
        {{ sort === 'liked' ? 'Пока нет понравившихся альбомов.' : 'Пока нет альбомов.' }}
      </div>
      <div v-else class="albums-feed-grid">
        <div
          v-for="(album, i) in displayAlbums"
          :key="`${album.ownerId}-${album.album}-${i}`"
          class="popular-album-card albums-feed-card"
          @click="goToAlbum(album)"
        >
          <button
            v-if="auth.user"
            type="button"
            class="album-like-btn"
            :class="{ liked: albumLikes.isLiked(album.ownerId, album.album) }"
            @click.stop="albumLikes.toggle(album.ownerId, album.album, album.ownerUsername, album.coverUrl, album.trackCount)"
            aria-label="Лайк"
          >
            ❤
          </button>
          <div class="popular-album-image" v-if="album.coverUrl">
            <img :src="assetUrl(album.coverUrl)" alt="cover" />
          </div>
            <div class="popular-album-overlay">
            <div class="popular-album-title">{{ album.album }}</div>
            <div class="popular-album-meta">
              {{ album.ownerUsername }} · {{ album.trackCount }} трек(ов)
              <span v-if="album.playCount != null && album.playCount > 0"> · {{ album.playCount }} прослушиваний</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
