import { createRouter, createWebHistory } from 'vue-router';
import Home from './views/Home.vue';
import Login from './views/Login.vue';
import Register from './views/Register.vue';
import Upload from './views/Upload.vue';
import Profile from './views/Profile.vue';
import ProfileEdit from './views/ProfileEdit.vue';
import Settings from './views/Settings.vue';
import Favorites from './views/Favorites.vue';
import FavoritesTracks from './views/FavoritesTracks.vue';
import Artist from './views/Artist.vue';
import Album from './views/Album.vue';
import AlbumEdit from './views/AlbumEdit.vue';
import TrackEdit from './views/TrackEdit.vue';
import FavoriteArtists from './views/FavoriteArtists.vue';
import MyReleases from './views/MyReleases.vue';
import Admin from './views/Admin.vue';
import Friends from './views/Friends.vue';
import Playlists from './views/Playlists.vue';
import PlaylistDetail from './views/PlaylistDetail.vue';
import Albums from './views/Albums.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: Home },
    { path: '/login', name: 'login', component: Login },
    { path: '/register', name: 'register', component: Register },
    { path: '/upload', name: 'upload', component: Upload },
    { path: '/profile', name: 'profile', component: Profile },
    { path: '/profile/edit', name: 'profile-edit', component: ProfileEdit },
    { path: '/my-releases', name: 'my-releases', component: MyReleases },
    { path: '/admin', name: 'admin', component: Admin },
    { path: '/settings', name: 'settings', component: Settings },
    { path: '/favorites', name: 'favorites', component: Favorites },
    { path: '/favorites/tracks', name: 'favorites-tracks', component: FavoritesTracks },
    { path: '/favorite-artists', name: 'favorite-artists', component: FavoriteArtists },
    { path: '/friends', name: 'friends', component: Friends },
    { path: '/playlists', name: 'playlists', component: Playlists },
    { path: '/playlists/:id', name: 'playlist-detail', component: PlaylistDetail },
    { path: '/albums', name: 'albums', component: Albums },
    { path: '/artist/:id', name: 'artist', component: Artist },
    { path: '/artist/:artistId/album/:albumName', name: 'album', component: Album },
    { path: '/artist/:artistId/album/:albumName/edit', name: 'album-edit', component: AlbumEdit },
    { path: '/artist/:artistId/track/:trackId/edit', name: 'track-edit', component: TrackEdit },
    { path: '/new', name: 'new', component: Home }
  ]
});

export default router;

