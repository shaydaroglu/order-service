.PHONY: help \
		catalog-up catalog-down catalog-logs catalog-test \
		order-up order-down order-logs order-test \
		up down

# ── Defaults ────────────────────────────────────────────────────────────────

CATALOG_DIR := ../catalog-service
ORDER_DIR   := .

# ── Help ────────────────────────────────────────────────────────────────────

help:
	@echo ""
	@echo "  Catalog Service"
	@echo "    make catalog-up      Start catalog service and database"
	@echo "    make catalog-down    Stop catalog service"
	@echo "    make catalog-logs    Tail catalog service logs"
	@echo "    make catalog-test    Run catalog test suite in Docker"
	@echo ""
	@echo "  Order Service"
	@echo "    make order-up        Start order service and database"
	@echo "    make order-down      Stop order service"
	@echo "    make order-logs      Tail order service logs"
	@echo "    make order-test      Run order test suite in Docker"
	@echo ""
	@echo "  Both Services"
	@echo "    make up              Start both services"
	@echo "    make down            Stop both services"
	@echo ""

# ── Catalog ──────────────────────────────────────────────────────────────────

catalog-up:
	docker compose -f $(CATALOG_DIR)/compose.yml up --build -d

catalog-down:
	docker compose -f $(CATALOG_DIR)/compose.yml down

catalog-logs:
	docker compose -f $(CATALOG_DIR)/compose.yml logs -f catalog-service

catalog-test:
	docker compose -f $(CATALOG_DIR)/compose.yml --profile test run --rm catalog-test

# ── Order ────────────────────────────────────────────────────────────────────

order-up: catalog-up
	docker compose -f $(ORDER_DIR)/compose.yml up --build -d

order-down:
	docker compose -f $(ORDER_DIR)/compose.yml down

order-logs:
	docker compose -f $(ORDER_DIR)/compose.yml logs -f order-service

order-test:
	docker compose -f $(ORDER_DIR)/compose.yml --profile test run --rm order-test

# ── Both ─────────────────────────────────────────────────────────────────────

up: catalog-up order-up

down:
	docker compose -f $(ORDER_DIR)/compose.yml down
	docker compose -f $(CATALOG_DIR)/compose.yml down
