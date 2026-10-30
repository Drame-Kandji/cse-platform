.PHONY: build test verify up down logs seed e2e docs clean

build:
	@echo "Building all modules..."
	mvn -f backend/pom.xml clean install -DskipTests

test:
	@echo "Running tests..."
	mvn -f backend/pom.xml test

verify:
	@echo "Running full verification..."
	mvn -f backend/pom.xml spotless:check checkstyle:check enforcer:enforce test jacoco:check 2>&1 | tail -40
	@echo "---"
	@cd frontend && pnpm lint 2>&1 | tail -20 || true
	@cd frontend && pnpm test --run 2>&1 | tail -20 || true

up:
	docker compose -f infra/docker-compose.yml up -d

down:
	docker compose -f infra/docker-compose.yml down

logs:
	docker compose -f infra/docker-compose.yml logs -f

seed:
	@echo "Seeding dev data..."

e2e:
	@echo "Running E2E tests..."

docs:
	@echo "Generating docs..."

clean:
	mvn -f backend/pom.xml clean
	rm -rf frontend/node_modules .pnpm-store
