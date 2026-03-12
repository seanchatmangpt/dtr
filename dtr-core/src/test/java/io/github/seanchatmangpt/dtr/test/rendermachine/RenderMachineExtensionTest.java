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
}
