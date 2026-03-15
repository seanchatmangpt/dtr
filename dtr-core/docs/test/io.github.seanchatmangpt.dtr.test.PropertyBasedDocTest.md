# io.github.seanchatmangpt.dtr.test.PropertyBasedDocTest

## Table of Contents

- [Property 1: String.length() Is Always Non-Negative](#property1stringlengthisalwaysnonnegative)
- [Property 2: Math.sqrt of a Non-Negative Input Is Non-Negative](#property2mathsqrtofanonnegativeinputisnonnegative)
- [Property 3: ArrayList Size Increases by Exactly 1 After add](#property3arraylistsizeincreasesbyexactly1afteradd)


## Property 1: String.length() Is Always Non-Negative

The Java Language Specification defines String.length() as the number of UTF-16 code units in the sequence. A count of code units cannot be negative: the minimum is 0, returned for the empty string. This is a foundational invariant that every Java program relies on, whether or not it is stated explicitly in API documentation.

DTR's sayPropertyBased captures this invariant as an executable contract. The predicate is applied to each sample input in turn. If every input passes, a pass table is written to the document. If any input violates the predicate, an AssertionError is thrown immediately — the test fails and the violation appears in the generated output as a WARNING block.

```java
// Invariant: String.length() >= 0 for every possible input
Predicate<Object> lengthNonNegative = o -> ((String) o).length() >= 0;

sayPropertyBased(
    "String.length() is always >= 0",
    lengthNonNegative,
    List.of("", "hello", "a", "longer string", "  ")
);
```

> [!NOTE]
> The empty string "" is the boundary case: length() returns 0, which satisfies >= 0. The whitespace string "  " has length 2. No string in the Java type system can produce a negative length.

### Property: String.length() is always >= 0

| Input | Result | Status |
| --- | --- | --- |
| `` | true | ✅ PASS |
| `hello` | true | ✅ PASS |
| `a` | true | ✅ PASS |
| `longer string` | true | ✅ PASS |
| `  ` | true | ✅ PASS |

**Result: 5/5 inputs satisfied the property.**

## Property 2: Math.sqrt of a Non-Negative Input Is Non-Negative

Math.sqrt(x) is defined as the principal (non-negative) square root of x for x >= 0.0. The IEEE 754 standard, which Java's floating-point arithmetic follows, guarantees that the result is non-negative when the input is non-negative. This property is a precondition for many numerical algorithms and geometric computations.

The five sample inputs cover distinct regions of the non-negative real line: the origin (0.0), unit value (1.0), a perfect square (4.0), a large integer value (100.0), and a sub-unit positive (0.001). Each produces a well-defined, non-negative result.

```java
// Invariant: Math.sqrt(x) >= 0 for all x >= 0
Predicate<Object> sqrtNonNegative = o -> Math.sqrt((Double) o) >= 0;

sayPropertyBased(
    "Math.sqrt of non-negative is non-negative",
    sqrtNonNegative,
    List.of(0.0, 1.0, 4.0, 100.0, 0.001)
);
```

| Input | Math.sqrt result | Exact? |
| --- | --- | --- |
| 0.0 | 0.0 | Yes |
| 1.0 | 1.0 | Yes |
| 4.0 | 2.0 | Yes |
| 100.0 | 10.0 | Yes |
| 0.001 | ~0.031622... | Rounded (IEEE 754) |

> [!WARNING]
> Math.sqrt(x) returns NaN when x < 0.0 and returns -0.0 (negative zero) only for the input -0.0, not for any positive input. All five inputs here are strictly non-negative, so NaN and -0.0 cannot arise.

### Property: Math.sqrt of non-negative is non-negative

| Input | Result | Status |
| --- | --- | --- |
| `0.0` | true | ✅ PASS |
| `1.0` | true | ✅ PASS |
| `4.0` | true | ✅ PASS |
| `100.0` | true | ✅ PASS |
| `0.001` | true | ✅ PASS |

**Result: 5/5 inputs satisfied the property.**

## Property 3: ArrayList Size Increases by Exactly 1 After add

The List.add(E) contract in java.util.Collection specifies that the size of the collection increases by one after a successful add. For ArrayList, which accepts all elements (no capacity exception under normal conditions), this is an unconditional invariant: size(after add) == size(before add) + 1.

The predicate receives an Integer N representing the initial list size. It constructs a fresh ArrayList, fills it with N placeholder elements, adds one more element, then asserts size() == N + 1. The five input sizes span the range from an empty list (0) to a list of one hundred elements (100), verifying that no resize or capacity boundary breaks the invariant.

```java
// Invariant: after one add, list.size() == initialSize + 1
Predicate<Object> sizeIncreasesOnAdd = o -> {
    int initialSize = (Integer) o;
    List<String> list = new ArrayList<>(initialSize);
    for (int i = 0; i < initialSize; i++) {
        list.add("item-" + i);
    }
    list.add("new-element");
    return list.size() == initialSize + 1;
};

sayPropertyBased(
    "ArrayList size increases by 1 on add",
    sizeIncreasesOnAdd,
    List.of(0, 1, 5, 10, 100)
);
```

> [!NOTE]
> The ArrayList constructor hint `new ArrayList<>(initialSize)` pre-allocates capacity but does not affect size. The invariant holds regardless of whether a capacity expansion (internal array copy) occurs during add.

### Property: ArrayList size increases by 1 on add

| Input | Result | Status |
| --- | --- | --- |
| `0` | true | ✅ PASS |
| `1` | true | ✅ PASS |
| `5` | true | ✅ PASS |
| `10` | true | ✅ PASS |
| `100` | true | ✅ PASS |

**Result: 5/5 inputs satisfied the property.**

---
*Generated by [DTR](http://www.dtr.org)*
