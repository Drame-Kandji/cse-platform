# ms-chatbot (port 8087, schéma `chatbot`, utilisateur `ms_chatbot`)

Assistant matériel et logistique. Pipeline du diagramme de séquence (Annexe UML, figure 5) :
`ChatbotController` → `ChatbotService` (**verifierRole** → **analyseIntention** → **getContexteData** →
**appliquerFiltreRole** → **genererReponse**) → `HistoriqueService` (conversation + messages + sources).

## API (contrat §10) : tous les rôles internes
| Méthode | Route | Détail |
|---|---|---|
| POST | `/api/chatbot/query` | `{question, conversationId?}` → `Reponse` ; 400 vide / > 1000 car., 404 conversation d'autrui, 429 débit |
| GET | `/api/chatbot/historique?page&size` | mes conversations, plus récentes d'abord |
| GET | `/api/chatbot/historique/{id}` | 404 si inexistante ou d'un autre utilisateur |
| DELETE | `/api/chatbot/historique/{id}` | 204 ; 404 idem |
| GET | `/api/chatbot/suggestions` | questions types selon les rôles (`cse.chatbot.suggestions`) |

## Intentions (classification déterministe, insensible aux accents)
`LOCALISER_ENGIN`, `DISPONIBILITE`, `STATUT_BESOIN`, `STATUT_COMMANDE` (contrats, appels d'offres,
livraisons), `HISTORIQUE_MAINTENANCE`, `PLANNING_AFFECTATION`, `FOURNISSEURS_PAR_CATEGORIE`, `PROCEDURE`,
`AUTRE`. Entités : références `BES-…`, `CTR-…`, `AO-…`, codes d'engins / immatriculations, catégorie et
désignation (vocabulaire BTP), chantier (nom ou code `CH-XXX-00`, « mon chantier »), quantité, statut.

## Données et sécurité
- Lecture seule via `ServiceRestClientFactory` (JWT **de l'utilisateur** relayé : les services appelés
  appliquent leurs droits ; 4xx → aucune donnée) + Resilience4j (retry + disjoncteur par service ; panne →
  section « indisponible », réponse dégradée sans erreur).
- Filtre par rôle (`cse.chatbot.acces`, `cse.chatbot.roles-montants`) : sections non autorisées jamais
  appelées ni affichées ; montants/coûts retirés hors RA, RM, DAF, DG (ex. chef de chantier).
- Anti-injection : texte et données assainis (caractères de contrôle/invisibles, délimiteurs `<<<`/`>>>`),
  blocs `DONNEES` / `QUESTION` délimités, aucun gabarit appliqué au contenu utilisateur, question ≤ 1000
  car., contexte ≤ 6000 car., limitation par utilisateur (seau de jetons en mémoire).

## LLM (optionnel)
Spring AI `ChatClient` sur une API OpenAI-compatible, créé **uniquement** si `LLM_BASE_URL` et `LLM_MODEL`
sont renseignés (pas d'auto-configuration Spring AI : démarrage garanti sans clé). Exemples :
OpenAI `https://api.openai.com`, Mistral `https://api.mistral.ai`, Ollama `http://localhost:11434/v1`
(`LLM_API_KEY` vide). Prompt système versionné : `src/main/resources/prompts/systeme-v1.st`.
Sans LLM ou en cas d'échec : **mode dégradé** (`modeDegrade: true`), réponse française construite à partir
des données (listes markdown par intention) et de la base de procédures `procedures.yml`.

## Variables
`DB_URL`, `DB_PASSWORD`, `JWK_SET_URI`, `JWT_ISSUER_URI`, `MS_SUIVI_URL`, `MS_BESOINS_URL`, `MS_ACHATS_URL`,
`MS_FOURNISSEURS_URL`, `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL`, `LLM_COMPLETIONS_PATH`, `LLM_TIMEOUT`,
`CHATBOT_RATE_CAPACITE`, `CHATBOT_RATE_PERIODE`.

## Build
`cd backend && mvn -o -q -pl ms-chatbot spotless:apply verify`.
