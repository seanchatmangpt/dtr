.PHONY: help clean build test verify package proxy act-ci act-qual act-pub deploy release cli

MVND  := /opt/mvnd/bin/mvnd
PROXY := -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 \
         -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128 \
         -Dhttp.nonProxyHosts=localhost|127.0.0.1

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

# ── Maven lifecycle ────────────────────────────────────────────────────────────
clean: ## Remove build artifacts
	$(MVND) clean

build: ## Compile + install all modules (skip tests)
	$(MVND) clean install -DskipTests

test: ## Run unit tests across all modules
	$(MVND) test

verify: ## Run full test suite including integration tests
	$(MVND) verify

package: ## Package signed JARs/WAR using release profile (skip tests)
	$(MVND) package -P release -DskipTests

# ── Proxy ──────────────────────────────────────────────────────────────────────
proxy: ## Start local auth proxy on :3128 (fixes Maven Central auth errors)
	python3 maven-proxy-auth.py &

proxy-build: ## Build through local auth proxy
	$(MVND) clean install -DskipTests $(PROXY)

# ── act — local GitHub Actions ─────────────────────────────────────────────────
act-ci: ## Run CI gate workflow locally (requires act)
	act -j build -W .github/workflows/ci-gate.yml

act-qual: ## Run quality gates workflow locally (requires act)
	act -W .github/workflows/quality-gates.yml

act-pub: ## Run publish workflow locally (requires act + .secrets file)
	@test -f .secrets || (echo "ERROR: create .secrets with CENTRAL_USERNAME/CENTRAL_TOKEN/GPG_PRIVATE_KEY/GPG_PASSPHRASE" && exit 1)
	act -W .github/workflows/publish.yml --secret-file .secrets

# ── Maven Central publish ──────────────────────────────────────────────────────
deploy: ## Deploy to Maven Central (needs GPG_PASSPHRASE + CENTRAL_TOKEN + CENTRAL_USERNAME env vars)
	$(MVND) deploy -P release

release: ## Full release: prepare + perform (tags repo, signs, publishes to Maven Central)
	$(MVND) release:prepare release:perform -P release

# ── Python CLI ─────────────────────────────────────────────────────────────────
cli: ## Run dtr-cli tests and linter
	$(MAKE) -C dtr-cli test lint
