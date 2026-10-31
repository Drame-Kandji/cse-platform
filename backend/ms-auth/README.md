# ms-auth : Utilisateurs et rôles (port 8081)

Profils métier des utilisateurs de la plateforme et pilotage de l'API d'administration Keycloak.
Aucun mot de passe n'est stocké (ADR-0012) : Keycloak reste l'unique détenteur des secrets et des
seconds facteurs. Schéma PostgreSQL `auth`, utilisateur `ms_auth`.

## Modèle
Hiérarchie UML (domaine pur, `sn.cse.auth.domain.model`) : `Utilisateur` (abstraite, scellée) →
`ChefDeChantier(chantierIds)`, `ResponsableMateriel(perimetreParc)`, `ResponsableAchats(seuilDelegation)`,
`DAF(budgetGere)`, `DirectionGenerale`, `AdministrateurSysteme`, `Fournisseur(fournisseurId)`.
Persistance JPA `SINGLE_TABLE` (table `utilisateurs`, discriminateur `role`) ; le changement de
rôle réécrit le discriminateur (ADR-0013). Fabrique : `Utilisateurs.creer(role, etat, attributs)`.

## API (contrat `docs/API_CONTRACT.md` §9)
| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/me` | tout utilisateur authentifié (profil provisionné depuis le JWT au premier appel) |
| GET | `/api/admin/utilisateurs?role&actif&q&page&size&sort` | ADMIN_SYSTEME (tri : nom, prenom, email, role, createdAt) |
| GET | `/api/admin/utilisateurs/{id}` | ADMIN_SYSTEME |
| POST | `/api/admin/utilisateurs` | ADMIN_SYSTEME : compte Keycloak (mot de passe temporaire, `UPDATE_PASSWORD`, `CONFIGURE_TOTP` si rôle soumis à l'OTP) + rôle unique + profil |
| PUT | `/api/admin/utilisateurs/{id}` | ADMIN_SYSTEME : identité, rôle, attributs |
| PATCH | `/api/admin/utilisateurs/{id}/statut` | ADMIN_SYSTEME |
| POST | `/api/admin/utilisateurs/{id}/reinitialiser-mot-de-passe` | ADMIN_SYSTEME → 204 |
| GET | `/api/admin/roles` | ADMIN_SYSTEME |

Codes d'erreur spécifiques : `EMAIL_DEJA_UTILISE` (409), `UTILISATEUR_INTROUVABLE` / `COMPTE_INTROUVABLE` (404),
`OPERATION_SUR_SOI_INTERDITE`, `ATTRIBUT_PROFIL_INVALIDE`, `MOT_DE_PASSE_NON_CONFORME`, `IDENTITE_REFUSEE` (422),
`AUCUN_ROLE_CSE` (403), `IDENTITE_INDISPONIBLE` (503, Keycloak injoignable).
Extension compatible du contrat : `fournisseurId?` accepté en création/modification (comptes FOURNISSEUR).

## Événements (topic `cse.auth.events`, payload `UtilisateurPayload`, via l'outbox)
- `UTILISATEUR_CREE` / `UTILISATEUR_MODIFIE` : actions de l'administrateur (création, modification, statut).
- `UTILISATEUR_SYNCHRONISE` : profil importé ou modifié par la synchronisation Keycloak, ou provisionné par `/api/me`.
Chaque événement produit aussi son entrée `AUDIT`.

## Synchronisation Keycloak → profils
Au démarrage (`ApplicationReadyEvent`) puis périodiquement : lecture des membres de chaque rôle CSE,
création/mise à jour idempotente des profils (Keycloak fait foi ; le téléphone reste local),
désactivation des profils dont le compte a disparu. Keycloak injoignable : avertissement et relance
(`relance`), jamais d'échec du démarrage. Un événement n'est publié que si le profil a changé.

## Configuration (`cse.auth.*`)
| Propriété | Variable | Défaut |
|---|---|---|
| `keycloak.url` | `KEYCLOAK_ADMIN_URL` | `http://localhost:8180` |
| `keycloak.realm` / `client-id` | `KEYCLOAK_REALM` / `KEYCLOAK_ADMIN_CLIENT_ID` | `cse` / `cse-admin-api` |
| `keycloak.client-secret` | `KEYCLOAK_ADMIN_CLIENT_SECRET` | vide (profil `dev` : secret du realm de dev) |
| `keycloak.connect-timeout` / `read-timeout` | `KEYCLOAK_ADMIN_CONNECT_TIMEOUT` / `..._READ_TIMEOUT` | `PT2S` / `PT5S` |
| `otp-obligatoire` | `AUTH_OTP_OBLIGATOIRE` | `DAF,DIRECTION_GENERALE,RESPONSABLE_ACHATS,ADMIN_SYSTEME` |
| `synchronisation.au-demarrage` / `relance` | `AUTH_SYNC_AU_DEMARRAGE` / `AUTH_SYNC_RELANCE` | `true` / `PT30S` |
| `synchronisation.periodique` / `intervalle` | `AUTH_SYNC_PERIODIQUE` / `AUTH_SYNC_INTERVALLE` | `true` / `PT15M` |

Le jeton du compte de service (`client_credentials`) est mis en cache jusqu'à 30 s avant expiration
et renouvelé automatiquement sur 401.

## Build et tests
```bash
cd backend && flock /tmp/cse-maven.lock env MAVEN_OPTS=-Xmx768m mvn -o -q -pl ms-auth spotless:apply verify
```
Tests : domaine et application (unitaires, JaCoCo ≥ 80 %), adaptateur Keycloak (WireMock : jeton,
cache, renouvellement, création, rôle unique, préservation des attributs, erreurs), sécurité
`@WebMvcTest` (401/403/400/409/503), une classe `@SpringBootTest` (Testcontainers PostgreSQL + WireMock :
synchronisation idempotente, création + changement de rôle SINGLE_TABLE, compensation, `/api/me`,
instantané OpenAPI `src/test/resources/openapi.json` : régénérer avec `-Dopenapi.snapshot.update=true`),
règles ArchUnit `HexagonalArchitectureRules`.
