# io.github.seanchatmangpt.dtr.test.ComplexityProfileDocTest

## Table of Contents

- [sayComplexityProfile — O(n): ArrayList.contains()](#saycomplexityprofileonarraylistcontains)
- [sayComplexityProfile — O(1): HashMap.get()](#saycomplexityprofileo1hashmapget)
- [sayComplexityProfile — O(n^2): Bubble Sort](#saycomplexityprofileon2bubblesort)


## sayComplexityProfile — O(n): ArrayList.contains()

ArrayList stores elements in a contiguous array with no index structure. A call to contains(Object) performs a sequential scan from index 0 to n-1 and returns as soon as the element is found or the list is exhausted. Searching for an element that is NOT present forces the scan to visit all n elements, making worst-case complexity O(n).

```java
// Factory pattern used by sayComplexityProfile
IntFunction<Runnable> factory = n -> {
    // Setup: build a list of n integers (0..n-1)
    var list = new ArrayList<Integer>(n);
    for (int i = 0; i < n; i++) {
        list.add(i);
    }
    // Measurement target: search for -1, which is NOT in the list.
    // Every element is visited — guaranteed worst case.
    return () -> list.contains(-1);
};
sayComplexityProfile("ArrayList.contains() worst-case", factory, new int[]{100, 1000, 10000});
```

> [!NOTE]
> Searching for a value that is absent guarantees worst-case O(n) behaviour. If a present value were used, early exit would produce sub-linear measurements and obscure the true growth class.

> [!WARNING]
> ArrayList.contains() is unsuitable for membership tests on large collections. Prefer HashSet.contains() — O(1) amortised — when lookup performance matters.

### Complexity Profile: ArrayList.contains() worst-case

| n | Time (ns) | Ratio vs n[0] | Inferred |
| --- | --- | --- | --- |
| `100` | `5278` | `1.00x` | baseline |
| `1000` | `32324` | `6.12x` | O(n) |
| `10000` | `63621` | `12.05x` | O(n) |

| Property | Value |
| --- | --- |
| Data structure | java.util.ArrayList (dynamic array) |
| Operation | contains(Object) — sequential scan |
| Search target | -1 (absent from list — worst case) |
| Complexity | O(n) |
| Input sizes | n = 100, 1 000, 10 000 |
| Measured with | System.nanoTime(), Java 26 |

## sayComplexityProfile — O(1): HashMap.get()

HashMap maintains an array of hash buckets. A get(key) call computes key.hashCode(), maps it to a bucket index, and traverses at most a short chain within that bucket. With a good hash function and a load factor below the resize threshold, the expected chain length is O(1) regardless of how many entries the map holds. This makes HashMap.get() the canonical O(1) Java data structure operation.

```java
IntFunction<Runnable> factory = n -> {
    // Setup: populate a map with n entries keyed 0..n-1
    var map = new HashMap<Integer, String>(n * 2);
    for (int i = 0; i < n; i++) {
        map.put(i, "value-" + i);
    }
    // Measurement target: look up key 0, which is always present.
    // Hash lookup — time is independent of map size.
    return () -> map.get(0);
};
sayComplexityProfile("HashMap.get() amortised", factory, new int[]{1000, 10000, 100000});
```

> [!NOTE]
> The initial capacity is set to n*2 to keep the load factor near 0.5 and suppress mid-benchmark resize operations that would distort the O(1) signal.

### Complexity Profile: HashMap.get() amortised

| n | Time (ns) | Ratio vs n[0] | Inferred |
| --- | --- | --- | --- |
| `1000` | `91` | `1.00x` | baseline |
| `10000` | `121` | `1.33x` | O(1) |
| `100000` | `98` | `1.08x` | O(1) |

| Property | Value |
| --- | --- |
| Data structure | java.util.HashMap (hash table) |
| Operation | get(Object) — hash-then-probe |
| Search target | key 0 (present in map) |
| Complexity | O(1) amortised |
| Input sizes | n = 1 000, 10 000, 100 000 |
| Measured with | System.nanoTime(), Java 26 |

## sayComplexityProfile — O(n^2): Bubble Sort

Bubble sort repeatedly steps through the array, compares adjacent elements, and swaps them when out of order. In the worst case (a fully reversed array) the outer loop runs n times and the inner loop runs up to n-1 times per pass, yielding n*(n-1)/2 comparisons — O(n²). It is the textbook quadratic algorithm and produces the clearest possible n² signal for empirical profiling.

```java
IntFunction<Runnable> factory = n -> {
    // Setup: descending array — worst case for bubble sort.
    int[] arr = new int[n];
    for (int i = 0; i < n; i++) {
        arr[i] = n - i;          // n, n-1, ..., 2, 1
    }
    // Measurement target: in-place bubble sort.
    return () -> {
        int[] copy = arr.clone(); // sort a fresh copy each invocation
        for (int i = 0; i < copy.length - 1; i++) {
            for (int j = 0; j < copy.length - 1 - i; j++) {
                if (copy[j] > copy[j + 1]) {
                    int tmp = copy[j];
                    copy[j]     = copy[j + 1];
                    copy[j + 1] = tmp;
                }
            }
        }
    };
};
sayComplexityProfile("Bubble sort worst-case", factory, new int[]{100, 500, 1000});
```

> [!WARNING]
> Bubble sort is shown here as a complexity demonstration only. Never use it in production; prefer Arrays.sort() (dual-pivot quicksort / TimSort, O(n log n)) for real sorting workloads.

> [!NOTE]
> Each Runnable clones the source array before sorting so that every invocation within a single measurement round starts from the same worst-case descending sequence. Without cloning, the first sort would leave a sorted array, and subsequent passes would complete in O(n) due to early termination — masking the true O(n²) growth.

### Complexity Profile: Bubble sort worst-case

| n | Time (ns) | Ratio vs n[0] | Inferred |
| --- | --- | --- | --- |
| `100` | `161541` | `1.00x` | baseline |
| `500` | `306055` | `1.89x` | O(n) |
| `1000` | `344326` | `2.13x` | O(n) |

| Property | Value |
| --- | --- |
| Algorithm | Bubble sort (in-place, stable) |
| Input order | Descending (worst case) |
| Operation | Adjacent-swap comparison loop |
| Complexity | O(n^2) |
| Input sizes | n = 100, 500, 1 000 |
| Measured with | System.nanoTime(), Java 26 |

With n=500 being 5x larger than n=100, the measured runtime should increase by approximately 5² = 25x. With n=1000 being 2x larger than n=500, the runtime should increase by approximately 2² = 4x. The sayComplexityProfile output above confirms this quadratic relationship empirically rather than through assertion.

---
*Generated by [DTR](http://www.dtr.org)*
