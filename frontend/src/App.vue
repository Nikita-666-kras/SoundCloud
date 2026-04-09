<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch, nextTick } from 'vue';
import { useRouter, useRoute, RouterView } from 'vue-router';
import { useAuthStore } from './stores/auth';
import { usePlayerStore, type TrackListItem } from './stores/player';
import { useNotificationStore } from './stores/notifications';
import { useAlbumLikesStore } from './stores/albumLikes';
import { useTrackFavoritesStore } from './stores/trackFavorites';
import { api } from './api';
import { assetUrl } from './config';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const player = usePlayerStore();
const notifications = useNotificationStore();
const albumLikes = useAlbumLikesStore();
const trackFavorites = useTrackFavoritesStore();
const showUserMenu = ref(false);
const audioElement = ref<HTMLAudioElement | null>(null);
const mobileMenuOpen = ref(false);
const mobileSearchOpen = ref(false);
const mobileSearchInputRef = ref<HTMLInputElement | null>(null);
const playerExpanded = ref(false);
const playerLikeBusy = ref(false);
const showScrollTop = ref(false);

const playerTrackLiked = computed(() => {
  const id = player.currentTrackId;
  const t = player.currentTrack;
  if (!id || !t) return false;
  if (t.likedByMe === true) return true;
  return auth.user ? trackFavorites.has(id) : false;
});

const mobileVolumePopoverOpen = ref(false);
const playerMobileVolumeHostRef = ref<HTMLElement | null>(null);

async function togglePlayerLike(event?: Event) {
  event?.stopPropagation();
  if (!auth.user || !player.currentTrackId || playerLikeBusy.value) return;
  playerLikeBusy.value = true;
  try {
    const res = await api.post<{ likes: number; likedByMe: boolean }>(
      `/tracks/${player.currentTrackId}/like`
    );
    const liked = Boolean(res.data?.likedByMe);
    trackFavorites.setLiked(player.currentTrackId, liked);
    player.patchTrackInQueue(player.currentTrackId, {
      likes: res.data.likes,
      likedByMe: liked,
    });
  } catch (e) {
    console.error(e);
  } finally {
    playerLikeBusy.value = false;
  }
}

function toggleMobileVolumePopover(e: Event) {
  e.stopPropagation();
  mobileVolumePopoverOpen.value = !mobileVolumePopoverOpen.value;
}
const SCROLL_TOP_THRESHOLD = 360;

function updateScrollTopVisibility() {
  showScrollTop.value = window.scrollY > SCROLL_TOP_THRESHOLD;
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

interface TrackSearchItem {
  id: string;
  title: string;
  genre: string | null;
  album: string | null;
  coverUrl: string | null;
  ownerId: string;
  ownerUsername: string;
  playCount: number;
  likes: number;
  createdAt: string;
}
interface ArtistSearchItem {
  id: string;
  username: string;
  bio: string | null;
  avatarUrl: string | null;
  playCount?: number;
}
interface AlbumSearchItem {
  ownerId: string;
  ownerUsername: string;
  album: string;
  coverUrl: string | null;
  trackCount: number;
  playCount?: number;
}
interface PlaylistSearchItem {
  id: string;
  name: string;
  description: string | null;
  createdAt: string;
  trackCount: number;
}

const searchQuery = ref('');
const searchOpen = ref(false);
const searchLoading = ref(false);
const searchResults = ref<{
  tracks: TrackSearchItem[];
  artists: ArtistSearchItem[];
  albums: AlbumSearchItem[];
  playlists: PlaylistSearchItem[];
}>({ tracks: [], artists: [], albums: [], playlists: [] });
let searchDebounce: ReturnType<typeof setTimeout> | null = null;

async function runSearch() {
  const q = searchQuery.value.trim();
  if (!q) {
    searchResults.value = { tracks: [], artists: [], albums: [], playlists: [] };
    return;
  }
  searchLoading.value = true;
  try {
    const [tracksRes, artistsRes, albumsRes, playlistsRes] = await Promise.all([
      api.get<TrackSearchItem[]>('/tracks/search', { params: { q, page: 0, size: 5 } }).catch(() => ({ data: [] })),
      api.get<ArtistSearchItem[]>('/users/search', { params: { q } }).catch(() => ({ data: [] })),
      api.get<AlbumSearchItem[]>('/tracks/search/albums', { params: { q } }).catch(() => ({ data: [] })),
      auth.user ? api.get<PlaylistSearchItem[]>('/playlists', { params: { q } }).catch(() => ({ data: [] })) : Promise.resolve({ data: [] })
    ]);
    searchResults.value = {
      tracks: Array.isArray(tracksRes.data) ? tracksRes.data : [],
      artists: Array.isArray(artistsRes.data) ? artistsRes.data : [],
      albums: Array.isArray(albumsRes.data) ? albumsRes.data : [],
      playlists: Array.isArray(playlistsRes.data) ? playlistsRes.data : []
    };
  } catch (e) {
    console.error(e);
    searchResults.value = { tracks: [], artists: [], albums: [], playlists: [] };
  } finally {
    searchLoading.value = false;
  }
}

function onSearchInput() {
  if (searchQuery.value.trim()) {
    searchOpen.value = true;
  }
  if (searchDebounce) clearTimeout(searchDebounce);
  searchDebounce = setTimeout(() => {
    searchDebounce = null;
    runSearch();
  }, 300);
}

function closeSearch() {
  searchOpen.value = false;
}

function goToTrack(t: TrackSearchItem) {
  player.setQueueAndPlay(searchResults.value.tracks as TrackListItem[], t.id);
  closeSearch();
  searchQuery.value = '';
}

function goToArtist(id: string) {
  router.push(`/artist/${id}`);
  closeSearch();
  searchQuery.value = '';
}

function goToAlbum(a: AlbumSearchItem) {
  router.push(`/artist/${a.ownerId}/album/${encodeURIComponent(a.album)}`);
  closeSearch();
  searchQuery.value = '';
}

function goToPlaylist(id: string) {
  router.push(`/playlists/${id}`);
  closeSearch();
  searchQuery.value = '';
}

const hasSearchResults = computed(() => {
  const r = searchResults.value;
  return r.tracks.length > 0 || r.artists.length > 0 || r.albums.length > 0 || r.playlists.length > 0;
});

function closeDropdownsOnClickOutside(e: MouseEvent) {
  const target = e.target as Node;
  const searchWrapper = document.querySelector('.search-header-wrapper');
  const mobileSearchBar = document.querySelector('.mobile-search-bar');
  const mobileSearchBtn = document.querySelector('.mobile-search-btn');
  const notifWrapper = document.querySelector('.notif-wrapper');
  const userMenu = document.querySelector('.user-menu');
  const insideSearch =
    (searchWrapper && searchWrapper.contains(target)) ||
    (mobileSearchBar && mobileSearchBar.contains(target)) ||
    (mobileSearchBtn && mobileSearchBtn.contains(target));
  if (!insideSearch) {
    searchOpen.value = false;
    mobileSearchOpen.value = false;
  }
  if (notifWrapper && !notifWrapper.contains(target)) {
    notifications.close();
  }
  if (userMenu && !userMenu.contains(target)) {
    showUserMenu.value = false;
  }
  if (
    mobileVolumePopoverOpen.value &&
    playerMobileVolumeHostRef.value &&
    !playerMobileVolumeHostRef.value.contains(target)
  ) {
    mobileVolumePopoverOpen.value = false;
  }
}

function applyThemeFromStorage() {
  const stored = (localStorage.getItem('theme') as 'light' | 'dark' | null) || 'light';
  if (stored === 'dark') {
    document.body.classList.add('dark-theme');
  } else {
    document.body.classList.remove('dark-theme');
  }
}

onMounted(() => {
  auth.hydrateFromStorage();
  if (auth.user) {
    albumLikes.loadLiked();
  }
  applyThemeFromStorage();
  notifications.load();
  if (audioElement.value) {
    player.setAudioElement(audioElement.value);
  }
  document.addEventListener('click', closeDropdownsOnClickOutside);
  window.addEventListener('scroll', updateScrollTopVisibility, { passive: true });
  updateScrollTopVisibility();
});

onBeforeUnmount(() => {
  document.removeEventListener('click', closeDropdownsOnClickOutside);
  window.removeEventListener('scroll', updateScrollTopVisibility);
});

const isUploadDisabled = computed(() => route.name === 'upload');

function toggleUserMenu() {
  if (!auth.user) return;
  showUserMenu.value = !showUserMenu.value;
}

function goToSettings() {
  showUserMenu.value = false;
  router.push('/settings');
}

function goToProfile() {
  showUserMenu.value = false;
  router.push('/profile');
}

function goToFavorites() {
  showUserMenu.value = false;
  router.push('/favorite-artists');
}

function goToMyReleases() {
  showUserMenu.value = false;
  router.push('/my-releases');
}

function goToAdmin() {
  showUserMenu.value = false;
  router.push('/admin');
}

function logout() {
  showUserMenu.value = false;
  mobileMenuOpen.value = false;
  auth.logout();
}

function openMobileSearch() {
  mobileSearchOpen.value = true;
  searchOpen.value = true;
  nextTick(() => mobileSearchInputRef.value?.focus());
}

function closeMobileSearch() {
  searchOpen.value = false;
  mobileSearchOpen.value = false;
}

function closeMobileMenu() {
  mobileMenuOpen.value = false;
}

function navTo(path: string) {
  closeMobileMenu();
  router.push(path);
}

watch(
  () => route.path,
  () => {
    closeMobileMenu();
    closeMobileSearch();
  }
);

watch(playerExpanded, open => {
  if (open) mobileVolumePopoverOpen.value = false;
});

watch(
  () => auth.user?.id,
  userId => {
    if (userId) void trackFavorites.fetchForUser(userId);
    else trackFavorites.clear();
  },
  { immediate: true }
);
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--mobile-search-open': mobileSearchOpen }">
    <header class="app-header">
      <div class="header-left">
        <div class="logo" @click="router.push('/')">
          <div class="logo-mark">
            <div class="logo-wave" />
          </div>
          <span>slapshous</span>
        </div>
        <button
          v-if="!mobileSearchOpen"
          type="button"
          class="mobile-search-btn header-mobile-only"
          aria-label="Поиск"
          aria-expanded="false"
          @click.stop="openMobileSearch"
        >
          <svg class="search-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
        </button>
        <div class="search-header-wrapper header-desktop-only">
          <div class="search-header-input-wrap">
            <svg class="search-header-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
            <input
              v-model="searchQuery"
              type="search"
              class="search-header-input"
              placeholder="Поиск: треки, исполнители, альбомы, плейлисты"
              autocomplete="off"
              @focus="searchOpen = true"
              @input="onSearchInput"
            />
          </div>
          <div
            v-if="searchOpen && (searchQuery.trim() || hasSearchResults)"
            class="search-header-dropdown"
            @click.stop
          >
            <div v-if="searchLoading" class="search-header-loading muted">Поиск...</div>
            <template v-else>
              <div v-if="searchQuery.trim() && !hasSearchResults" class="muted" style="padding: 12px">
                Ничего не найдено.
              </div>
              <template v-else>
                <section v-if="searchResults.tracks.length" class="search-header-section">
                  <div class="search-header-section-title">Треки</div>
                  <div
                    v-for="t in searchResults.tracks"
                    :key="t.id"
                    class="search-header-item"
                    @click="goToTrack(t)"
                  >
                    <div class="search-header-item-cover">
                      <img
                        v-if="t.coverUrl"
                        :src="assetUrl(t.coverUrl)"
                        alt=""
                      />
                      <span v-else class="search-header-item-cover-placeholder">♪</span>
                    </div>
                    <div class="search-header-item-text">
                      <span class="search-header-item-primary">{{ t.title }}</span>
                      <span class="muted">{{ t.ownerUsername }}</span>
                    </div>
                  </div>
                </section>
                <section v-if="searchResults.artists.length" class="search-header-section">
                  <div class="search-header-section-title">Исполнители</div>
                  <div
                    v-for="a in searchResults.artists"
                    :key="a.id"
                    class="search-header-item"
                    @click="goToArtist(a.id)"
                  >
                    <div class="search-header-item-cover search-header-item-avatar">
                      <img
                        :src="assetUrl(`/api/users/${a.id}/avatar`)"
                        alt=""
                        @error="$event.target.style.display = 'none'"
                      />
                      <span v-if="!a.avatarUrl" class="search-header-item-cover-placeholder">{{ (a.username || '?').charAt(0) }}</span>
                    </div>
                    <div class="search-header-item-text">
                      <span class="search-header-item-primary">{{ a.username }}</span>
                      <span v-if="a.playCount != null && a.playCount > 0" class="muted">{{ a.playCount }} прослушиваний</span>
                    </div>
                  </div>
                </section>
                <section v-if="searchResults.albums.length" class="search-header-section">
                  <div class="search-header-section-title">Альбомы</div>
                  <div
                    v-for="(a, i) in searchResults.albums"
                    :key="`${a.ownerId}-${a.album}-${i}`"
                    class="search-header-item"
                    @click="goToAlbum(a)"
                  >
                    <div class="search-header-item-cover">
                      <img
                        v-if="a.coverUrl"
                        :src="assetUrl(a.coverUrl)"
                        alt=""
                      />
                      <span v-else class="search-header-item-cover-placeholder">⌘</span>
                    </div>
                    <div class="search-header-item-text">
                      <span class="search-header-item-primary">{{ a.album }}</span>
                      <span class="muted">{{ a.ownerUsername }} · {{ a.trackCount }} трек(ов)<span v-if="a.playCount != null && a.playCount > 0"> · {{ a.playCount }} прослушиваний</span></span>
                    </div>
                  </div>
                </section>
                <section v-if="searchResults.playlists.length" class="search-header-section">
                  <div class="search-header-section-title">Плейлисты</div>
                  <div
                    v-for="p in searchResults.playlists"
                    :key="p.id"
                    class="search-header-item"
                    @click="goToPlaylist(p.id)"
                  >
                    <div class="search-header-item-cover search-header-item-cover-placeholder">
                      {{ p.name.charAt(0) }}
                    </div>
                    <div class="search-header-item-text">
                      <span class="search-header-item-primary">{{ p.name }}</span>
                      <span class="muted">{{ p.trackCount }} трек(ов)</span>
                    </div>
                  </div>
                </section>
              </template>
            </template>
          </div>
        </div>
      </div>

      <div class="header-center header-desktop-only">
        <nav class="main-nav">
          <button
            type="button"
            class="main-nav-item"
            :class="{ active: route.name === 'home' }"
            @click="router.push('/')"
          >
            Главная
          </button>
          <button
            type="button"
            class="main-nav-item"
            :class="{ active: route.name === 'favorites' || route.name === 'favorites-tracks' }"
            @click="router.push('/favorites')"
          >
            Избранное
          </button>
          <button
            type="button"
            class="main-nav-item"
            :class="{ active: route.name === 'favorite-artists' }"
            @click="router.push('/favorite-artists')"
          >
            Любимые артисты
          </button>
          <button
            type="button"
            class="main-nav-item"
            :class="{ active: route.name === 'friends' }"
            @click="router.push('/friends')"
          >
            Друзья
          </button>
        </nav>
      </div>

      <div class="header-right">
        <button
          type="button"
          class="burger-btn header-mobile-only"
          aria-label="Меню"
          @click="mobileMenuOpen = true"
        >
          <span class="burger-line" />
          <span class="burger-line" />
          <span class="burger-line" />
        </button>
        <div class="auth-header-actions header-desktop-only">
        <div v-if="auth.user" class="notif-wrapper">
          <button
            type="button"
            class="notif-button"
            @click="notifications.toggleOpen"
          >
            <svg class="notif-bell-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
            <span v-if="notifications.unreadCount" class="notif-badge">
              {{ notifications.unreadCount }}
            </span>
          </button>
          <div
            v-if="notifications.open"
            class="notif-dropdown"
            @click.stop
          >
            <div class="notif-dropdown-header">
              <span class="notif-dropdown-title">Уведомления</span>
              <button
                v-if="notifications.items.length && notifications.unreadCount > 0"
                type="button"
                class="notif-mark-all-btn"
                :disabled="notifications.markAllReadLoading"
                title="Пометить все уведомления как прочитанные"
                @click.stop="notifications.markAllRead()"
              >
                {{ notifications.markAllReadLoading ? '…' : 'Прочитать всё' }}
              </button>
            </div>
            <div v-if="!notifications.items.length" class="muted notif-dropdown-empty">
              Уведомлений пока нет.
            </div>
            <div v-else class="notif-list-scroll">
              <div
                v-for="n in notifications.items"
                :key="n.id"
                class="notif-item"
                :class="{ unread: !n.read }"
                @click="notifications.markRead(n.id)"
              >
                <div class="notif-text">{{ n.message }}</div>
                <div class="notif-time">
                  {{ new Date(n.createdAt).toLocaleString() }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="auth.user" class="user-menu header-desktop-only" @click="toggleUserMenu">
          <div class="avatar-wrapper">
            <img
              :src="assetUrl(`/api/users/${auth.user.id}/avatar`)"
              alt="avatar"
              class="avatar-image"
              @error="$event.target.style.display = 'none'"
            />
          </div>
          <span class="user-name">{{ auth.user.username }}</span>
          <div v-if="showUserMenu" class="user-dropdown">
            <button type="button" class="user-dropdown-item" @click.stop="goToSettings">
              Настройки
            </button>
            <button type="button" class="user-dropdown-item" @click.stop="goToProfile">
              Профиль
            </button>
            <button type="button" class="user-dropdown-item" @click.stop="goToMyReleases">
              Мои релизы
            </button>
            <button type="button" class="user-dropdown-item" @click.stop="goToFavorites">
              Избранные
            </button>
            <button
              v-if="auth.user.admin"
              type="button"
              class="user-dropdown-item"
              @click.stop="goToAdmin"
            >
              Админка
            </button>
            <button type="button" class="user-dropdown-item" @click.stop="logout">
              Выйти
            </button>
          </div>
        </div>
        <button v-if="!auth.user" class="secondary-button" @click="router.push('/login')">
          Вход
        </button>
        <button v-if="!auth.user" class="secondary-button" @click="router.push('/register')">
          Регистрация
        </button>
        <button class="primary-button upload-button header-desktop-only" :disabled="isUploadDisabled" @click="router.push('/upload')">
          Выложить трек
        </button>
      </div>
      </div>

      <!-- Мобильный поиск: вторая строка шапки (открывается по лупе) -->
      <div
        v-if="mobileSearchOpen"
        class="mobile-search-bar header-mobile-only"
      >
        <div class="mobile-search-bar-inner">
          <div class="search-header-input-wrap mobile-search-input-wrap">
            <svg class="search-header-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
            <input
              ref="mobileSearchInputRef"
              v-model="searchQuery"
              type="search"
              class="search-header-input"
              placeholder="Поиск: треки, исполнители, альбомы"
              autocomplete="off"
              enterkeyhint="search"
              @input="onSearchInput"
              @focus="searchOpen = true"
            />
          </div>
          <button
            type="button"
            class="mobile-search-close"
            aria-label="Закрыть поиск"
            @click="closeMobileSearch"
          >
            ×
          </button>
        </div>
        <div
          v-if="searchOpen && (searchQuery.trim() || hasSearchResults)"
          class="search-header-dropdown mobile-search-dropdown"
          @click.stop
        >
        <div v-if="searchLoading" class="search-header-loading muted">Поиск...</div>
        <template v-else>
          <div v-if="searchQuery.trim() && !hasSearchResults" class="muted" style="padding: 12px">Ничего не найдено.</div>
          <template v-else>
            <section v-if="searchResults.tracks.length" class="search-header-section">
              <div class="search-header-section-title">Треки</div>
              <div v-for="t in searchResults.tracks" :key="t.id" class="search-header-item" @click="goToTrack(t); closeMobileSearch()">
                <div class="search-header-item-cover">
                  <img v-if="t.coverUrl" :src="assetUrl(t.coverUrl)" alt="" />
                  <span v-else class="search-header-item-cover-placeholder">♪</span>
                </div>
                <div class="search-header-item-text">
                  <span class="search-header-item-primary">{{ t.title }}</span>
                  <span class="muted">{{ t.ownerUsername }}</span>
                </div>
              </div>
            </section>
            <section v-if="searchResults.artists.length" class="search-header-section">
              <div class="search-header-section-title">Исполнители</div>
              <div v-for="a in searchResults.artists" :key="a.id" class="search-header-item" @click="goToArtist(a.id); closeMobileSearch()">
                <div class="search-header-item-cover search-header-item-avatar">
                  <img :src="assetUrl(`/api/users/${a.id}/avatar`)" alt="" @error="$event.target.style.display = 'none'" />
                  <span v-if="!a.avatarUrl" class="search-header-item-cover-placeholder">{{ (a.username || '?').charAt(0) }}</span>
                </div>
                <div class="search-header-item-text">
                  <span class="search-header-item-primary">{{ a.username }}</span>
                </div>
              </div>
            </section>
            <section v-if="searchResults.albums.length" class="search-header-section">
              <div class="search-header-section-title">Альбомы</div>
              <div v-for="(a, i) in searchResults.albums" :key="`${a.ownerId}-${a.album}-${i}`" class="search-header-item" @click="goToAlbum(a); closeMobileSearch()">
                <div class="search-header-item-cover">
                  <img v-if="a.coverUrl" :src="assetUrl(a.coverUrl)" alt="" />
                  <span v-else class="search-header-item-cover-placeholder">⌘</span>
                </div>
                <div class="search-header-item-text">
                  <span class="search-header-item-primary">{{ a.album }}</span>
                  <span class="muted">{{ a.ownerUsername }}</span>
                </div>
              </div>
            </section>
            <section v-if="searchResults.playlists.length" class="search-header-section">
              <div class="search-header-section-title">Плейлисты</div>
              <div v-for="p in searchResults.playlists" :key="p.id" class="search-header-item" @click="goToPlaylist(p.id); closeMobileSearch()">
                <div class="search-header-item-cover search-header-item-cover-placeholder">{{ p.name.charAt(0) }}</div>
                <div class="search-header-item-text">
                  <span class="search-header-item-primary">{{ p.name }}</span>
                </div>
              </div>
            </section>
          </template>
        </template>
        </div>
      </div>
    </header>

    <!-- Бургер-меню (мобильное) -->
    <Teleport to="body">
      <Transition name="burger">
        <div v-if="mobileMenuOpen" class="burger-overlay" @click="closeMobileMenu">
          <div class="burger-drawer" @click.stop>
            <div class="burger-drawer-header">
              <span class="burger-drawer-title">Меню</span>
              <button type="button" class="burger-close" aria-label="Закрыть" @click="closeMobileMenu">×</button>
            </div>
            <nav class="burger-nav">
              <button type="button" class="burger-nav-item" :class="{ active: route.name === 'home' }" @click="navTo('/')">
                Главная
              </button>
              <button type="button" class="burger-nav-item" :class="{ active: route.name === 'favorites' || route.name === 'favorites-tracks' }" @click="navTo('/favorites')">
                Избранное
              </button>
              <button type="button" class="burger-nav-item" :class="{ active: route.name === 'favorite-artists' }" @click="navTo('/favorite-artists')">
                Любимые артисты
              </button>
              <button type="button" class="burger-nav-item" :class="{ active: route.name === 'friends' }" @click="navTo('/friends')">
                Друзья
              </button>
            </nav>
            <div v-if="auth.user" class="burger-user">
              <button type="button" class="burger-profile-btn" @click="navTo('/profile')">
                <div class="avatar-wrapper burger-avatar">
                  <img :src="assetUrl(`/api/users/${auth.user.id}/avatar`)" alt="" class="avatar-image" @error="$event.target.style.display = 'none'" />
                </div>
                <span>{{ auth.user.username }}</span>
              </button>
              <button type="button" class="burger-nav-item" @click="navTo('/settings')">Настройки</button>
              <button type="button" class="burger-nav-item" @click="navTo('/my-releases')">Мои релизы</button>
              <button v-if="auth.user.admin" type="button" class="burger-nav-item" @click="navTo('/admin')">Админка</button>
              <button type="button" class="burger-nav-item" @click="logout">Выйти</button>
            </div>
            <div v-else class="burger-auth">
              <button type="button" class="primary-button burger-upload-btn" @click="navTo('/login')">Вход</button>
              <button type="button" class="secondary-button" @click="navTo('/register')">Регистрация</button>
            </div>
            <button class="primary-button burger-upload-btn full-width" :disabled="isUploadDisabled" @click="navTo('/upload')">
              Выложить трек
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <main class="main-content">
      <RouterView />
    </main>

    <Transition name="scroll-to-top">
      <button
        v-if="showScrollTop"
        type="button"
        class="scroll-to-top-btn"
        :class="{ 'scroll-to-top-btn--above-player': !!player.currentTrack }"
        aria-label="Наверх"
        @click="scrollToTop"
      >
        <svg class="scroll-to-top-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.25" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M12 19V5M5 12l7-7 7 7" />
        </svg>
      </button>
    </Transition>

    <!-- Мини-плеер (одна полоса как в Spotify) -->
    <div
      v-if="player.currentTrack"
      class="player-bar"
      @click="playerExpanded = true"
    >
      <div class="player-bar-progress-top" @click.stop>
        <div class="player-progress-bar player-bar-progress-bar" @click="player.seek">
          <div
            class="player-progress-fill"
            :style="{ width: player.progressPercent + '%', opacity: 0.4 + player.volume * 0.6 }"
          ></div>
        </div>
      </div>

      <div class="player-bar-row">
        <div class="player-bar-left">
          <div v-if="player.currentTrack.coverUrl" class="player-bar-cover">
            <img :src="assetUrl(player.currentTrack.coverUrl)" alt="cover" />
          </div>
          <div v-else class="player-bar-cover player-bar-cover-placeholder">♪</div>
          <div class="player-bar-info">
            <div class="player-bar-title">{{ player.currentTrack.title }}</div>
            <div class="player-bar-meta">{{ player.currentTrack.ownerUsername }}</div>
          </div>
        </div>

        <div class="player-bar-center" @click.stop>
          <button class="player-bar-btn" type="button" @click="player.playPrev" aria-label="Предыдущий">‹‹</button>
          <button class="player-bar-btn player-bar-btn-play" type="button" @click="player.togglePlay" aria-label="Старт/пауза">
            <svg
              v-if="!player.isPlaying"
              class="track-action-play-svg"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path fill="currentColor" d="M8 5.14v13.72L19 12 8 5.14z" />
            </svg>
            <svg v-else class="track-action-play-svg" viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M6 5h4v14H6V5zm8 0h4v14h-4V5z" />
            </svg>
          </button>
          <button
            class="player-bar-btn"
            type="button"
            aria-label="Следующий"
            @click="() => player.playNext()"
          >
            ››
          </button>
        </div>

        <div class="player-bar-right" @click.stop>
          <div class="player-volume-wrap player-volume-desktop-only">
            <div class="player-volume">
              <input
                class="player-volume-slider"
                type="range"
                min="0"
                max="1"
                step="0.01"
                :value="player.volume"
                @input="player.changeVolume"
              />
            </div>
            <div class="player-time player-bar-time">
              <span>{{ player.formatTime(player.currentTime) }}</span>
              <span>{{ player.formatTime(player.duration) }}</span>
            </div>
          </div>

          <div class="player-bar-right-mobile-cluster" @click.stop>
            <div ref="playerMobileVolumeHostRef" class="player-bar-volume-mobile-host">
              <button
                type="button"
                class="player-bar-volume-icon-btn"
                aria-label="Громкость"
                :aria-expanded="mobileVolumePopoverOpen"
                @click="toggleMobileVolumePopover"
              >
                <svg
                  class="player-volume-icon-svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"
                >
                  <path d="M11 5 6 9H2v6h4l5 4V5z" />
                  <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
                  <path d="M19.07 4.93a8 8 0 0 1 0 14.14" />
                </svg>
              </button>
              <div
                v-show="mobileVolumePopoverOpen"
                class="player-bar-volume-flyout"
                @click.stop
              >
                <div class="player-bar-volume-flyout-inner">
                  <input
                    class="player-volume-slider player-volume-slider-flyout player-volume-slider-flyout--vertical"
                    type="range"
                    min="0"
                    max="1"
                    step="0.01"
                    :value="player.volume"
                    @input="player.changeVolume"
                  />
                </div>
              </div>
            </div>
            <button
              v-if="auth.user"
              type="button"
              class="track-action-icon-btn player-like-in-bar player-like-on-mobile"
              :class="{ liked: playerTrackLiked }"
              :disabled="playerLikeBusy"
              aria-label="Лайк"
              @click.stop="togglePlayerLike"
            >
              <svg
                v-if="playerTrackLiked"
                class="player-heart-svg player-heart-svg--filled"
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path
                  fill="currentColor"
                  d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                />
              </svg>
              <svg
                v-else
                class="player-heart-svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path
                  d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                />
              </svg>
            </button>
          </div>

          <button
            v-if="auth.user"
            type="button"
            class="track-action-icon-btn player-like-in-bar player-like-on-desktop"
            :class="{ liked: playerTrackLiked }"
            :disabled="playerLikeBusy"
            aria-label="Лайк"
            @click.stop="togglePlayerLike"
          >
            <svg
              v-if="playerTrackLiked"
              class="player-heart-svg player-heart-svg--filled"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                fill="currentColor"
                d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
              />
            </svg>
            <svg
              v-else
              class="player-heart-svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path
                d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
              />
            </svg>
          </button>
          <button class="player-bar-expand" type="button" aria-label="Развернуть плеер" @click="playerExpanded = true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Развёрнутый плеер (как в Spotify) -->
    <Teleport to="body">
      <Transition name="player-expand">
        <div
          v-if="playerExpanded && player.currentTrack"
          class="player-full-overlay"
          @click.self="playerExpanded = false"
        >
          <div class="player-full">
            <button class="player-full-close" type="button" aria-label="Свернуть" @click="playerExpanded = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>

            <div class="player-full-cover-wrap">
              <div v-if="player.currentTrack.coverUrl" class="player-full-cover">
                <img :src="assetUrl(player.currentTrack.coverUrl)" alt="cover" />
              </div>
              <div v-else class="player-full-cover player-full-cover-placeholder">♪</div>
            </div>

            <div class="player-full-info">
              <div class="player-full-title">{{ player.currentTrack.title }}</div>
              <div class="player-full-meta">{{ player.currentTrack.ownerUsername }}</div>
            </div>

            <div class="player-full-progress" @click="player.seek">
              <div class="player-progress-bar player-full-progress-bar">
                <div
                  class="player-progress-fill"
                  :style="{ width: player.progressPercent + '%', opacity: 0.4 + player.volume * 0.6 }"
                ></div>
              </div>
              <div class="player-time player-full-time">
                <span>{{ player.formatTime(player.currentTime) }}</span>
                <span>{{ player.formatTime(player.duration) }}</span>
              </div>
            </div>

            <div class="player-full-controls">
              <button class="player-bar-btn" type="button" @click="player.playPrev">‹‹</button>
              <button class="player-bar-btn player-bar-btn-play player-full-play" type="button" @click="player.togglePlay" aria-label="Старт/пауза">
                <svg
                  v-if="!player.isPlaying"
                  class="track-action-play-svg"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path fill="currentColor" d="M8 5.14v13.72L19 12 8 5.14z" />
                </svg>
                <svg v-else class="track-action-play-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <path fill="currentColor" d="M6 5h4v14H6V5zm8 0h4v14h-4V5z" />
                </svg>
              </button>
              <button class="player-bar-btn" type="button" @click="() => player.playNext()">››</button>
            </div>

            <div class="player-full-extra">
              <button
                class="player-mode-button"
                type="button"
                @click="player.cyclePlaybackMode"
                :title="player.playbackMode === 'queue' ? 'Поток' : player.playbackMode === 'shuffle' ? 'Шафл' : 'Повтор'"
              >
                <svg v-if="player.playbackMode === 'queue'" class="player-mode-icon" viewBox="0 0 24 24" fill="currentColor"><path d="M4 6h2v2H4V6zm0 5h2v2H4v-2zm0 5h2v2H4v-2zm3-10h10v2H7V6zm0 5h10v2H7v-2zm0 5h10v2H7v-2z"/></svg>
                <svg v-else-if="player.playbackMode === 'shuffle'" class="player-mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5"/></svg>
                <svg v-else class="player-mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 1l4 4-4 4M3 11V9a4 4 0 014-4h14M7 23l-4-4 4-4M21 13v2a4 4 0 01-4 4H3"/></svg>
              </button>
              <div class="player-full-volume-like-row">
                <div class="player-volume player-full-volume">
                  <input
                    class="player-volume-slider"
                    type="range"
                    min="0"
                    max="1"
                    step="0.01"
                    :value="player.volume"
                    @input="player.changeVolume"
                  />
                </div>
                <button
                  v-if="auth.user"
                  type="button"
                  class="track-action-icon-btn player-full-like-btn"
                  :class="{ liked: playerTrackLiked }"
                  :disabled="playerLikeBusy"
                  aria-label="Лайк"
                  @click.stop="togglePlayerLike"
                >
                  <svg
                    v-if="playerTrackLiked"
                    class="player-heart-svg player-heart-svg--filled"
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                  >
                    <path
                      fill="currentColor"
                      d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                    />
                  </svg>
                  <svg
                    v-else
                    class="player-heart-svg"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                  >
                    <path
                      d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                    />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Аудио-элемент всегда в DOM, чтобы стор мог им управлять -->
    <audio
      ref="audioElement"
      style="display: none"
      @loadedmetadata="player.onLoadedMetadata"
      @timeupdate="player.onTimeUpdate"
      @ended="player.onEnded"
    />
  </div>
</template>