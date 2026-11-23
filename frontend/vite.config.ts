import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// En desarrollo, /api, Swagger y Actuator se redirigen a la API de Spring Boot.
// Así el navegador habla con un solo origen, igual que detrás de nginx en Docker.
const apiTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: apiTarget, changeOrigin: true },
      '/actuator': { target: apiTarget, changeOrigin: true },
      '/swagger-ui': { target: apiTarget, changeOrigin: true },
      '/v3/api-docs': { target: apiTarget, changeOrigin: true },
    },
  },
});
