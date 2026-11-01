# Plateforme CSE : commandes usuelles. `make verify` est la porte de qualité (backend + frontend).
.PHONY: help build test verify up infra start stop status down front portail front-verify logs clean certs backup restore perf images e2e e2e-api e2e-ui

MVN := mvn -f backend/pom.xml
PNPM := pnpm --dir frontend
COMPOSE := docker compose --env-file .env -f infra/docker-compose.yml
K6_IMAGE ?= grafana/k6:0.54.0
PERF_ARGS ?=
E2E_GATEWAY ?= http://localhost:8080
E2E_BASE_URL ?= http://localhost:5173

help:
	@grep -E '^[a-z0-9-]+:.*?## ' $(MAKEFILE_LIST) | awk -F':.*?## ' '{printf "  %-14s %s\n", $$1, $$2}'

build: ## Compile et package tous les microservices (tests inclus)
	$(MVN) -q install

test: ## Tests backend (unitaires, intégration Testcontainers, ArchUnit, snapshots OpenAPI)
	$(MVN) test

verify: ## Porte de qualité complète : format, tests, couverture JaCoCo, puis frontend
	$(MVN) verify
	$(MAKE) front-verify

FRONT_APPS := @cse/web-interne @cse/portail-fournisseur

front-verify: ## Typecheck, lint, tests et build du frontend (web interne + portail fournisseur), gardes charte
	@for app in $(FRONT_APPS); do \
	  $(PNPM) --filter $$app typecheck && $(PNPM) --filter $$app lint && \
	  $(PNPM) --filter $$app test && $(PNPM) --filter $$app build || exit 1; \
	done
	node frontend/scripts/check-no-emoji.mjs
	node frontend/scripts/check-palette.mjs

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

portail: ## Lance le portail fournisseur sur http://localhost:5174
	scripts/cse.sh portail

up: certs ## Plateforme complète conteneurisée (images construites)
	$(COMPOSE) --profile apps up -d --build

images: ## Construit les images (une à la fois : poste à mémoire limitée)
	COMPOSE_PARALLEL_LIMIT=1 $(COMPOSE) --profile apps build

certs: ## PKI de développement dans infra/certs (CA + certificats frontal et services)
	scripts/gen-certs.sh

backup: ## Sauvegarde immédiate (cse + keycloak) dans le volume des sauvegardes ; DEST=<rép> pour l'hôte
	scripts/backup-now.sh $(if $(DEST),--dest $(DEST))

restore: ## Restaure une sauvegarde : make restore DUMP=<fichier.dump|répertoire|AAAAMMJJ-HHMMSS> [BASE=cse] [ARGS=--yes]
	@test -n "$(DUMP)" || { echo "Usage : make restore DUMP=<sauvegarde> [BASE=cse] [ARGS='--globals --yes']"; exit 2; }
	scripts/restore.sh $(DUMP) $(if $(BASE),--base $(BASE)) $(ARGS)

perf: ## Test de charge k6 via la passerelle (PERF_ARGS="-e VUS=200 -e PLATEAU=10m")
	docker run --rm --network host -v "$(CURDIR)/tests/perf/k6:/scripts:ro" $(K6_IMAGE) run $(PERF_ARGS) /scripts/scenario.js

e2e: e2e-api e2e-ui ## Tests E2E des 3 scénarios critiques (pile démarrée : make infra && make start && make front)

e2e-api: ## Tests E2E API RestAssured (scénarios, RBAC, ProblemDetail) contre la passerelle
	mvn -f tests/e2e-api/pom.xml verify -De2e.gateway=$(E2E_GATEWAY)

e2e-ui: ## Tests E2E Playwright (chromium) contre l'application web interne
	E2E_BASE_URL=$(E2E_BASE_URL) E2E_GATEWAY_URL=$(E2E_GATEWAY) $(PNPM) --filter @cse/web-interne test:e2e

down: ## Arrête tout
	scripts/cse.sh down

logs: ## Journaux de l'infrastructure Docker
	$(COMPOSE) logs -f

clean: ## Nettoie les artefacts de build
	$(MVN) -q clean
	rm -rf frontend/apps/web-interne/dist frontend/apps/portail-fournisseur/dist
