import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// В разработке /api проксируется на бэкенд; в Docker то же делает nginx (nginx.conf)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // changeOrigin: false — Host остаётся как у страницы, и для бэкенда запрос same-origin (без CORS)
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: false } },
  },
  // Прототип: один бандл с antd — осознанно, без code splitting
  build: { chunkSizeWarningLimit: 1600 },
});
