package io.github.seanchatmangpt.dtr.contract;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Verifies and documents interface contract coverage across implementation classes.
 *
 * <p>For each public method in the contract interface, checks whether each
 * implementation class provides a concrete (non-abstract) override or inherits
 * it. Uses only {@link Class#getMethods()} and {@link Class#getDeclaredMethod()}
 * — no external dependencies.</p>
 */
public final class ContractVerifier {

    private ContractVerifier() {}

    /**
     * A single row in the contract verification table.
     *
     * @param methodSig      human-readable method signature
     * @param implStatus     map from implementation simple class name to coverage status
     */
    public record ContractRow(String methodSig, Map<String, String> implStatus) {}

    /**
     * Verifies coverage of all public interface methods across the given implementations.
     *
     * @param contract        the interface to check
     * @param implementations zero or more implementation classes
     * @return list of rows, one per public method in the contract
     */
    public static List<ContractRow> verify(Class<?> contract, Class<?>... implementations) {
        var rows = new ArrayList<ContractRow>();

        var contractMethods = Arrays.stream(contract.getMethods())
                .filter(m -> !m.isDefault())
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toList();

        for (Method cm : contractMethods) {
            String sig = buildSig(cm);
            Map<String, String> implStatus = new LinkedHashMap<>();

            for (Class<?> impl : implementations) {
                implStatus.put(impl.getSimpleName(), checkCoverage(cm, impl));
            }
            rows.add(new ContractRow(sig, implStatus));
        }

        return rows;
    }

    private static String checkCoverage(Method contractMethod, Class<?> impl) {
        try {
            Method implMethod = impl.getMethod(contractMethod.getName(), contractMethod.getParameterTypes());
            if (Modifier.isAbstract(implMethod.getModifiers())) {
                return "abstract";
            }
            // Check if it's directly declared or inherited
            try {
                impl.getDeclaredMethod(contractMethod.getName(), contractMethod.getParameterTypes());
                return "✅ direct";
            } catch (NoSuchMethodException e) {
                return "↗ inherited";
            }
        } catch (NoSuchMethodException e) {
            return "**❌ MISSING**";
        }
    }

    private static String buildSig(Method m) {
        String params = Arrays.stream(m.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return m.getReturnType().getSimpleName() + " " + m.getName() + "(" + params + ")";
    }
}
