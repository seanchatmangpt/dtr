# io.github.seanchatmangpt.dtr.test.ApiDiffDocTest

## Table of Contents

- [sayApiDiff — UserService V1 to V2 Evolution](#sayapidiffuserservicev1tov2evolution)
- [sayApiDiff — No Changes (Same API Compared with Itself)](#sayapidiffnochangessameapicomparedwithitself)
- [sayApiDiff — RenderMachineCommands vs RenderMachine (Real-World Diff)](#sayapidiffrendermachinecommandsvsrendermachinerealworlddiff)


## sayApiDiff — UserService V1 to V2 Evolution

API compatibility is one of the hardest guarantees to maintain across releases. A single method removal or signature change silently breaks downstream consumers that compiled against the old version. `sayApiDiff` surfaces every change by comparing the public method sets of two class versions using Java reflection — no source access required, no manual changelog entry needed.

The two versions below represent a realistic service refactor. V1 exposes `deleteUser`, which V2 removes in favour of a softer deactivation path (not shown). V2 adds `findUserByEmail` and `updateUser`. Any caller relying on `deleteUser` will break.

```java
// V1 — original contract
static class UserServiceV1 {
    public String findUser(String id) { return null; }
    public void deleteUser(String id) {}
    public List<String> listUsers() { return null; }
}

// V2 — evolved contract (deleteUser removed, two methods added)
static class UserServiceV2 {
    public String findUser(String id) { return null; }
    public String findUserByEmail(String email) { return null; }
    public List<String> listUsers() { return null; }
    public void updateUser(String id, String data) {}
}

// In test:
sayApiDiff(UserServiceV1.class, UserServiceV2.class);
```

> [!WARNING]
> Removing `deleteUser` is a binary-breaking change. Any compiled caller that references this method will throw NoSuchMethodError at runtime until recompiled against V2.

### API Diff: `UserServiceV1` → `UserServiceV2`

**Added (2):**

| Method |
| --- |
| ✅ `String findUserByEmail(String)` |
| ✅ `void updateUser(String, String)` |

**Removed (1):**

| Method |
| --- |
| ❌ `void deleteUser(String)` |

The diff tables above are generated entirely from bytecode via `Class.getDeclaredMethods()`. No source files or Javadoc are required. The documentation is always consistent with the binary on the classpath.

> [!NOTE]
> Run `sayApiDiff` in a dedicated test per release to make every breaking change visible in the generated docs before the artifact ships.

## sayApiDiff — No Changes (Same API Compared with Itself)

A diff that shows no changes is an explicit, machine-verified claim of backward compatibility. Comparing a class with itself is the simplest way to confirm that `sayApiDiff` produces an empty result — and therefore that a patch release has introduced no API modifications.

Here, `UserServiceV1` is passed as both the `before` and `after` argument. Every method present in V1 is also present in V1 with an identical signature, so all three tables (added, removed, changed) must be empty.

```java
// Both arguments are the same class — no diff expected
sayApiDiff(UserServiceV1.class, UserServiceV1.class);
```

### API Diff: `UserServiceV1` → `UserServiceV1`

*(No API changes detected)*

> [!NOTE]
> An empty diff is not the same as skipping the diff. Generating it explicitly provides a documented, timestamped assertion that the API surface has not changed between runs.

## sayApiDiff — RenderMachineCommands vs RenderMachine (Real-World Diff)

`RenderMachineCommands` defines the minimal `say*` contract that all DTR render machines must implement. `RenderMachine` is the abstract base class that inherits that contract and extends it with additional lifecycle and output-routing methods such as `setFileName`, `saySlideOnly`, `sayDocOnly`, and `saySpeakerNote`.

Running `sayApiDiff` across these two types reveals the exact surface that `RenderMachine` adds beyond the core interface. This makes the architectural boundary self-documenting: any method visible in the "added" table is an extension point not required by the contract.

```java
sayApiDiff(
    RenderMachineCommands.class,  // before: the interface contract
    RenderMachine.class           // after:  the abstract base class
);
```

### API Diff: `RenderMachineCommands` → `RenderMachine`

**Added (9):**

| Method |
| --- |
| ✅ `void setFileName(String)` |
| ✅ `void finishAndWriteOut()` |
| ✅ `void saySlideOnly(String)` |
| ✅ `void sayDocOnly(String)` |
| ✅ `void saySpeakerNote(String)` |
| ✅ `void sayHeroImage(String)` |
| ✅ `void sayTweetable(String)` |
| ✅ `void sayTldr(String)` |
| ✅ `void sayCallToAction(String)` |

**Removed (16):**

| Method |
| --- |
| ❌ `void say(String)` |
| ❌ `void sayNextSection(String)` |
| ❌ `void sayCode(String, String)` |
| ❌ `void sayWarning(String)` |
| ❌ `void sayNote(String)` |
| ❌ `void sayRaw(String)` |
| ❌ `void sayTable(String[][])` |
| ❌ `void sayKeyValue(Map)` |
| ❌ `void sayUnorderedList(List)` |
| ❌ `void sayOrderedList(List)` |
| ❌ `void sayJson(Object)` |
| ❌ `void sayAssertions(Map)` |
| ❌ `void sayRef(DocTestRef)` |
| ❌ `void sayFootnote(String)` |
| ❌ `void sayCite(String, String)` |
| ❌ `void sayCite(String)` |

The diff is computed at test runtime from the live `.class` files on the classpath. If `RenderMachine` gains or loses methods in a future release, the generated documentation updates automatically on the next test run without any manual changelog edit.

> [!NOTE]
> Use `sayApiDiff(interface, abstractClass)` as a recurring pattern to document the layered extension points in any framework you ship with DTR.

---
*Generated by [DTR](http://www.dtr.org)*
