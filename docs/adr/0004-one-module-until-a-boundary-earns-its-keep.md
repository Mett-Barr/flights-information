# 4. One module until a boundary earns its keep

Accepted · 2026-07-29

## Context

The project has three layers — `domain`, `data`, `presentation` — with
dependencies pointing inward, repository interfaces in `domain/repository/` and
implementations in `data/repository/` bound through Hilt `@Binds`. The inversion
is real, not nominal.

But it lives in one Gradle module. `settings.gradle.kts` is `include(":app")` and
nothing else. The layers are package conventions, and package conventions are
advisory.

The discipline currently holds. As of this record:

```
grep -r "import moozy.flightinformation.data"          .../domain/        → 0
grep -r "import moozy.flightinformation.presentation"  .../domain/        → 0
grep -r "import io.ktor"                               .../presentation/  → 0
grep -rn "Dto"                                         .../presentation/  → 0
```

That is an observation about today, not a guarantee about tomorrow. CI runs
`assembleDebug test` only, so a `presentation` file importing a DTO would compile,
pass, and merge without anyone noticing. The boundary is enforced by the author
remembering it exists.

The obvious fix is to split into modules and let the compiler enforce what the
convention currently asks for. Before doing that it is worth asking what
modularisation actually buys at this size.

| What modules usually buy | Does it apply here |
|---|---|
| Parallel and incremental builds | No. 4,374 lines; a clean `assembleDebug` is ~35s |
| Ownership boundaries between teams | No. Single developer |
| Reuse across applications | No. One application |
| Compiler-enforced layering | **Yes** |

One of four. And the one that applies is available more cheaply.

## Decision

Stay on one module. Address the layering gap directly rather than as a side
effect of a build-graph change.

**Enforce the boundary with an architecture test.** Konsist runs as an ordinary
JVM unit test, so it joins the existing gate with no Gradle ceremony:

```kotlin
@Test
fun `domain does not depend on outer layers`() {
    Konsist.scopeFromProject()
        .files
        .withPackage("..domain..")
        .assertFalse {
            it.hasImport { imp ->
                imp.name.contains(".data.") || imp.name.contains(".presentation.")
            }
        }
}
```

A dozen lines converts "holds by discipline" into "holds by CI", and the failure
message names the offending file and import — more useful to the person who broke
it than an unresolved-reference error would be.

**Extract `:feature:calculator` when, and only when, the sync story materialises.**
That directory is not written for this app. It was ported from the Moji project's
`release/1.5.0`, tests and all, and the intent is to keep pulling upstream changes.
That is a genuine cross-project boundary — the only one in the codebase — and a
module would make each sync a clean diff against a known surface (`Calculator`,
`CalculatorUI`) instead of a directory-shaped merge.

It is deferred rather than done because the boundary is currently maintained by
copying files, and copying files works fine. The trigger to revisit is the second
or third sync, when the diff noise starts costing more than the module setup would.

**Do not split `:core:domain` / `:core:data` yet.** They would be ceremony at this
size. If a reason appears — a second application, a KMP target, a genuine need to
build the domain without Android — that reason will make the split obvious, and it
can be done then with the layering already proven clean by the test above.

## Consequences

The layering gap is real until the Konsist test exists. This record does not close
it; it only says the fix is a test rather than a build graph. Until that test is
written, a `Dto` in `presentation` still compiles.

A reviewer looking for a multi-module Android project will not find one, and may
read that as a gap in the author's experience rather than a decision. That risk is
the reason this record exists: the answer to "why is this one module?" should be a
document, not a shrug.

The choice is also reversible in a way the opposite is not. Splitting a clean
single module later is mechanical. Un-splitting a premature module graph — undoing
the `api`/`implementation` decisions, the duplicated Gradle config, the
convention plugins written to manage it — is not.

What this arrangement does not give us is the forcing function. A module boundary
makes a violation impossible; a test makes it *caught*, and only if the test is
kept honest as the codebase grows. That is a weaker guarantee, accepted knowingly,
in exchange for not paying build-graph costs that nothing in this project's size
or shape currently justifies.
