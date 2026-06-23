import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Le front appelle le back via /api ; en dev, Vite proxifie vers Spring Boot (8080).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
