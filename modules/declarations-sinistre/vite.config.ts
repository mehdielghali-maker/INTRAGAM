import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

// PWA client autonome de la déclaration de sinistre. Réutilise la couche partagée @decsin.
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      manifest: {
        name: 'Déclaration de sinistre — GAM',
        short_name: 'Sinistre GAM',
        description: 'Déclarez votre sinistre depuis votre téléphone, même hors-ligne.',
        lang: 'fr',
        theme_color: '#0A3D12',
        background_color: '#F8F5EE',
        display: 'standalone',
        start_url: '/',
        icons: [
          { src: '/icone.svg', sizes: '192x192', type: 'image/svg+xml', purpose: 'any' },
          { src: '/icone.svg', sizes: '512x512', type: 'image/svg+xml', purpose: 'any maskable' },
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      '@decsin': fileURLToPath(new URL('../../shared/decsin', import.meta.url)),
      '@sinistre-ui': fileURLToPath(new URL('../../shared/sinistre-ui', import.meta.url)),
    },
  },
  server: { port: 5174 },
  test: { environment: 'jsdom' },
});
