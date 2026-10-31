# Plateforme CSE — commandes usuelles. `make verify` est la porte de qualité (backend + frontend).
.PHONY: help build test verify up infra start stop status down front front-verify logs clean certs backup restore perf images

MVN := mvn -f backend/pom.xml
PNPM := pnpm --dir frontend
COMPOSE := docker compose --env-file .env -f infra/docker-compose.yml
K6_IMAGE ?= grafana/k6:0.54.0
PERF_ARGS ?=

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

down: ## Arrête tout
	scripts/cse.sh down

logs: ## Journaux de l'infrastructure Docker
	$(COMPOSE) logs -f

clean: ## Nettoie les artefacts de build
	$(MVN) -q clean
	rm -rf frontend/apps/web-interne/dist
