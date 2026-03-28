import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface TrackListItem {
  id: string;
  title: string;
  genre: string | null;
  album: string | null;
  ownerId: string;
  ownerUsername: string;
  playCount: number;
  likes: number;
  createdAt: string;
  coverUrl?: string | null;
}

const VOLUME_KEY = 'slapshous_volume';

type PlaybackMode = 'queue' | 'shuffle' | 'repeat';

export const usePlayerStore = defineStore('player', () => {
  const queue = ref<TrackListItem[]>([]);
  const currentTrackId = ref<string | null>(null);
  const audioElement = ref<HTMLAudioElement | null>(null);

  const isPlaying = ref(false);
  const currentTime = ref(0);
  const duration = ref(0);
  const volume = ref(1);
  const playbackMode = ref<PlaybackMode>('queue');

  // Инициализация громкости из localStorage
  const savedVolume = localStorage.getItem(VOLUME_KEY);
  if (savedVolume !== null) {
    const parsed = Number(savedVolume);
    if (!Number.isNaN(parsed)) {
      volume.value = Math.min(1, Math.max(0, parsed));
    }
  }

  const currentTrack = computed(
    () => queue.value.find(t => t.id === currentTrackId.value) || null
  );

  const progressPercent = computed(() => {
    const d = duration.value;
    const t = currentTime.value;
    if (!Number.isFinite(d) || d <= 0 || !Number.isFinite(t) || t < 0) {
      return 0;
    }
    return Math.min(100, (t / d) * 100);
  });

  const currentIndex = computed(() =>
    queue.value.findIndex(t => t.id === currentTrackId.value)
  );

  function setAudioElement(el: HTMLAudioElement | null) {
    audioElement.value = el;
    if (audioElement.value) {
      audioElement.value.volume = volume.value;
    }
  }

  function setQueueAndPlay(tracks: TrackListItem[], trackId: string) {
    queue.value = tracks;
    playTrack(trackId);
  }

  function playTrack(trackId: string) {
    currentTrackId.value = trackId;
    if (!audioElement.value) return;

    const audio = audioElement.value;
    audio.volume = volume.value;
    audio.src = `http://localhost:8080/api/tracks/${trackId}/stream`;
    audio
      .play()
      .then(() => {
        isPlaying.value = true;
      })
      .catch(error => {
        if ((error as any)?.name !== 'AbortError') {
          console.error(error);
        }
      });
  }

  function togglePlay() {
    if (!audioElement.value || !currentTrack.value) return;
    const audio = audioElement.value;

    if (audio.paused) {
      audio
        .play()
        .then(() => {
          isPlaying.value = true;
        })
        .catch(error => {
          if ((error as any)?.name !== 'AbortError') {
            console.error(error);
          }
        });
    } else {
      audio.pause();
      isPlaying.value = false;
    }
  }

  function playByIndex(index: number) {
    if (index < 0 || index >= queue.value.length) return;
    playTrack(queue.value[index].id);
  }

  function playNext() {
    if (!queue.value.length) return;
    const idx = currentIndex.value;
    if (playbackMode.value === 'repeat') {
      if (idx !== -1) {
        playByIndex(idx);
      }
      return;
    }

    let nextIdx: number;
    if (playbackMode.value === 'shuffle') {
      if (queue.value.length === 1) {
        nextIdx = 0;
      } else {
        const otherIndexes = queue.value.map((_, i) => i).filter(i => i !== idx);
        nextIdx = otherIndexes[Math.floor(Math.random() * otherIndexes.length)];
      }
    } else {
      nextIdx = idx === -1 || idx === queue.value.length - 1 ? 0 : idx + 1;
    }
    playByIndex(nextIdx);
  }

  function playPrev() {
    if (!queue.value.length) return;
    const idx = currentIndex.value;
    if (playbackMode.value === 'repeat') {
      if (idx !== -1) {
        playByIndex(idx);
      }
      return;
    }

    let prevIdx: number;
    if (playbackMode.value === 'shuffle') {
      if (queue.value.length === 1) {
        prevIdx = 0;
      } else {
        const otherIndexes = queue.value.map((_, i) => i).filter(i => i !== idx);
        prevIdx = otherIndexes[Math.floor(Math.random() * otherIndexes.length)];
      }
    } else {
      prevIdx = idx <= 0 ? queue.value.length - 1 : idx - 1;
    }
    playByIndex(prevIdx);
  }

  function onLoadedMetadata() {
    if (!audioElement.value) return;
    const d = audioElement.value.duration;
    duration.value = Number.isFinite(d) && d > 0 ? d : 0;
  }

  function onTimeUpdate() {
    if (!audioElement.value) return;
    const t = audioElement.value.currentTime;
    currentTime.value = Number.isFinite(t) && t >= 0 ? t : 0;
  }

  function onEnded() {
    isPlaying.value = false;
    if (playbackMode.value === 'repeat') {
      if (currentIndex.value !== -1) {
        playByIndex(currentIndex.value);
      }
    } else {
      playNext();
    }
  }

  function seek(event: MouseEvent) {
    if (!audioElement.value || !Number.isFinite(duration.value) || duration.value <= 0) return;
    const bar = event.currentTarget as HTMLElement;
    const rect = bar.getBoundingClientRect();
    if (!rect.width) return;
    const rawRatio = (event.clientX - rect.left) / rect.width;
    if (!Number.isFinite(rawRatio)) return;
    const ratio = Math.min(1, Math.max(0, rawRatio));
    const newTime = duration.value * ratio;
    if (!Number.isFinite(newTime)) return;

    audioElement.value.currentTime = newTime;
    currentTime.value = newTime;
  }

  function formatTime(value: number) {
    if (!Number.isFinite(value) || value <= 0) return '0:00';
    const minutes = Math.floor(value / 60);
    const seconds = Math.floor(value % 60)
      .toString()
      .padStart(2, '0');
    return `${minutes}:${seconds}`;
  }

  function changeVolume(event: Event) {
    if (!audioElement.value) return;
    const target = event.target as HTMLInputElement;
    const value = Number(target.value);
    volume.value = value;
    audioElement.value.volume = value;
    localStorage.setItem(VOLUME_KEY, String(value));
  }

  function cyclePlaybackMode() {
    if (playbackMode.value === 'queue') {
      playbackMode.value = 'shuffle';
    } else if (playbackMode.value === 'shuffle') {
      playbackMode.value = 'repeat';
    } else {
      playbackMode.value = 'queue';
    }
  }

  return {
    // state
    queue,
    currentTrackId,
    isPlaying,
    currentTime,
    duration,
    volume,
    currentTrack,
    progressPercent,
    playbackMode,
    // actions
    setAudioElement,
    setQueueAndPlay,
    playTrack,
    togglePlay,
    playNext,
    playPrev,
    onLoadedMetadata,
    onTimeUpdate,
    onEnded,
    seek,
    formatTime,
    changeVolume,
    cyclePlaybackMode
  };
});

