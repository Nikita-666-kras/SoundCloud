import { createRouter, createWebHistory } from 'vue-router';

/** Safari / LTE: повтор при сбое загрузки JS-чанка (часто после деплоя или обрыва сети). */
function lazyView(loader: () => Promise<unknown>) {
  return () =>
    loader().catch(async (err) => {
      console.warn('[router] chunk load failed, retry once', err);
      await new Promise((r) => setTimeout(r, 800));
      return loader();
    });
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: lazyView(() => import('./views/Home.vue')) },
    { path: '/login', name: 'login', component: lazyView(() => import('./views/Login.vue')) },
    { path: '/register', name: 'register', component: lazyView(() => import('./views/Register.vue')) },
    { path: '/upload', name: 'upload', component: lazyView(() => import('./views/Upload.vue')) },
    { path: '/profile', name: 'profile', component: lazyView(() => import('./views/Profile.vue')) },
    { path: '/profile/edit', name: 'profile-edit', component: lazyView(() => import('./views/ProfileEdit.vue')) },
    { path: '/my-releases', name: 'my-releases', component: lazyView(() => import('./views/MyReleases.vue')) },
    { path: '/admin', name: 'admin', component: lazyView(() => import('./views/Admin.vue')) },
    { path: '/settings', name: 'settings', component: lazyView(() => import('./views/Settings.vue')) },
    { path: '/support', name: 'support', component: lazyView(() => import('./views/Support.vue')) },
    { path: '/favorites', name: 'favorites', component: lazyView(() => import('./views/Favorites.vue')) },
    { path: '/favorites/tracks', name: 'favorites-tracks', component: lazyView(() => import('./views/FavoritesTracks.vue')) },
    { path: '/favorite-artists', name: 'favorite-artists', component: lazyView(() => import('./views/FavoriteArtists.vue')) },
    { path: '/friends', name: 'friends', component: lazyView(() => import('./views/Friends.vue')) },
    { path: '/playlists', name: 'playlists', component: lazyView(() => import('./views/Playlists.vue')) },
    { path: '/playlists/:id', name: 'playlist-detail', component: lazyView(() => import('./views/PlaylistDetail.vue')) },
    { path: '/albums', name: 'albums', component: lazyView(() => import('./views/Albums.vue')) },
    { path: '/artist/:id', name: 'artist', component: lazyView(() => import('./views/Artist.vue')) },
    { path: '/artist/:artistId/album/:albumName', name: 'album', component: lazyView(() => import('./views/Album.vue')) },
    { path: '/artist/:artistId/album/:albumName/edit', name: 'album-edit', component: lazyView(() => import('./views/AlbumEdit.vue')) },
    { path: '/artist/:artistId/track/:trackId/edit', name: 'track-edit', component: lazyView(() => import('./views/TrackEdit.vue')) },
    { path: '/new', name: 'new', component: lazyView(() => import('./views/Home.vue')) }
  ]
});

export default router;
