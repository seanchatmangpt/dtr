## DTR Release — Make is the sole control surface for publishing.
## The human decides the type of change. The number is derived.

.PHONY: help release-major release-minor release-patch snapshot version

CURRENT_VERSION := $(shell scripts/current-version.sh)

help: ## Show available targets
	@awk 'BEGIN{FS=":.*## "} /^[a-zA-Z_-]+:.*## /{printf "  \033[36m%-16s\033[0m %s\n",$$1,$$2}' $(MAKEFILE_LIST)

release-major: ## Breaking API change → bumps X.0.0
	scripts/bump-version.sh major $(CURRENT_VERSION)
	scripts/release.sh

release-minor: ## New backward-compatible features → bumps x.Y.0
	scripts/bump-version.sh minor $(CURRENT_VERSION)
	scripts/release.sh

release-patch: ## Bug fix, no API change → bumps x.y.Z
	scripts/bump-version.sh patch $(CURRENT_VERSION)
	scripts/release.sh

snapshot: ## Deploy current SNAPSHOT to Maven Central (no tag, no release)
	./mvnw clean deploy -Prelease -DskipTests --no-transfer-progress

version: ## Print current version
	@echo $(CURRENT_VERSION)
