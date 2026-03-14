## DTR Release — Make is the sole control surface for publishing.
## The human decides the type of change. The version number is derived.
## CalVer scheme: YYYY.MINOR.PATCH

.PHONY: help release-minor release-patch release-year \
        release-rc-minor release-rc-patch snapshot version

CURRENT_VERSION := $(shell scripts/current-version.sh)

help: ## Show available targets
	@awk 'BEGIN{FS=":.*## "} /^[a-zA-Z_-]+:.*## /{printf "  \033[36m%-20s\033[0m %s\n",$$1,$$2}' $(MAKEFILE_LIST)

## ── Final Releases ────────────────────────────────────────────────────────────
## Triggers: mvnd verify → Maven Central deploy → GitHub Release

release-minor: ## New say* methods, additive features → bumps YYYY.(N+1).0
	scripts/bump.sh minor
	scripts/release.sh

release-patch: ## Bug fix, no API change → bumps YYYY.MINOR.(N+1)
	scripts/bump.sh patch
	scripts/release.sh

release-year: ## Explicit year boundary → YYYY.1.0 (use in January)
	scripts/bump.sh year
	scripts/release.sh

## ── Release Candidates ────────────────────────────────────────────────────────
## Triggers: mvnd verify → GitHub Packages deploy (NOT Maven Central)

release-rc-minor: ## RC for a minor bump → YYYY.(N+1).0-rc.N
	scripts/bump.sh minor rc
	scripts/release-rc.sh

release-rc-patch: ## RC for a patch fix → YYYY.MINOR.(N+1)-rc.N
	scripts/bump.sh patch rc
	scripts/release-rc.sh

## ── Utilities ─────────────────────────────────────────────────────────────────

snapshot: ## Deploy current SNAPSHOT to Maven Central (no tag, no release)
	./mvnw clean deploy -Prelease -DskipTests --no-transfer-progress

version: ## Print current version
	@echo $(CURRENT_VERSION)
