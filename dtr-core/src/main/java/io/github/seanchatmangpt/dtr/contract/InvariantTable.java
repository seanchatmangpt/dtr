package io.github.seanchatmangpt.dtr.contract;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Evaluates named invariants (boolean conditions) and produces a structured
 * table showing which conditions hold and which are violated.
 *
 * <p>Supports pre-conditions, post-conditions, and class invariants.
 */
public final class InvariantTable {

    public enum InvariantKind { PRE, POST, INVARIANT, ASSERT }

    public record InvariantSpec(
        String name,
        InvariantKind kind,
        BooleanSupplier condition
    ) {}

    public record InvariantRow(
        String name,
        String kind,
        boolean holds,
        String symbol   // "✓" or "✗"
    ) {}

    public record InvariantResult(
        List<InvariantRow> rows,
        int passing,
        int failing,
        boolean allPass
    ) {}

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final List<InvariantSpec> specs = new ArrayList<>();

        public Builder pre(String name, BooleanSupplier condition) {
            specs.add(new InvariantSpec(name, InvariantKind.PRE, condition));
            return this;
        }
        public Builder post(String name, BooleanSupplier condition) {
            specs.add(new InvariantSpec(name, InvariantKind.POST, condition));
            return this;
        }
        public Builder invariant(String name, BooleanSupplier condition) {
            specs.add(new InvariantSpec(name, InvariantKind.INVARIANT, condition));
            return this;
        }
        public Builder check(String name, BooleanSupplier condition) {
            specs.add(new InvariantSpec(name, InvariantKind.ASSERT, condition));
            return this;
        }

        public InvariantResult evaluate() {
            List<InvariantRow> rows = new ArrayList<>();
            int passing = 0, failing = 0;
            for (InvariantSpec spec : specs) {
                boolean holds;
                try {
                    holds = spec.condition().getAsBoolean();
                } catch (Exception e) {
                    holds = false;
                }
                if (holds) passing++; else failing++;
                rows.add(new InvariantRow(
                    spec.name(),
                    spec.kind().name().toLowerCase(),
                    holds,
                    holds ? "\u2713" : "\u2717"
                ));
            }
            return new InvariantResult(rows, passing, failing, failing == 0);
        }
    }
}
