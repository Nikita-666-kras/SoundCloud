/**
 * Прод: пусто — тот же origin, что и SPA (nginx + proxy /api).
 * Дев: пусто + Vite proxy на /api; либо VITE_APP_ORIGIN=http://localhost:8080 без proxy.
 */
export function appOrigin(): string {
  const v = import.meta.env.VITE_APP_ORIGIN as string | undefined;
  return (v ?? '').trim().replace(/\/$/, '');
}

/** База для axios: /api или {origin}/api */
export function getApiBaseUrl(): string {
  const o = appOrigin();
  return o ? `${o}/api` : '/api';
}

/** Абсолютный URL для img/audio: путь вида /api/... */
export function assetUrl(path: string | null | undefined): string {
  if (path == null || path === '') return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  const p = path.startsWith('/') ? path : `/${path}`;
  const o = appOrigin();
  return o ? `${o}${p}` : p;
}
