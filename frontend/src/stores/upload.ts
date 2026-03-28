import { defineStore } from 'pinia';

export type UploadMode = 'single' | 'album';

export interface AlbumTrackForm {
  title: string;
  genre: string;
  features: string;
  file: File | null;
}

interface UploadState {
  mode: UploadMode;
  title: string;
  description: string;
  genre: string;
  album: string;
  albumTracks: AlbumTrackForm[];
}

const STORAGE_KEY = 'slapshous_upload_state_v1';

interface StoredAlbumTrack {
  title: string;
  genre: string;
  features: string;
}

interface StoredState {
  mode: UploadMode;
  title: string;
  description: string;
  genre: string;
  album: string;
  albumTracks: StoredAlbumTrack[];
}

export const useUploadStore = defineStore('upload', {
  state: (): UploadState => ({
    mode: 'single',
    title: '',
    description: '',
    genre: '',
    album: '',
    albumTracks: [{ title: '', genre: '', features: '', file: null }]
  }),
  actions: {
    loadFromStorage() {
      try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return;
        const data = JSON.parse(raw) as Partial<StoredState>;
        if (data.mode === 'single' || data.mode === 'album') {
          this.mode = data.mode;
        }
        if (typeof data.title === 'string') this.title = data.title;
        if (typeof data.description === 'string') this.description = data.description;
        if (typeof data.genre === 'string') this.genre = data.genre;
        if (typeof data.album === 'string') this.album = data.album;
        if (Array.isArray(data.albumTracks) && data.albumTracks.length) {
          this.albumTracks = data.albumTracks.map(t => ({
            title: t.title || '',
            genre: t.genre || '',
            features: t.features || '',
            file: null
          }));
        }
      } catch (e) {
        console.error(e);
      }
    },
    saveToStorage() {
      try {
        const serialized: StoredState = {
          mode: this.mode,
          title: this.title,
          description: this.description,
          genre: this.genre,
          album: this.album,
          albumTracks: this.albumTracks.map(t => ({
            title: t.title,
            genre: t.genre,
            features: t.features
          }))
        };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(serialized));
      } catch (e) {
        console.error(e);
      }
    },
    resetAfterUpload() {
      this.mode = 'single';
      this.title = '';
      this.description = '';
      this.genre = '';
      this.album = '';
      this.albumTracks = [{ title: '', genre: '', features: '', file: null }];
      this.saveToStorage();
    }
  }
});

