import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

// PWA agent autonome de souscription auto. Réutilise le socle @sinistre-ui + le domaine @souscription.
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      manifest: {
        name: 'Souscription auto — GAM',
        short_name: 'Souscription GAM',
        description: 'Souscrivez un contrat auto sur le terrain, même hors-ligne.',
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
      '@souscription': fileURLToPath(new URL('../../shared/souscription', import.meta.url)),
      '@sinistre-ui': fileURLToPath(new URL('../../shared/sinistre-ui', import.meta.url)),
    },
  },
  server: { port: 5175 },
  test: { environment: 'jsdom' },
});
