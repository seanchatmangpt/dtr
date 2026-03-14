## DTR Release — Make is the sole control surface for publishing.
## Usage: make <target>

.PHONY: help version tag release patch minor snapshot

MVN := ./mvnw

# Extract version from pom.xml without noisy Maven output
VERSION := $(shell $(MVN) help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)

help: ## Show available targets
	@awk 'BEGIN{FS=":.*## "} /^[a-zA-Z_-]+:.*## /{printf "  \033[36m%-10s\033[0m %s\n",$$1,$$2}' $(MAKEFILE_LIST)

version: ## Print current project version
	@echo $(VERSION)

tag: ## Create annotated git tag v<VERSION> and push — triggers CI publish to Maven Central
	@echo "Creating tag v$(VERSION)"
	git tag -a "v$(VERSION)" -m "Release v$(VERSION)"
	git push origin "v$(VERSION)"

release: tag ## Alias for tag

patch: ## Bump patch digit (2.6.0 → 2.6.1), commit, tag, and push
	$(eval NEW := $(shell echo "$(VERSION)" | awk -F. '{print $$1"."$$2"."$$3+1}'))
	$(MVN) versions:set -DnewVersion=$(NEW) -DgenerateBackupPoms=false -q
	git add -A
	git commit -m "release: bump to v$(NEW)"
	$(MAKE) VERSION=$(NEW) tag

minor: ## Bump minor digit (2.6.0 → 2.7.0), commit, tag, and push
	$(eval NEW := $(shell echo "$(VERSION)" | awk -F. '{print $$1"."$$2+1".0"}'))
	$(MVN) versions:set -DnewVersion=$(NEW) -DgenerateBackupPoms=false -q
	git add -A
	git commit -m "release: bump to v$(NEW)"
	$(MAKE) VERSION=$(NEW) tag

snapshot: ## Deploy current SNAPSHOT to Maven Central (no tag)
	$(MVN) clean deploy -Prelease -DskipTests --no-transfer-progress
