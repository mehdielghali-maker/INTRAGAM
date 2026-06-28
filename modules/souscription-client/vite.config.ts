import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

// PWA CLIENTE de souscription auto : le client remplit à distance (lien + OTP), hors-ligne possible.
// Réutilise le socle @sinistre-ui + le domaine @souscription + le socle brouillon @dossier.
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      manifest: {
        name: 'Souscription auto — GAM (client)',
        short_name: 'Souscription GAM',
        description: 'Finalisez votre souscription auto depuis votre téléphone, même hors-ligne.',
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
      '@dossier': fileURLToPath(new URL('../../shared/dossier', import.meta.url)),
    },
  },
  server: { port: 5176 },
  test: { environment: 'jsdom' },
});
