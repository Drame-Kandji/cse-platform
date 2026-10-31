# Plateforme CSE — commandes usuelles. `make verify` est la porte de qualité (backend + frontend).
.PHONY: help build test verify up infra start stop status down front front-verify logs clean

MVN := mvn -f backend/pom.xml
PNPM := pnpm --dir frontend

help:
	@grep -E '^[a-z-]+:.*?## ' $(MAKEFILE_LIST) | awk -F':.*?## ' '{printf "  %-14s %s\n", $$1, $$2}'

build: ## Compile et package tous les microservices (tests inclus)
	$(MVN) -q install

test: ## Tests backend (unitaires, intégration Testcontainers, ArchUnit, snapshots OpenAPI)
	$(MVN) test

verify: ## Porte de qualité complète : format, tests, couverture JaCoCo, puis frontend
	$(MVN) verify
	$(MAKE) front-verify

front-verify: ## Typecheck, lint, tests et build du frontend
	$(PNPM) --filter @cse/web-interne typecheck
	$(PNPM) --filter @cse/web-interne lint
	$(PNPM) --filter @cse/web-interne test
	$(PNPM) --filter @cse/web-interne build

infra: ## Démarre l'infrastructure Docker (PostgreSQL, Kafka, Keycloak, Redis, MailHog)
	scripts/cse.sh infra

start: ## Démarre les microservices sur l'hôte (profil dev)
	scripts/cse.sh start

stop: ## Arrête les microservices
	scripts/cse.sh stop

status: ## État des microservices
	scripts/cse.sh status

front: ## Lance l'application web interne sur http://localhost:5173
	scripts/cse.sh front

up: ## Plateforme complète conteneurisée (images construites)
	docker compose --env-file .env -f infra/docker-compose.yml --profile apps up -d --build

down: ## Arrête tout
	scripts/cse.sh down

logs: ## Journaux de l'infrastructure Docker
	docker compose --env-file .env -f infra/docker-compose.yml logs -f

clean: ## Nettoie les artefacts de build
	$(MVN) -q clean
	rm -rf frontend/apps/web-interne/dist
