#!/bin/bash
# DTR 2026.4.0 Release - Git Tagging Commands
# Execute these commands to complete the release

set -e  # Exit on error

echo "=== DTR 2026.4.0 Release Tagging ==="
echo ""

# Step 1: Verify current state
echo "Step 1: Verifying current commit..."
git log -1
echo ""

# Step 2: Switch to master branch
echo "Step 2: Switching to master branch..."
git checkout master
git pull origin master
echo ""

# Step 3: Merge docs-update branch (if needed)
echo "Step 3: Merging docs-update branch..."
git merge docs-update || echo "Merge may have already been done or there are conflicts"
echo ""

# Step 4: Create the annotated tag
echo "Step 4: Creating annotated tag v2026.4.0..."
git tag -a v2026.4.0 -m "Release v2026.4.0 - DX/QoL Enhancement Release

Key highlights:
- 4 new sayAndAssertThat methods combine assertions with documentation
- 7 new presentation-specific methods (saySlideOnly, sayTweetable, sayTldr, etc.)
- New sayRef overload for convenient cross-references
- JUnit Jupiter 5 → 6 migration complete
- Critical bug fix: DtrContext parameter injection now works correctly
- IDE integration support with LivePreview annotation
- Breaking change: sayCodeModel(Method) → sayMethodSignature(Method)
"
echo ""

# Step 5: Verify the tag
echo "Step 5: Verifying tag was created..."
git tag -l "v2026.4.0"
echo ""
echo "Tag details:"
git show v2026.4.0 --quiet
echo ""

# Step 6: Push the tag (COMMENTED OUT - UNCOMMENT WHEN READY)
echo "Step 6: Push tag to remote..."
echo "⚠️  This will trigger the CI release pipeline!"
read -p "Are you sure you want to push the tag? (yes/no): " confirm

if [ "$confirm" = "yes" ]; then
    echo "Pushing tag v2026.4.0 to origin..."
    git push origin v2026.4.0
    echo ""
    echo "✅ Tag pushed successfully!"
    echo "🚀 CI pipeline triggered at: https://github.com/seanchatmangpt/dtr/actions"
else
    echo "❌ Tag push cancelled. Tag exists locally but not pushed."
    echo "To push manually later, run: git push origin v2026.4.0"
fi

echo ""
echo "=== Release Tagging Complete ==="
