import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('./views/Home.vue') },
    { path: '/login', name: 'login', component: () => import('./views/Login.vue') },
    { path: '/register', name: 'register', component: () => import('./views/Register.vue') },
    { path: '/upload', name: 'upload', component: () => import('./views/Upload.vue') },
    { path: '/profile', name: 'profile', component: () => import('./views/Profile.vue') },
    { path: '/profile/edit', name: 'profile-edit', component: () => import('./views/ProfileEdit.vue') },
    { path: '/my-releases', name: 'my-releases', component: () => import('./views/MyReleases.vue') },
    { path: '/admin', name: 'admin', component: () => import('./views/Admin.vue') },
    { path: '/settings', name: 'settings', component: () => import('./views/Settings.vue') },
    { path: '/support', name: 'support', component: () => import('./views/Support.vue') },
    { path: '/favorites', name: 'favorites', component: () => import('./views/Favorites.vue') },
    { path: '/favorites/tracks', name: 'favorites-tracks', component: () => import('./views/FavoritesTracks.vue') },
    { path: '/favorite-artists', name: 'favorite-artists', component: () => import('./views/FavoriteArtists.vue') },
    { path: '/friends', name: 'friends', component: () => import('./views/Friends.vue') },
    { path: '/playlists', name: 'playlists', component: () => import('./views/Playlists.vue') },
    { path: '/playlists/:id', name: 'playlist-detail', component: () => import('./views/PlaylistDetail.vue') },
    { path: '/albums', name: 'albums', component: () => import('./views/Albums.vue') },
    { path: '/artist/:id', name: 'artist', component: () => import('./views/Artist.vue') },
    { path: '/artist/:artistId/album/:albumName', name: 'album', component: () => import('./views/Album.vue') },
    { path: '/artist/:artistId/album/:albumName/edit', name: 'album-edit', component: () => import('./views/AlbumEdit.vue') },
    { path: '/artist/:artistId/track/:trackId/edit', name: 'track-edit', component: () => import('./views/TrackEdit.vue') },
    { path: '/new', name: 'new', component: () => import('./views/Home.vue') }
  ]
});

export default router;
