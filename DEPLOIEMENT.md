# Déploiement — SIRH (Docker)

Ce document explique comment lancer l'application SIRH complète (Postgres + Redis + backend Spring Boot + frontend React/Nginx) avec Docker, de A à Z.

## Prérequis

- Docker Desktop (ou Docker Engine + Docker Compose plugin) installé et démarré.
- Les deux dépôts clonés **en dossiers frères**, au même niveau :

```
workespace/
├── sirh-project-backend/
└── sirh-project-frontend/
```

Chaque dépôt possède son propre `docker-compose.yml`, capable de démarrer **toute la pile à lui seul** en construisant l'autre service depuis `../sirh-project-frontend` ou `../sirh-project-backend`. Les deux fichiers sont maintenus identiques.

⚠️ **Choisissez un point d'entrée et gardez-le** : le volume `./storage` (contrats, justificatifs, rapports exportés) est un montage local relatif au dossier depuis lequel `docker compose` est lancé. Si vous lancez tantôt depuis `sirh-project-backend/`, tantôt depuis `sirh-project-frontend/`, vous obtiendrez deux dossiers `storage/` distincts. Recommandation : lancez toujours depuis **`sirh-project-backend/`**.

## Démarrage rapide

Depuis `sirh-project-backend/` :

```bash
docker compose up --build -d
```

Cette commande construit les images (backend : build Maven multi-étapes puis JRE Alpine ; frontend : build Vite puis Nginx Alpine) et démarre les 4 conteneurs dans l'ordre correct (`postgres`/`redis` doivent être *healthy* avant `backend`, qui doit être *healthy* avant `frontend`).

Vérifier que tout est démarré :

```bash
docker compose ps
```

Les 4 services doivent afficher `Up` (et `healthy` pour `postgres`, `redis`, `backend`).

## Accès

| Service | URL | Identifiants de dev |
|---|---|---|
| Application (frontend) | http://localhost:3000 | `admin` / `ChangeMe123!` (compte SUPERUSER seedé par Flyway — **ne jamais utiliser en production**, cf. `CLAUDE.md`) |
| API backend (directe, debug) | http://localhost:8080 | — |
| Healthcheck backend | http://localhost:8080/actuator/health | — |

Le frontend (Nginx) sert les fichiers statiques sur `/` et reverse-proxy tout appel `/api/*` vers le conteneur `backend:8080` (voir `nginx.conf`), en retirant le préfixe `/api` — c'est la même convention que le proxy Vite utilisé en développement (`vite.config.ts`).

## Variables d'environnement

Toutes ont une valeur par défaut fonctionnelle pour un environnement de démo/dev — **à surcharger obligatoirement en production** via un fichier `.env` (non commité) à côté du `docker-compose.yml`, ou via de vraies variables d'environnement/secrets de votre plateforme de déploiement :

| Variable | Défaut | Description |
|---|---|---|
| `POSTGRES_DB` | `sirh` | Nom de la base |
| `POSTGRES_USER` | `sirh` | Utilisateur Postgres |
| `POSTGRES_PASSWORD` | `sirh` | ⚠️ à changer en production |
| `JWT_SECRET` | `change-me-in-production` | ⚠️ à changer en production — signature des access tokens |
| `JWT_REFRESH_SECRET` | `change-me-too-in-production` | ⚠️ à changer en production — signature des refresh tokens |
| `SPRING_PROFILES_ACTIVE` | `docker` | Profil Spring actif |

Exemple de fichier `.env` :

```env
POSTGRES_PASSWORD=un-mot-de-passe-solide
JWT_SECRET=une-cle-aleatoire-longue
JWT_REFRESH_SECRET=une-autre-cle-aleatoire-longue
```

## Persistance des données

- **Postgres** : volume nommé `postgres_data` (survit à `docker compose down`, supprimé uniquement par `docker compose down -v`).
- **Fichiers** (contrats, justificatifs, rapports exportés) : montage local `./storage` → `/app/storage` dans le conteneur `backend` — cf. avertissement sur le point d'entrée ci-dessus.

## Commandes utiles

```bash
# Logs en direct d'un service
docker compose logs -f backend

# Redémarrer un seul service après un changement de code
docker compose up --build -d backend

# Tout arrêter (conserve les volumes)
docker compose down

# Tout arrêter et supprimer les données (reset complet)
docker compose down -v
```

## Dépannage

- **Le backend reste `unhealthy`** : `docker compose logs backend` — le plus souvent la base n'a pas fini ses migrations Flyway ou `postgres` n'était pas encore prêt (le healthcheck `depends_on: condition: service_healthy` gère normalement ce cas).
- **Le frontend affiche une page blanche ou des 404 sur `/api/*`** : vérifier que `nginx.conf` proxifie bien vers `backend:8080` (nom du service Docker, pas `localhost`) et que le conteneur `backend` est sain.
- **Port déjà utilisé (`5432`, `6379`, `8080` ou `3000`)** : un Postgres/Redis local ou un `mvn spring-boot:run`/`npm run dev` tourne déjà sur ces ports — arrêtez-les ou adaptez le mapping de ports dans `docker-compose.yml`.
