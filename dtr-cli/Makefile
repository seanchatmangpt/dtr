.PHONY: help install install-dev lint format type-check test coverage clean

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
NC := \033[0m

help:
	@echo "$(BLUE)DocTester CLI — Development Commands$(NC)"
	@echo ""
	@echo "$(GREEN)Installation:$(NC)"
	@echo "  make install          Install CLI in development mode"
	@echo "  make install-dev      Install with development dependencies"
	@echo ""
	@echo "$(GREEN)Code Quality:$(NC)"
	@echo "  make lint             Run Ruff linter"
	@echo "  make format           Format code with Ruff"
	@echo "  make type-check       Run type checking with mypy"
	@echo "  make check            Run all checks (lint, type-check)"
	@echo ""
	@echo "$(GREEN)Testing:$(NC)"
	@echo "  make test             Run pytest"
	@echo "  make coverage         Run pytest with coverage report"
	@echo ""
	@echo "$(GREEN)Maintenance:$(NC)"
	@echo "  make clean            Remove build artifacts"
	@echo "  make lock             Update uv.lock file"

install:
	uv sync

install-dev:
	uv sync --all-extras

lint:
	ruff check dtr_cli tests

format:
	ruff format dtr_cli tests
	ruff check --fix dtr_cli tests

type-check:
	mypy dtr_cli

check: lint type-check
	@echo "$(GREEN)✓ All checks passed$(NC)"

test:
	pytest

coverage:
	pytest --cov=dtr_cli --cov-report=html --cov-report=term-missing

clean:
	rm -rf build/ dist/ *.egg-info
	rm -rf .pytest_cache .mypy_cache .coverage htmlcov
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete

lock:
	uv lock
