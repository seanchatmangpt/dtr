.PHONY: build-dtr-javadoc extract-javadoc

build-dtr-javadoc:
	cd scripts/rust/dtr-javadoc && cargo build --release

extract-javadoc: build-dtr-javadoc
	scripts/rust/dtr-javadoc/target/release/dtr-javadoc \
		--source dtr-core/src/main/java \
		--output docs/meta/javadoc.json
