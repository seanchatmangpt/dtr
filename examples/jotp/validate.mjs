#!/usr/bin/env node
/**
 * validate.mjs — Compares ggen-generated files against golden reference files.
 *
 * Usage:
 *   node validate.mjs
 *
 * Exit codes:
 *   0 — all generated files match golden files
 *   1 — one or more files differ or are missing
 *
 * Workflow:
 *   1. Run: ggen sync   (generates files into generated/)
 *   2. Run: node validate.mjs   (compare generated/ vs golden/)
 */

import { readFileSync, existsSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const GOLDEN_DIR    = 'golden/src/test/java/io/github/seanchatmangpt/dtr/test/jotp';
const GENERATED_DIR = 'generated/src/test/java/io/github/seanchatmangpt/dtr/test/jotp';

// ── Collect all golden file paths ────────────────────────────────────────────
function walk(dir) {
  const results = [];
  if (!existsSync(dir)) return results;
  for (const name of readdirSync(dir)) {
    const full = join(dir, name);
    if (statSync(full).isDirectory()) {
      results.push(...walk(full));
    } else {
      results.push(full);
    }
  }
  return results;
}

const goldenFiles = walk(GOLDEN_DIR);

if (goldenFiles.length === 0) {
  console.error(`❌  No golden files found in: ${GOLDEN_DIR}`);
  process.exit(1);
}

let passed = 0;
let failed = 0;

for (const goldenPath of goldenFiles) {
  const relativePath     = relative(GOLDEN_DIR, goldenPath);
  const generatedPath    = join(GENERATED_DIR, relativePath);

  if (!existsSync(generatedPath)) {
    console.error(`❌  MISSING  ${relativePath}`);
    console.error(`    Expected: ${generatedPath}`);
    console.error(`    Run: ggen sync   to generate it.`);
    failed++;
    continue;
  }

  const golden    = readFileSync(goldenPath, 'utf8');
  const generated = readFileSync(generatedPath, 'utf8');

  if (golden === generated) {
    console.log(`✅  ${relativePath}`);
    passed++;
  } else {
    console.error(`❌  MISMATCH  ${relativePath}`);
    // Show first differing line
    const goldenLines    = golden.split('\n');
    const generatedLines = generated.split('\n');
    const maxLines       = Math.max(goldenLines.length, generatedLines.length);
    for (let i = 0; i < maxLines; i++) {
      if (goldenLines[i] !== generatedLines[i]) {
        console.error(`    Line ${i + 1}:`);
        console.error(`    GOLDEN:    ${JSON.stringify(goldenLines[i] ?? '<missing>')}`);
        console.error(`    GENERATED: ${JSON.stringify(generatedLines[i] ?? '<missing>')}`);
        break;
      }
    }
    failed++;
  }
}

console.log('');
console.log(`Results: ${passed} passed, ${failed} failed`);

if (failed > 0) {
  console.error('');
  console.error('To fix mismatches:');
  console.error('  1. Edit ontology/jotp-api.ttl or templates/*.tera');
  console.error('  2. Run: ggen sync');
  console.error('  3. Re-run: node validate.mjs');
  console.error('');
  console.error('To update golden files after intentional template change:');
  console.error('  cp -r generated/ golden/');
  process.exit(1);
}

console.log('✅ All files match golden files');
