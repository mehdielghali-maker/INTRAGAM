import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

// Le front appelle le back via /api ; en dev, Vite proxifie vers Spring Boot (8080).
export default defineConfig({
  plugins: [
    react(),
    // Le poste AGA est installable (PWA) et fonctionne hors-ligne après une 1ʳᵉ visite.
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      manifest: {
        name: 'Poste de travail unifié — GAM',
        short_name: 'Poste GAM',
        description: 'Poste de travail unifié GAM (espace AGA).',
        lang: 'fr',
        theme_color: '#0A3D12',
        background_color: '#F8F5EE',
        display: 'standalone',
        start_url: '/',
        icons: [
          { src: '/logo-gam.webp', sizes: '192x192', type: 'image/webp', purpose: 'any' },
          { src: '/logo-gam.webp', sizes: '512x512', type: 'image/webp', purpose: 'any maskable' },
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      // Couche partagée DECSIN (adaptateur unique mutualisé avec la PWA client).
      '@decsin': fileURLToPath(new URL('../shared/decsin', import.meta.url)),
      // Socle de capture partagé (composants React).
      '@sinistre-ui': fileURLToPath(new URL('../shared/sinistre-ui', import.meta.url)),
      // Domaine souscription auto (adaptateur GAM mock↔réel + catalogue de capture).
      '@souscription': fileURLToPath(new URL('../shared/souscription', import.meta.url)),
      // Socle « dossier en cours » (machine à états brouillon/validation + auto-save).
      '@dossier': fileURLToPath(new URL('../shared/dossier', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // allowedHosts : autorise les domaines de TUNNEL HTTPS (dev:tunnel) pour tester sur téléphone.
    allowedHosts: ['.trycloudflare.com', '.ngrok-free.app', '.ngrok.io'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
