package io.github.seanchatmangpt.dtr.coverage;

/**
 * A single row in a documentation coverage report.
 *
 * @param methodSig  the method signature (e.g., "void sayTable(String[][])")
 * @param documented whether this method was documented during the test
 * @param via        the say* method used to document it, or "—" if not documented
 */
public record CoverageRow(String methodSig, boolean documented, String via) {}
