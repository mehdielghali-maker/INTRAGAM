# ADR 0008 — Authentification par login / mot de passe (mock, SSO différé)

- **Statut** : accepté
- **Date** : 2026-06-25

## Contexte

Jusqu'ici le poste n'avait **aucune authentification** : `GET /api/contexte` hydratait un
profil par défaut, on changeait librement de profil (`POST /api/mock/identite/actif`) et
`/admin` était accessible à tous. Il faut une **vraie page de connexion** : chaque **AGA** se
connecte avec un **login + mot de passe** (gérés en admin à la création du compte), et un
**compte admin** (login `admin`, mot de passe initial `admin`, modifiable, avec adresse de
récupération) administre le tout. L'**auth réelle visée est SSO Microsoft (Entra ID)**, mais
elle sera activée plus tard.

## Décision

1. **Pas de Spring Security complet** (pas de filter chain, pas de réécriture de l'existant) :
   on ajoute uniquement `spring-security-crypto` pour **BCrypt**, et on réutilise le pattern
   **session serveur** déjà en place (HttpSession via `RequestContextHolder`, cf. ADR 0005).
2. L'authentification vit dans le module **`dz.gam.poste.contexte`** (proche des profils et de
   l'identité — évite un cycle inter-contextes). Domaine pur : `AuthentificationService`
   derrière les ports `AuthentifierUseCase` / `ConsulterSessionUseCase` / `GererCompteAdminUseCase`
   et les ports de sortie `MotDePasseEncodeur`, `SessionAuthStore`, `CompteAdminStore`,
   `ComptesAgaStore`. Le mot de passe en clair n'est **jamais** persisté ni relu (hachage
   centralisé dans le store, empreinte BCrypt seule en base).
3. **Deux types d'utilisateur** (`Principal.type`) : **ADMIN** (administration uniquement) et
   **AGA** (espace métier). À la connexion d'un AGA, le **profil actif** de session est fixé
   (le reste de l'app dérive l'agence active du profil, inchangé).
4. **Garde côté back** : `AuthentificationInterceptor` (avant `AccesModuleInterceptor`) exige
   une session authentifiée sur `/api/**` (**401**), sauf `/api/auth/**` ; `/api/admin/**`
   exige le rôle **ADMIN** (**403**). On ne fait jamais confiance au seul masquage du menu.
5. **Compte admin** persisté (`contexte_compte_admin`, ligne unique `admin`), **semé** à
   `admin`/`admin` au 1er démarrage. Mot de passe et **e-mail de récupération** modifiables via
   `/api/admin/compte`. « Mot de passe oublié » renvoie un **indice masqué** de l'adresse
   (envoi d'e-mail réel **différé**, comme le SSO).
6. **Identifiants AGA** : `login` (unique) + `motDePasseHash` ajoutés à `contexte_profil`,
   gérés via `/api/admin/profils` (login requis ; mot de passe requis à la création, facultatif
   en modification = inchangé). Logins de démo semés depuis la config.
7. **« Activer » un profil** devient un **aperçu admin (impersonation)** sous
   `/api/admin/identite/actif` (réservé ADMIN) : l'admin endosse l'AGA le temps de la
   prévisualisation ; il revient à l'administration en se déconnectant.
8. **SSO Microsoft** : bouton présent sur la page de login mais **désactivé**, piloté par
   `poste.auth.sso-microsoft-actif` (faux). `GET /api/auth/config` l'expose au front.

## Conséquences

- **Bascule Entra ID** (ultérieure) : le `Principal` et le périmètre proviendront des claims du
  token ; `AuthentificationInterceptor` et la session restent, le formulaire login/mot de passe
  et le store de mots de passe deviennent un mode de repli. Le domaine ne change pas.
- **Tests** : l'interceptor ne s'exécute que sur un dispatch HTTP réel ; les tests (unitaires +
  4 IT pilotant les use cases) ne sont pas impactés. `AuthentificationServiceTest` couvre les
  cas admin/AGA, mot de passe erroné, login inconnu et changement de mot de passe.
- **Sécurité** : empreintes BCrypt seules en base ; erreurs de connexion indifférenciées
  (401) ; accès admin vérifié côté back (403), pas seulement masqué côté front.
