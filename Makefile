.PHONY: release-minor release-patch release-year \
        release-rc-minor release-rc-patch \
        snapshot version tag \
        build-dtr-javadoc extract-javadoc check-javadoc gen-javadoc-docs

# ─── Release Commands ─────────────────────────────────────────────────────────
# The human decides the type of change. The version number is derived.
# Never type a version number. Never run mvn deploy directly.
# All paths flow through scripts/bump.sh → scripts/release.sh → GitHub Actions.

release-minor:
	scripts/bump.sh minor
	scripts/release.sh

release-patch:
	scripts/bump.sh patch
	scripts/release.sh

release-year:
	scripts/bump.sh year
	scripts/release.sh

release-rc-minor:
	scripts/bump.sh minor rc
	scripts/release-rc.sh

release-rc-patch:
	scripts/bump.sh patch rc
	scripts/release-rc.sh

snapshot:
	./mvnw clean deploy --no-transfer-progress

version:
	@scripts/current-version.sh

tag:
	scripts/release.sh

# ─── Javadoc ──────────────────────────────────────────────────────────────────

build-dtr-javadoc:
	cd scripts/rust/dtr-javadoc && cargo build --release

# Extract Javadoc to JSON + generate docs/api/ markdown.
# TPS: fails the build if any public class/method is missing a Javadoc comment.
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
