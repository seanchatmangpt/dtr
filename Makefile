# DTR — Documentation Testing Runtime
#
# The human decides the type of change. The number is derived.
#
#   make release-patch   → bug fix, no API change
#   make release-minor   → new say* methods, backward compatible
#   make release-major   → breaking API change
#
# That is the only decision a human needs to make.
# scripts/bump-version.sh owns the arithmetic.
# scripts/release.sh owns the tag and push.
# GitHub Actions owns the signing and Maven Central publish.

MVND           := /opt/mvnd/bin/mvnd
CURRENT_VERSION := $(shell scripts/current-version.sh)

.DEFAULT_GOAL := help
.PHONY: help compile test verify clean install package \
        release-major release-minor release-patch \
        publish version check

help:
	@echo ""
	@echo "DTR build targets:"
	@echo ""
	@echo "  compile          compile all modules"
	@echo "  test             run unit tests"
	@echo "  verify           compile + test + checks (CI gate)"
	@echo "  clean            remove build artifacts"
	@echo "  install          install to local Maven repo (skip tests)"
	@echo "  package          package JARs (skip tests)"
	@echo ""
	@echo "Release — decide the change type, the version is derived:"
	@echo ""
	@echo "  release-patch    bug fix, no API change     ($(CURRENT_VERSION) → next patch)"
	@echo "  release-minor    new features, compatible   ($(CURRENT_VERSION) → next minor)"
	@echo "  release-major    breaking API change        ($(CURRENT_VERSION) → next major)"
	@echo ""
	@echo "  publish          deploy to Maven Central locally (needs creds)"
	@echo "  version          print current project version"
	@echo "  check            verify toolchain (Java, Maven, GPG, Git)"
	@echo ""

compile:
	$(MVND) compile

test:
	$(MVND) test

verify:
	$(MVND) verify

clean:
	$(MVND) clean

install:
	$(MVND) install -DskipTests

package:
	$(MVND) package -DskipTests

release-major:
	scripts/bump-version.sh major $(CURRENT_VERSION)
	scripts/release.sh

release-minor:
	scripts/bump-version.sh minor $(CURRENT_VERSION)
	scripts/release.sh

release-patch:
	scripts/bump-version.sh patch $(CURRENT_VERSION)
	scripts/release.sh

# Local deploy — for developers who have GPG + Central credentials configured.
# In CI, the tag push triggers publish.yml which does this automatically.
publish:
	$(MVND) clean deploy -Prelease -DskipTests

version:
	@scripts/current-version.sh

check:
	@echo "==> Java"
	@java -version 2>&1
	@echo "==> mvnd"
	@$(MVND) --version 2>&1 | head -2
	@echo "==> GPG"
	@gpg --version 2>&1 | head -1
	@echo "==> Git"
	@git --version
	@echo "==> Current version"
	@scripts/current-version.sh
