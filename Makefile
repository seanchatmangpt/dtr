# DTR — Documentation Testing Runtime
#
# The human decides the type of change. The calendar owns the year.
# The script derives the version number. No human types a version number.
#
# Release decisions:
#   make release-minor     new say* methods, new capabilities, additive changes
#   make release-patch     bug fixes, documentation corrections, dependency updates
#   make release-year      explicit year boundary (if Jan 1 hasn't triggered it)
#
#   make release-rc-minor  bump minor, tag as rc.1, push to GitHub Packages
#   make release-rc-patch  bump patch, tag as rc.1, push to GitHub Packages
#   (then: make release-minor or release-patch to promote RC → final)
#
# release-major does not exist. The year is the major version.
# The calendar owns that decision.

MVND           := /opt/mvnd/bin/mvnd
CURRENT_VERSION := $(shell scripts/current-version.sh)

.DEFAULT_GOAL := help
.PHONY: help compile test verify clean install package snapshot \
        release-minor release-patch release-year \
        release-rc-minor release-rc-patch \
        publish version check \
        build-dtr-javadoc extract-javadoc check-javadoc gen-javadoc-docs

help:
	@echo ""
	@echo "DTR $(CURRENT_VERSION) — build targets:"
	@echo ""
	@echo "  compile            compile all modules"
	@echo "  test               run unit tests"
	@echo "  verify             compile + test + checks (CI gate)"
	@echo "  clean              remove build artifacts"
	@echo "  install            install to local Maven repo (skip tests)"
	@echo "  package            package JARs (skip tests)"
	@echo "  snapshot           deploy SNAPSHOT to remote (no signing)"
	@echo ""
	@echo "Release — decide the type of change, version is derived:"
	@echo ""
	@echo "  release-minor      additive: new capabilities   → next minor"
	@echo "  release-patch      corrective: fixes, deps      → next patch"
	@echo "  release-year       year boundary (explicit)     → YYYY.1.0"
	@echo ""
	@echo "  release-rc-minor   RC: bump minor, push to GitHub Packages"
	@echo "  release-rc-patch   RC: bump patch, push to GitHub Packages"
	@echo "  (promote RC → final with: make release-minor or release-patch)"
	@echo ""
	@echo "  publish            deploy locally (needs GPG + Central creds)"
	@echo "  version            print current project version"
	@echo "  check              verify toolchain (Java, Maven, GPG, Git)"
	@echo ""
	@echo "Javadoc:"
	@echo "  extract-javadoc    extract Javadoc to JSON + generate docs/api/"
	@echo "  check-javadoc      audit mode: exits 0 even if docs missing"
	@echo "  gen-javadoc-docs   generate docs/api/ without TPS check"
	@echo ""
	@echo "Breaking changes: use @Deprecated with min 1-year removal window."
	@echo "The year boundary is the breaking change window, not a major bump."
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

# Snapshot deploy → GitHub Packages, no signing, no Central publish.
# Uses release-rc profile for correct distributionManagement (GitHub Packages).
# Requires GITHUB_TOKEN env var (export GITHUB_TOKEN=<pat> before running).
snapshot:
	$(MVND) clean deploy -Prelease-rc -Dgpg.skip=true --no-transfer-progress

# ─── Final releases — bump, changelog, commit, tag, push → GitHub Actions ───

release-minor:
	scripts/bump.sh minor
	scripts/release.sh

release-patch:
	scripts/bump.sh patch
	scripts/release.sh

release-year:
	scripts/bump.sh year
	scripts/release.sh

# ─── Release candidates → GitHub Packages ────────────────────────────────────

release-rc-minor:
	scripts/bump.sh minor rc
	scripts/release-rc.sh

release-rc-patch:
	scripts/bump.sh patch rc
	scripts/release-rc.sh

# ─── Utilities ───────────────────────────────────────────────────────────────

# Local deploy — requires GPG key and Central credentials in ~/.m2/settings.xml.
# In CI the tag push triggers publish.yml which does this automatically.
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

# ─── Javadoc ─────────────────────────────────────────────────────────────────

build-dtr-javadoc:
	cd scripts/rust/dtr-javadoc && cargo build --release

# Extract Javadoc to JSON + generate docs/api/ markdown.
# Fails the build if any public class/method is missing a Javadoc comment.
extract-javadoc: build-dtr-javadoc
	scripts/rust/dtr-javadoc/target/release/dtr-javadoc \
		--source dtr-core/src/main/java \
		--output docs/meta/javadoc.json \
		--docs docs/api

# Same as extract-javadoc but exits 0 even if docs are missing (CI audit mode).
check-javadoc: build-dtr-javadoc
	scripts/rust/dtr-javadoc/target/release/dtr-javadoc \
		--source dtr-core/src/main/java \
		--output docs/meta/javadoc.json \
		--docs docs/api \
		--allow-missing-docs

# Generate docs/api/ markdown without the TPS violation check.
gen-javadoc-docs: build-dtr-javadoc
	scripts/rust/dtr-javadoc/target/release/dtr-javadoc \
		--source dtr-core/src/main/java \
		--output docs/meta/javadoc.json \
		--docs docs/api \
		--allow-missing-docs
