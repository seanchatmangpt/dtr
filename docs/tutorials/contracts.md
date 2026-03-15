# Tutorial 6: Contract Verification

**Duration**: 25 minutes | **Prerequisites**: Completed Tutorial 5 | **Level**: Intermediate

---

## What You'll Learn

By the end of this tutorial, you'll be able to:

- Document interface implementation coverage across multiple classes
- Detect contract violations (missing methods) automatically
- Use sealed interfaces for automatic subclass detection
- Verify plugin architectures and strategy patterns
- Generate implementation compliance reports

---

## What is Contract Verification?

Contract verification answers the question: *"Do all my implementations actually fulfill their interface contract?"*

In real-world systems, you often have:

- **Plugin systems**: Multiple classes implementing a common interface
- **Service providers**: Different vendors implementing the same API
- **Strategy patterns**: Interchangeable algorithms with a shared contract
- **Sealed hierarchies**: Compile-time known implementations

DTR's `sayContractVerification` automatically inspects all specified implementations and reports which interface methods each class implements — detecting violations before they reach production.

---

## Basic Contract Verification

### Step 1: Define Your Interface Contract

Create a `PaymentProcessor` interface with clear contract methods:

```java
// src/main/java/com/example/payments/PaymentProcessor.java
package com.example.payments;

import java.math.BigDecimal;

public interface PaymentProcessor {
    String processPayment(BigDecimal amount);
    void refund(String transactionId);
    boolean validateTransaction(String transactionId);
}
```

### Step 2: Implement the Contract

Create multiple implementations (some complete, some incomplete):

```java
// src/main/java/com/example/payments/StripeProcessor.java
package com.example.payments;

import java.math.BigDecimal;

public class StripeProcessor implements PaymentProcessor {
    @Override
    public String processPayment(BigDecimal amount) {
        return "stripe-" + System.currentTimeMillis();
    }

    @Override
    public void refund(String transactionId) {
        System.out.println("Refunding Stripe transaction: " + transactionId);
    }

    @Override
    public boolean validateTransaction(String transactionId) {
        return transactionId != null && transactionId.startsWith("stripe-");
    }
}
```

```java
// src/main/java/com/example/payments/PayPalProcessor.java
package com.example.payments;

import java.math.BigDecimal;

public class PayPalProcessor implements PaymentProcessor {
    @Override
    public String processPayment(BigDecimal amount) {
        return "paypal-" + System.currentTimeMillis();
    }

    @Override
    public void refund(String transactionId) {
        System.out.println("Refunding PayPal transaction: " + transactionId);
    }

    // ❌ MISSING: validateTransaction not implemented
    // This is a contract violation!
}
```

```java
// src/main/java/com/example/payments/SquareProcessor.java
package com.example.payments;

import java.math.BigDecimal;

public class SquareProcessor extends AbstractPaymentProcessor {
    @Override
    public String processPayment(BigDecimal amount) {
        return "square-" + System.currentTimeMillis();
    }

    // Inherits refund() from AbstractPaymentProcessor
    // Inherits validateTransaction() from AbstractPaymentProcessor
}
```

```java
// src/main/java/com/example/payments/AbstractPaymentProcessor.java
package com.example.payments;

public abstract class AbstractPaymentProcessor implements PaymentProcessor {
    @Override
    public void refund(String transactionId) {
        System.out.println("Generic refund: " + transactionId);
    }

    @Override
    public boolean validateTransaction(String transactionId) {
        return transactionId != null && transactionId.contains("-");
    }
}
```

### Step 3: Document Contract Compliance

Create a test that verifies all implementations:

```java
// src/test/java/com/example/payments/PaymentContractTest.java
package com.example.payments;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import io.github.seanchatmangpt.dtr.api.DtrTest;

public class PaymentContractTest extends DtrTest {

    @Test
    void documentPaymentContract() {
        sayNextSection("Payment Processor Contract Verification");

        say("""
            All payment processors must implement the `PaymentProcessor` contract.
            This table verifies that each implementation provides all required methods.
            """);

        // Verify contract compliance across all implementations
        sayContractVerification(
            PaymentProcessor.class,
            StripeProcessor.class,
            PayPalProcessor.class,
            SquareProcessor.class
        );

        // Document the violation
        sayWarning("""
            **PayPalProcessor is missing `validateTransaction`** — this is a contract
            violation that will cause runtime errors when validation is attempted.
            """);

        // Assert that we have at least 3 implementations
        sayAndAssertThat(
            "Implementation Count",
            new Class<?>[] {
                StripeProcessor.class,
                PayPalProcessor.class,
                SquareProcessor.class
            }.length,
            greaterThanOrEqualTo(3)
        );
    }
}
```

### Step 4: Run the Test

```bash
mvnd test -Dtest=PaymentContractTest
```

**Generated Documentation** (`target/docs/PaymentContractTest.md`):

```markdown
# Payment Processor Contract Verification

All payment processors must implement the `PaymentProcessor` contract.
This table verifies that each implementation provides all required methods.

| Method | StripeProcessor | PayPalProcessor | SquareProcessor |
|--------|----------------|-----------------|-----------------|
| processPayment | ✓ direct | ✓ direct | ✓ direct |
| refund | ✓ direct | ✓ direct | ↗ inherited |
| validateTransaction | ✓ direct | ❌ MISSING | ↗ inherited |

> [!WARNING]
> **PayPalProcessor is missing `validateTransaction`** — this is a contract
> violation that will cause runtime errors when validation is attempted.

**Implementation Count**: 3 implementations found
```

---

## Understanding the Output

The contract verification table uses three symbols:

### ✓ Direct Implementation

The class explicitly implements the method itself:

```java
public class StripeProcessor implements PaymentProcessor {
    @Override
    public String processPayment(BigDecimal amount) {
        return "stripe-" + System.currentTimeMillis();
    }
}
```

**Result**: `processPayment` → `✓ direct` for `StripeProcessor`

### ↗ Inherited Implementation

The class inherits the method from a superclass:

```java
public abstract class AbstractPaymentProcessor implements PaymentProcessor {
    @Override
    public void refund(String transactionId) {
        System.out.println("Generic refund: " + transactionId);
    }
}

public class SquareProcessor extends AbstractPaymentProcessor {
    // refund() is inherited from AbstractPaymentProcessor
}
```

**Result**: `refund` → `↗ inherited` for `SquareProcessor`

### ❌ MISSING (Contract Violation!)

The class doesn't have the method at all:

```java
public class PayPalProcessor implements PaymentProcessor {
    @Override
    public String processPayment(BigDecimal amount) { /* ... */ }

    @Override
    public void refund(String transactionId) { /* ... */ }

    // ❌ validateTransaction is not implemented
    // This will cause NoSuchMethodError at runtime!
}
```

**Result**: `validateTransaction` → `❌ MISSING` for `PayPalProcessor`

---

## Sealed Interface Automation

Java 21+ sealed interfaces let DTR automatically detect all permitted subclasses:

### Step 1: Define a Sealed Interface

```java
// src/main/java/com/example/export/DataExporter.java
package com.example.export;

public sealed interface DataExporter permits
    JsonExporter,
    XmlExporter,
    CsvExporter {

    byte[] exportData(String data);
    String getMimeType();
}
```

### Step 2: Implement Permitted Subclasses

```java
// src/main/java/com/example/export/JsonExporter.java
package com.example.export;

public final class JsonExporter implements DataExporter {
    @Override
    public byte[] exportData(String data) {
        return ("{" + data + "}").getBytes();
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }
}
```

```java
// src/main/java/com/example/export/XmlExporter.java
package com.example.export;

public final class XmlExporter implements DataExporter {
    @Override
    public byte[] exportData(String data) {
        return ("<data>" + data + "</data>").getBytes();
    }

    @Override
    public String getMimeType() {
        return "application/xml";
    }
}
```

```java
// src/main/java/com/example/export/CsvExporter.java
package com.example.export;

public final class CsvExporter implements DataExporter {
    @Override
    public byte[] exportData(String data) {
        return (data.replace(" ", ",")).getBytes();
    }

    // ❌ MISSING: getMimeType() not implemented
}
```

### Step 3: Document Contract (Auto-Detect Subclasses)

```java
// src/test/java/com/example/export/ExportContractTest.java
package com.example.export;

import org.junit.jupiter.api.Test;
import io.github.seanchatmangpt.dtr.api.DtrTest;

public class ExportContractTest extends DtrTest {

    @Test
    void documentDataExporterContract() {
        sayNextSection("Data Exporter Contract");

        say("""
            The `DataExporter` sealed interface automatically detects all permitted
            subclasses. This table verifies that each exporter provides both required methods.
            """);

        // DTR automatically finds JsonExporter, XmlExporter, CsvExporter
        sayContractVerification(DataExporter.class);

        sayWarning("""
            **CsvExporter is missing `getMimeType`** — HTTP clients cannot determine
            the content type without this method.
            """);
    }
}
```

**Generated Output**:

```markdown
# Data Exporter Contract

The `DataExporter` sealed interface automatically detects all permitted
subclasses. This table verifies that each exporter provides both required methods.

| Method | JsonExporter | XmlExporter | CsvExporter |
|--------|--------------|-------------|-------------|
| exportData | ✓ direct | ✓ direct | ✓ direct |
| getMimeType | ✓ direct | ✓ direct | ❌ MISSING |

> [!WARNING]
> **CsvExporter is missing `getMimeType`** — HTTP clients cannot determine
> the content type without this method.
```

---

## Complete Example: Plugin Architecture

Real-world example: A logging framework with multiple plugin implementations.

### Interface Definition

```java
// src/main/java/com/example/logging/LogPlugin.java
package com.example.logging;

public interface LogPlugin {
    void log(String level, String message);
    void configure(String config);
    void shutdown();
}
```

### Plugin Implementations

```java
// ConsolePlugin.java
public class ConsolePlugin implements LogPlugin {
    @Override
    public void log(String level, String message) {
        System.out.println("[" + level + "] " + message);
    }

    @Override
    public void configure(String config) {
        // No configuration needed for console
    }

    @Override
    public void shutdown() {
        // No cleanup needed
    }
}
```

```java
// FilePlugin.java
public class FilePlugin implements LogPlugin {
    @Override
    public void log(String level, String message) {
        // Write to file
    }

    @Override
    public void configure(String config) {
        // Parse file path from config
    }

    // ❌ MISSING: shutdown() not implemented
    // File handles will leak!
}
```

```java
// RemotePlugin.java
public class RemotePlugin implements LogPlugin {
    @Override
    public void log(String level, String message) {
        // Send to remote server
    }

    // Inherits configure() from AbstractPlugin
    // Inherits shutdown() from AbstractPlugin
}
```

### Contract Verification Test

```java
// LogPluginContractTest.java
@Test
void verifyAllPluginsImplementContract() {
    sayNextSection("Log Plugin Contract Verification");

    say("""
        All logging plugins must implement the full `LogPlugin` contract to ensure
        proper resource management and configuration handling.
        """);

    sayContractVerification(
        LogPlugin.class,
        ConsolePlugin.class,
        FilePlugin.class,
        RemotePlugin.class
    );

    sayWarning("""
        **FilePlugin is missing `shutdown()`** — file handles will leak and cause
        resource exhaustion. Implement `shutdown()` to close file streams.
        """);

    say("""
        **ConsolePlugin** and **RemotePlugin** correctly implement all required methods.
        RemotePlugin safely inherits configuration and shutdown logic from its base class.
        """);
}
```

**Generated Documentation**:

```markdown
# Log Plugin Contract Verification

All logging plugins must implement the full `LogPlugin` contract to ensure
proper resource management and configuration handling.

| Method | ConsolePlugin | FilePlugin | RemotePlugin |
|--------|---------------|------------|--------------|
| log | ✓ direct | ✓ direct | ✓ direct |
| configure | ✓ direct | ✓ direct | ↗ inherited |
| shutdown | ✓ direct | ❌ MISSING | ↗ inherited |

> [!WARNING]
> **FilePlugin is missing `shutdown()`** — file handles will leak and cause
> resource exhaustion. Implement `shutdown()` to close file streams.

**ConsolePlugin** and **RemotePlugin** correctly implement all required methods.
RemotePlugin safely inherits configuration and shutdown logic from its base class.
```

---

## Exercise

**Task**: Document a `CacheProvider` contract with multiple implementations

### Requirements

1. Create a `CacheProvider` interface with:
   - `void put(String key, Object value)`
   - `Object get(String key)`
   - `void clear()`

2. Create 4 implementations:
   - `MemoryCacheProvider` (implements all methods directly)
   - `RedisCacheProvider` (implements all methods directly)
   - `FileCacheProvider` (missing `clear()`)
   - `CachedDbCacheProvider` (extends abstract class with `put()` and `clear()`)

3. Create `CacheContractTest` that:
   - Uses `sayContractVerification` to document compliance
   - Uses `sayWarning` to highlight the missing method
   - Uses `sayAndAssertThat` to verify implementation count

### Starter Code

```java
// src/main/java/com/example/cache/CacheProvider.java
package com.example.cache;

public interface CacheProvider {
    void put(String key, Object value);
    Object get(String key);
    void clear();
}
```

```java
// src/test/java/com/example/cache/CacheContractTest.java
package com.example.cache;

import org.junit.jupiter.api.Test;
import io.github.seanchatmangpt.dtr.api.DtrTest;

public class CacheContractTest extends DtrTest {

    @Test
    void documentCacheProviderContract() {
        sayNextSection("Cache Provider Contract");

        say("""
            TODO: Add contract description
            """);

        // TODO: Add sayContractVerification call

        // TODO: Add warning for missing method

        // TODO: Add assertion for implementation count
    }
}
```

### Solution

<details>
<summary>Click to expand solution</summary>

```java
// MemoryCacheProvider.java
public class MemoryCacheProvider implements CacheProvider {
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
    }

    @Override
    public Object get(String key) {
        return cache.get(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
```

```java
// RedisCacheProvider.java
public class RedisCacheProvider implements CacheProvider {
    @Override
    public void put(String key, Object value) {
        // Redis SET operation
    }

    @Override
    public Object get(String key) {
        // Redis GET operation
        return null;
    }

    @Override
    public void clear() {
        // Redis FLUSHDB operation
    }
}
```

```java
// FileCacheProvider.java
public class FileCacheProvider implements CacheProvider {
    @Override
    public void put(String key, Object value) {
        // Write to file
    }

    @Override
    public Object get(String key) {
        // Read from file
        return null;
    }

    // ❌ MISSING: clear() not implemented
}
```

```java
// CachedDbCacheProvider.java
public abstract class AbstractCacheProvider implements CacheProvider {
    @Override
    public void put(String key, Object value) {
        // Shared implementation
    }

    @Override
    public void clear() {
        // Shared implementation
    }
}

public class CachedDbCacheProvider extends AbstractCacheProvider {
    @Override
    public Object get(String key) {
        // Database-specific implementation
        return null;
    }
}
```

```java
// CacheContractTest.java
@Test
void documentCacheProviderContract() {
    sayNextSection("Cache Provider Contract");

    say("""
        All cache providers must implement the full `CacheProvider` contract to ensure
        consistent behavior across different storage backends.
        """);

    sayContractVerification(
        CacheProvider.class,
        MemoryCacheProvider.class,
        RedisCacheProvider.class,
        FileCacheProvider.class,
        CachedDbCacheProvider.class
    );

    sayWarning("""
        **FileCacheProvider is missing `clear()`** — cached files will accumulate
        indefinitely. Implement `clear()` to delete cached files.
        """);

    sayAndAssertThat(
        "Cache Provider Count",
        new Class<?>[] {
            MemoryCacheProvider.class,
            RedisCacheProvider.class,
            FileCacheProvider.class,
            CachedDbCacheProvider.class
        }.length,
        equalTo(4)
    );
}
```

**Expected Output**:

```markdown
# Cache Provider Contract

All cache providers must implement the full `CacheProvider` contract to ensure
consistent behavior across different storage backends.

| Method | MemoryCacheProvider | RedisCacheProvider | FileCacheProvider | CachedDbCacheProvider |
|--------|---------------------|--------------------|-------------------|----------------------|
| put | ✓ direct | ✓ direct | ✓ direct | ↗ inherited |
| get | ✓ direct | ✓ direct | ✓ direct | ✓ direct |
| clear | ✓ direct | ✓ direct | ❌ MISSING | ↗ inherited |

> [!WARNING]
> **FileCacheProvider is missing `clear()`** — cached files will accumulate
> indefinitely. Implement `clear()` to delete cached files.
```

</details>

---

## What's Next

**Tutorial 7: Coverage Reporting** — Learn how to track which public methods have documentation and generate coverage reports.

Or explore the [full sayContractVerification API reference](../API.md#contract-verification).

---

## Summary

- **`sayContractVerification`** automatically inspects interface implementations
- **Three symbols**: ✓ direct, ↗ inherited, ❌ MISSING
- **Sealed interfaces** auto-detect permitted subclasses (no need to list them)
- **Real-world use cases**: Plugin systems, service providers, strategy patterns
- **Detect violations early** — before they cause runtime `NoSuchMethodError`

**Time Investment**: 25 minutes → **Payoff**: Verifiable contract compliance across your entire codebase
