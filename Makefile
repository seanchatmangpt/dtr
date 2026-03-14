.PHONY: build-dtr-javadoc extract-javadoc check-javadoc gen-javadoc-docs

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
