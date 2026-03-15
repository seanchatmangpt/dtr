/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.rendermachine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the extended say* API methods in RenderMachineImpl.
 */
class RenderMachineExtensionTest {

    private RenderMachineImpl renderMachine;

    @BeforeEach
    void setup() {
        renderMachine = new RenderMachineImpl();
    }

    @Test
    void testSayTable_Basic() {
        String[][] data = {
            {"Name", "Status", "Score"},
            {"Alice", "Active", "95"},
            {"Bob", "Inactive", "87"}
        };

        renderMachine.sayTable(data);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| Name | Status | Score |"), "Table header should be present");
        assertTrue(output.contains("| Alice | Active | 95 |"), "First row should be present");
        assertTrue(output.contains("| Bob | Inactive | 87 |"), "Second row should be present");
        assertTrue(output.contains("| --- |"), "Separator row should be present");
    }

    @Test
    void testSayTable_Empty() {
        renderMachine.sayTable(null);
        renderMachine.sayTable(new String[][]{});

        assertTrue(renderMachine.markdownDocument.isEmpty(), "Empty table should not add content");
    }

    @Test
    void testSayTable_WithNullCells() {
        String[][] data = {
            {"Col1", "Col2"},
            {"Value1", null},
            {null, "Value2"}
        };

        renderMachine.sayTable(data);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| Value1 |"), "Should handle null cells gracefully");
        assertTrue(output.contains("| Value2 |"), "Should handle null cells gracefully");
    }

    @Test
    void testSayCode_Java() {
        String javaCode = "public void sayHello() {\n    System.out.println(\"Hello\");\n}";
        renderMachine.sayCode(javaCode, "java");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("```java"), "Language hint should be present");
        assertTrue(output.contains("public void sayHello()"), "Code should be present");
        assertTrue(output.contains("```"), "Code block should be closed");
    }

    @Test
    void testSayCode_SQL() {
        String sql = "SELECT * FROM users WHERE id = 1;";
        renderMachine.sayCode(sql, "sql");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("```sql"), "SQL language hint should be present");
        assertTrue(output.contains("SELECT * FROM users"), "SQL code should be present");
    }

    @Test
    void testSayCode_NoLanguage() {
        String code = "some code";
        renderMachine.sayCode(code, "");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("```\n"), "Code block should have fence even without language");
        assertTrue(output.contains("some code"), "Code should be present");
    }

    @Test
    void testSayCode_Null() {
        renderMachine.sayCode(null, "java");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("```java"), "Language hint should be present");
        assertTrue(output.contains("```"), "Code block should still exist");
    }

    @Test
    void testSayWarning() {
        renderMachine.sayWarning("This is a warning!");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("> [!WARNING]"), "Warning marker should be present");
        assertTrue(output.contains("> This is a warning!"), "Warning message should be present");
    }

    @Test
    void testSayWarning_Empty() {
        renderMachine.sayWarning("");
        renderMachine.sayWarning(null);

        assertTrue(renderMachine.markdownDocument.size() <= 2, "Empty warnings should be skipped");
    }

    @Test
    void testSayNote() {
        renderMachine.sayNote("This is a note.");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("> [!NOTE]"), "Note marker should be present");
        assertTrue(output.contains("> This is a note."), "Note message should be present");
    }

    @Test
    void testSayNote_Empty() {
        renderMachine.sayNote("");
        renderMachine.sayNote(null);

        assertTrue(renderMachine.markdownDocument.size() <= 2, "Empty notes should be skipped");
    }

    @Test
    void testSayKeyValue_Basic() {
        Map<String, String> pairs = Map.of(
            "Host", "localhost:8080",
            "Port", "8080",
            "Protocol", "HTTP"
        );

        renderMachine.sayKeyValue(pairs);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| Key | Value |"), "Table header should be present");
        assertTrue(output.contains("| `Host` | `localhost:8080` |"), "Key-value pair should be present");
        assertTrue(output.contains("| `Port` | `8080` |"), "Key-value pair should be present");
        assertTrue(output.contains("| `Protocol` | `HTTP` |"), "Key-value pair should be present");
    }

    @Test
    void testSayKeyValue_Empty() {
        renderMachine.sayKeyValue(null);
        renderMachine.sayKeyValue(Map.of());

        assertTrue(renderMachine.markdownDocument.isEmpty(), "Empty key-value map should not add content");
    }

    @Test
    void testSayKeyValue_WithNullValues() {
        Map<String, String> pairs = Map.of(
            "Key1", "Value1",
            "Key2", ""
        );

        renderMachine.sayKeyValue(pairs);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| `Key1` | `Value1` |"), "Normal pair should be present");
        assertTrue(output.contains("| `Key2` | `` |"), "Empty value should be handled");
    }

    @Test
    void testSayUnorderedList_Basic() {
        List<String> items = List.of("First item", "Second item", "Third item");
        renderMachine.sayUnorderedList(items);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("- First item"), "First item should be present");
        assertTrue(output.contains("- Second item"), "Second item should be present");
        assertTrue(output.contains("- Third item"), "Third item should be present");
    }

    @Test
    void testSayUnorderedList_Empty() {
        renderMachine.sayUnorderedList(null);
        renderMachine.sayUnorderedList(List.of());

        assertTrue(renderMachine.markdownDocument.isEmpty(), "Empty list should not add content");
    }

    @Test
    void testSayUnorderedList_WithNull() {
        List<String> items = Arrays.asList("First", null, "Third");
        renderMachine.sayUnorderedList(items);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("- First"), "Non-null item should be present");
        assertTrue(output.contains("- "), "Null item should be handled");
        assertTrue(output.contains("- Third"), "Non-null item should be present");
    }

    @Test
    void testSayOrderedList_Basic() {
        List<String> items = List.of("First step", "Second step", "Third step");
        renderMachine.sayOrderedList(items);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("1. First step"), "Numbered first item should be present");
        assertTrue(output.contains("2. Second step"), "Numbered second item should be present");
        assertTrue(output.contains("3. Third step"), "Numbered third item should be present");
    }

    @Test
    void testSayOrderedList_Empty() {
        renderMachine.sayOrderedList(null);
        renderMachine.sayOrderedList(List.of());

        assertTrue(renderMachine.markdownDocument.isEmpty(), "Empty list should not add content");
    }

    @Test
    void testSayJson_Object() {
        Map<String, Object> json = Map.of("name", "Alice", "age", 30, "active", true);
        renderMachine.sayJson(json);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("```json"), "JSON code block should be present");
        assertTrue(output.contains("\"name\""), "JSON content should be present");
        assertTrue(output.contains("\"Alice\""), "JSON value should be present");
    }

    @Test
    void testSayJson_List() {
        List<String> items = List.of("a", "b", "c");
        renderMachine.sayJson(items);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("```json"), "JSON code block should be present");
        assertTrue(output.contains("\"a\""), "JSON array element should be present");
    }

    @Test
    void testSayJson_Null() {
        renderMachine.sayJson(null);

        assertTrue(renderMachine.markdownDocument.isEmpty(), "Null object should not add content");
    }

    @Test
    void testSayJson_InvalidObject() {
        // Custom object that can be serialized
        record Person(String name, int age) {}
        Person person = new Person("Bob", 25);

        renderMachine.sayJson(person);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("```json"), "JSON code block should be present");
        assertTrue(output.contains("\"name\""), "JSON field should be present");
        assertTrue(output.contains("Bob"), "JSON value should be present");
    }

    @Test
    void testSayAssertions_Basic() {
        Map<String, String> assertions = Map.of(
            "Status is 200", "PASS",
            "Body contains data", "PASS",
            "Headers are valid", "FAIL"
        );

        renderMachine.sayAssertions(assertions);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| Check | Result |"), "Assertions table header should be present");
        assertTrue(output.contains("| Status is 200 | `PASS` |"), "Assertion should be present");
        assertTrue(output.contains("| Body contains data | `PASS` |"), "Assertion should be present");
        assertTrue(output.contains("| Headers are valid | `FAIL` |"), "Assertion should be present");
    }

    @Test
    void testSayAssertions_Empty() {
        renderMachine.sayAssertions(null);
        renderMachine.sayAssertions(Map.of());

        assertTrue(renderMachine.markdownDocument.isEmpty(), "Empty assertions should not add content");
    }

    @Test
    void testSayAssertions_WithNullValues() {
        Map<String, String> assertions = Map.of(
            "Check1", "PASS",
            "Check2", ""
        );

        renderMachine.sayAssertions(assertions);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| Check1 | `PASS` |"), "Assertion should be present");
        assertTrue(output.contains("| Check2 | `` |"), "Empty result should be handled");
    }

    @Test
    void testMultipleSayMethodsCombined() {
        renderMachine.sayNextSection("Test Section");
        renderMachine.say("This is a test.");
        renderMachine.sayWarning("Important!");
        renderMachine.sayJson(Map.of("key", "value"));
        renderMachine.sayUnorderedList(List.of("Item 1", "Item 2"));

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("## Test Section"), "Section should be present");
        assertTrue(output.contains("This is a test."), "Paragraph should be present");
        assertTrue(output.contains("> [!WARNING]"), "Warning should be present");
        assertTrue(output.contains("```json"), "JSON should be present");
        assertTrue(output.contains("- Item 1"), "List should be present");
    }

    @Test
    void testSaySecurityManager_Basic() {
        renderMachine.saySecurityManager();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("### Security Manager"), "Security Manager section should be present");
        assertTrue(output.contains("| Property | Status |"), "Status table header should be present");
        assertTrue(output.contains("Security Manager"), "Security Manager row should be present");
        assertTrue(output.contains("### Security Providers"), "Security Providers section should be present");
        assertTrue(output.contains("| Provider | Version | Info |"), "Providers table header should be present");
        assertTrue(output.contains("### Available Cryptographic Algorithms"), "Crypto algorithms section should be present");
        assertTrue(output.contains("**KeyPairGenerator:**"), "KeyPairGenerator section should be present");
        assertTrue(output.contains("**Cipher:**"), "Cipher section should be present");
        assertTrue(output.contains("**MessageDigest:**"), "MessageDigest section should be present");
        assertTrue(output.contains("### SecureRandom"), "SecureRandom section should be present");
    }

    @Test
    void testSaySecurityManager_ContainsProviderInfo() {
        renderMachine.saySecurityManager();

        String output = String.join("\n", renderMachine.markdownDocument);
        // Should contain at least one provider (typically SUN, SunRsaSign, SunJCE, etc.)
        assertTrue(output.contains("| `"), "Should contain at least one provider with backticks");
        // Should contain algorithm listings
        assertTrue(output.contains("RSA") || output.contains("AES") || output.contains("SHA"),
                "Should contain common crypto algorithms (RSA, AES, or SHA)");
        // Should contain SecureRandom info
        assertTrue(output.contains("| Strong Algorithm |") || output.contains("| Default Algorithm |"),
                "Should contain SecureRandom algorithm information");
    }

    @Test
    void testSaySecurityManager_SecurityManagerStatus() {
        renderMachine.saySecurityManager();

        String output = String.join("\n", renderMachine.markdownDocument);
        // Check for either PRESENT or ABSENT (modern Java typically has no SecurityManager)
        assertTrue(output.contains("`PRESENT`") || output.contains("`ABSENT`"),
                "Should show Security Manager as either PRESENT or ABSENT");
    }

    @Test
    void testSayThreadDump_Basic() {
        renderMachine.sayThreadDump();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("### Thread Summary"), "Thread Summary section should be present");
        assertTrue(output.contains("### Thread Details"), "Thread Details section should be present");
        assertTrue(output.contains("| Thread ID | Name | State | Alive | Interrupted |"),
                   "Thread table header should be present");
        assertTrue(output.contains("| Thread Count |"), "Thread Count metric should be present");
        assertTrue(output.contains("| Daemon Thread Count |"), "Daemon Thread Count metric should be present");
        assertTrue(output.contains("| Peak Thread Count |"), "Peak Thread Count metric should be present");
        assertTrue(output.contains("| Total Started Thread Count |"),
                   "Total Started Thread Count metric should be present");
    }

    @Test
    void testSayThreadDump_ContainsMainThread() {
        renderMachine.sayThreadDump();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("main"), "Main thread should be present in thread dump");
    }

    @Test
    void testSayThreadDump_HasThreadStates() {
        renderMachine.sayThreadDump();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("RUNNABLE") || output.contains("WAITING") || output.contains("BLOCKED"),
                   "Thread dump should contain at least one thread state");
    }

    @Test
    void testSayThreadDump_HasMetrics() {
        renderMachine.sayThreadDump();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| --- | --- |"), "Table separator should be present");
        assertTrue(output.matches("(?s).*live threads.*"), "Should mention number of live threads");
    }

    @Test
    void testSayOperatingSystem_Basic() {
        renderMachine.sayOperatingSystem();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("### Operating System Metrics"), "OS Metrics section should be present");
        assertTrue(output.contains("| Metric | Value |"), "Table header should be present");
        assertTrue(output.contains("| OS Name |"), "OS Name row should be present");
        assertTrue(output.contains("| OS Version |"), "OS Version row should be present");
        assertTrue(output.contains("| OS Architecture |"), "OS Architecture row should be present");
        assertTrue(output.contains("| Available Processors |"), "Available Processors row should be present");
    }

    @Test
    void testSayOperatingSystem_ContainsSystemLoadAverage() {
        renderMachine.sayOperatingSystem();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| System Load Average |"), "System Load Average row should be present");
        // May contain N/A or a numeric value
        assertTrue(output.contains("System Load Average") &&
                   (output.contains("N/A") || output.matches("(?s).*System Load Average.*\\d+.*")),
                   "System Load Average should be present with value or N/A");
    }

    @Test
    void testSayOperatingSystem_ContainsCPUMetrics() {
        renderMachine.sayOperatingSystem();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| Process CPU Load |"), "Process CPU Load row should be present");
        assertTrue(output.contains("| System CPU Load |"), "System CPU Load row should be present");
        // May contain N/A or percentage values
        assertTrue(output.contains("Process CPU Load") &&
                   (output.contains("N/A") || output.contains("%")),
                   "Process CPU Load should have value or N/A");
    }

    @Test
    void testSayOperatingSystem_ContainsMemoryMetrics() {
        renderMachine.sayOperatingSystem();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| Total Physical Memory |"), "Total Physical Memory row should be present");
        assertTrue(output.contains("| Free Physical Memory |"), "Free Physical Memory row should be present");
        assertTrue(output.contains("| Used Physical Memory |"), "Used Physical Memory row should be present");
        assertTrue(output.contains("| Total Swap Space |"), "Total Swap Space row should be present");
        assertTrue(output.contains("| Free Swap Space |"), "Free Swap Space row should be present");
        assertTrue(output.contains("| Used Swap Space |"), "Used Swap Space row should be present");
    }

    @Test
    void testSayOperatingSystem_MemoryValuesInMB() {
        renderMachine.sayOperatingSystem();

        String output = String.join("\n", renderMachine.markdownDocument);
        // Memory values should be in MB format
        assertTrue(output.matches("(?s).*Total Physical Memory.*\\d+\\s+MB.*"),
                   "Total Physical Memory should be in MB");
        assertTrue(output.matches("(?s).*Free Physical Memory.*\\d+\\s+MB.*") ||
                   output.contains("N/A"),
                   "Free Physical Memory should be in MB or N/A");
    }

    @Test
    void testSayOperatingSystem_ProcessorCountPositive() {
        renderMachine.sayOperatingSystem();

        String output = String.join("\n", renderMachine.markdownDocument);
        // Extract the processor count value and verify it's positive
        assertTrue(output.matches("(?s).*Available Processors.*[1-9]\\d*.*"),
                   "Available Processors should be a positive number");
    }

    @Test
    void testSaySystemProperties_All() {
        renderMachine.saySystemProperties();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("### JVM System Properties"), "Section heading should be present");
        assertTrue(output.contains("| Property Key | Property Value |"), "Table header should be present");
        assertTrue(output.contains("| --- | --- |"), "Separator row should be present");
        // Check for common properties that should always exist
        assertTrue(output.contains("java.version") || output.contains("java.home"),
                "Should contain java.version or java.home");
        assertTrue(output.contains("properties documented"), "Should show property count");
    }

    @Test
    void testSaySystemProperties_WithFilter() {
        renderMachine.saySystemProperties("java.*");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("### JVM System Properties"), "Section heading should be present");
        assertTrue(output.contains("*Filter: `java.*`*"), "Filter note should be present");
        assertTrue(output.contains("| Property Key | Property Value |"), "Table header should be present");
        // All properties should start with "java."
        var lines = output.split("\n");
        boolean foundJavaProperty = false;
        for (var line : lines) {
            if (line.matches("\\|\\s*`java\\..*`\\s*\\|.*")) {
                foundJavaProperty = true;
                break;
            }
        }
        assertTrue(foundJavaProperty, "Should find at least one java.* property");
    }

    @Test
    void testSaySystemProperties_UserFilter() {
        renderMachine.saySystemProperties("user.*");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("*Filter: `user.*`*"), "Filter note should be present");
        // Should contain user.name, user.dir, user.home, etc.
        assertTrue(output.contains("user.name") || output.contains("user.dir") || output.contains("user.home"),
                "Should contain user.* properties");
    }

    @Test
    void testSaySystemProperties_NoMatchFilter() {
        renderMachine.saySystemProperties("nonexistent.property.*");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("> [!NOTE]"), "Should render a note box");
        assertTrue(output.contains("No system properties found matching filter"), "Should indicate no matches");
        assertTrue(output.contains("`nonexistent.property.*`"), "Should show the filter that failed");
    }

    @Test
    void testSaySystemProperties_EmptyFilter() {
        renderMachine.saySystemProperties("");

        String output = String.join("\n", renderMachine.markdownDocument);
        // Empty filter should behave like no filter - show all properties
        assertTrue(output.contains("### JVM System Properties"), "Should show all properties");
        assertFalse(output.contains("*Filter:"), "Should not show filter note for empty filter");
    }

    @Test
    void testSaySystemProperties_CommonProperties() {
        renderMachine.saySystemProperties();

        String output = String.join("\n", renderMachine.markdownDocument);
        // Check for commonly documented properties
        assertTrue(output.contains("java.version") || output.contains("java.home") ||
                output.contains("user.timezone") || output.contains("file.encoding") ||
                output.contains("os.name") || output.contains("os.arch"),
                "Should contain at least one common system property");
    }

    @Test
    void testSayModuleDependencies_SingleClass() {
        renderMachine.sayModuleDependencies(String.class);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("### Module Dependencies"), "Should have module dependencies section");
        assertTrue(output.contains("java.base"), "Should mention java.base module");
        assertTrue(output.contains("**Requires**"), "Should show requires section");
        assertTrue(output.contains("**Exports**"), "Should show exports section");
    }

    @Test
    void testSayModuleDependencies_MultipleClasses() {
        renderMachine.sayModuleDependencies(String.class, List.class, Map.class);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("### Module Dependencies"), "Should have module dependencies section");
        // All these should be from java.base
        assertTrue(output.contains("java.base"), "Should mention java.base module");
    }

    @Test
    void testSayModuleDependencies_EmptyArray() {
        renderMachine.sayModuleDependencies();

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("> [!NOTE]"), "Should show note for empty input");
        assertTrue(output.contains("No classes provided"), "Should explain no classes provided");
    }

    @Test
    void testSayModuleDependencies_NullArray() {
        renderMachine.sayModuleDependencies((Class<?>[]) null);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("> [!NOTE]"), "Should show note for null input");
    }

    @Test
    void testSayModuleDependencies_WithNullElements() {
        renderMachine.sayModuleDependencies(String.class, null, List.class);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("### Module Dependencies"), "Should have module dependencies section");
        assertTrue(output.contains("java.base"), "Should mention java.base module");
        // Should filter out null elements
        assertFalse(output.contains("null"), "Should not render null class names");
    }

    @Test
    void testSayModuleDependencies_ModuleMetadata() {
        renderMachine.sayModuleDependencies(String.class);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("**Module Name:**"), "Should show module name");
        assertTrue(output.contains("**Automatic:**"), "Should show automatic status");
        assertTrue(output.contains("**Packages**"), "Should show packages section");
    }

    @Test
    void testSayModuleDependencies_RequiresTable() {
        // String.class is in java.base which has no requires
        renderMachine.sayModuleDependencies(String.class);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("**Requires**"), "Should show requires section");
        // java.base has no requires, so we get the "none" message instead of a table
        assertTrue(output.contains("*(none — this is likely the base module)*") ||
                    output.contains("| Module | Modifiers |"),
                    "Should show 'none' message or table header");
    }

    @Test
    void testSayModuleDependencies_ExportsTable() {
        renderMachine.sayModuleDependencies(String.class);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("**Exports**"), "Should show exports section");
        assertTrue(output.contains("| Package | Target Modules |"), "Should have exports table header");
    }

    @Test
    void testSayModuleDependencies_ServicesSection() {
        renderMachine.sayModuleDependencies(String.class);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("**Uses**"), "Should show uses section");
        assertTrue(output.contains("**Provides**"), "Should show provides section");
    }
}
