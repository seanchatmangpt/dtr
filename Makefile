# DTR — Documentation Testing Runtime
#
# Release flow (Joe Armstrong: make it work, make it right, make it fast):
#
#   make tag VERSION=2.7.0        # bump version, commit, create annotated tag
#   git push origin main          # push commit
#   git push origin v2.7.0        # push tag — GitHub Actions publishes to Maven Central
#
# Local publish (needs GPG key + CENTRAL_USERNAME/CENTRAL_TOKEN in ~/.m2/settings.xml):
#   make publish
#
# See: https://central.sonatype.com/publishing/deployments

MVND := $(shell command -v /opt/mvnd/bin/mvnd 2>/dev/null)
MVN  := $(if $(MVND),$(MVND),./mvnw)

.DEFAULT_GOAL := help
.PHONY: help compile test verify clean install package tag release publish version check

help:
	@echo ""
	@echo "DTR build targets:"
	@echo "  compile           compile all modules"
	@echo "  test              run unit tests"
	@echo "  verify            compile + test + checks"
	@echo "  clean             remove build artifacts"
	@echo "  install           install to local Maven repo (skip tests)"
	@echo "  package           package JARs (skip tests)"
	@echo "  tag VERSION=x.y.z bump pom version, commit, create git tag"
	@echo "  publish           deploy to Maven Central (local, needs creds)"
	@echo "  version           print current project version"
	@echo "  check             verify toolchain (Java, Maven, GPG, Git)"
	@echo ""
	@echo "Release flow:"
	@echo "  make tag VERSION=2.7.0"
	@echo "  git push origin main && git push origin v2.7.0"
	@echo "  # GitHub Actions handles GPG signing and Maven Central publish"
	@echo ""

compile:
	$(MVN) compile

test:
	$(MVN) test

verify:
	$(MVN) verify

clean:
	$(MVN) clean

install:
	$(MVN) install -DskipTests

package:
	$(MVN) package -DskipTests

# Create a release tag.
# Updates pom.xml version, commits, and creates an annotated git tag.
# GitHub Actions will publish to Maven Central when the tag is pushed.
tag:
ifndef VERSION
	$(error VERSION is required: make tag VERSION=x.y.z)
endif
	@echo "==> Setting version to $(VERSION)"
	$(MVN) versions:set -DnewVersion=$(VERSION) -DgenerateBackupPoms=false
	@sed -i 's|<tag>.*</tag>|<tag>v$(VERSION)</tag>|' pom.xml
	git add pom.xml dtr-core/pom.xml dtr-benchmarks/pom.xml 2>/dev/null; git add -u
	git commit -m "Release v$(VERSION)"
	git tag -a v$(VERSION) -m "Release v$(VERSION)"
	@echo ""
	@echo "==> Tagged v$(VERSION). Push with:"
	@echo "    git push origin $$(git branch --show-current) && git push origin v$(VERSION)"

release: tag

# Deploy directly from local machine (CI normally does this via the tag workflow).
# Requires ~/.m2/settings.xml with central server credentials and GPG key loaded.
publish:
	$(MVN) clean deploy -Prelease -DskipTests

version:
	@$(MVN) help:evaluate -Dexpression=project.version -q -DforceStdout

check:
	@echo "==> Java"
	@java -version 2>&1
	@echo "==> Build tool ($(MVN))"
	@$(MVN) --version 2>&1 | head -2
	@echo "==> GPG"
	@gpg --version 2>&1 | head -1
	@echo "==> Git"
	@git --version
	@echo "==> Project version"
	@$(MAKE) -s version
