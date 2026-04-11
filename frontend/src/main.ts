import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';

import './style.css';

// После деплоя старый index может тянуть удалённые чанки — Vite шлёт это событие; перезагрузка подтягивает новый манифест.
window.addEventListener('vite:preloadError', () => {
  window.location.reload();
});

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.mount('#app');

