import React from 'react';
import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import AppShell from './app/AppShell';
import RequireAuth from './app/RequireAuth';
import LoginPage from './features/auth/LoginPage';
import AccueilAgencePage from './features/accueil/AccueilAgencePage';
import ChequeListPage from './features/cheques/ChequeListPage';
import ChequeDetailPage from './features/cheques/ChequeDetailPage';
import CotationPage from './features/cotation/CotationPage';
import DpdPage from './features/dpd/DpdPage';
import VersementPage from './features/versement/VersementPage';
import AdminProfilsPage from './features/admin/AdminProfilsPage';
import PlaceholderPage from './features/placeholder/PlaceholderPage';
import { ITEMS_SOON } from './app/navigation';
import './index.css';
import './theme/gam.css';

const router = createBrowserRouter([
  // Page de connexion, hors espace protégé.
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    // Tout l'espace applicatif est protégé : RequireAuth redirige vers /login si non connecté
    // et applique la séparation des rôles (admin ↔ AGA).
    element: (
      <RequireAuth>
        <AppShell />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <AccueilAgencePage /> },
      { path: 'cheques', element: <ChequeListPage /> },
      { path: 'cheques/:id', element: <ChequeDetailPage /> },
      { path: 'cotation', element: <CotationPage /> },
      { path: 'versement', element: <VersementPage /> },
      { path: 'accords-echeancier', element: <DpdPage /> },
      { path: 'admin', element: <AdminProfilsPage /> },
      // Fonctions non encore implémentées : page placeholder « à venir ».
      ...ITEMS_SOON.map((item) => ({
        path: item.route.replace(/^\//, ''),
        element: <PlaceholderPage />,
      })),
    ],
  },
]);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
);
