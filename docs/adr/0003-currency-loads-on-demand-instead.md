# 3. Currency loads on demand instead

Accepted · 2026-07-26

## Context

ADR 0001 replaced event-driven fetching in `FlightsViewModel` with a condition
expressed through `stateIn(WhileSubscribed)`. `CurrencyViewModel` had the same
constructor-side-effect problem — it launched a request from `init`, with a
comment from the author conceding the anti-pattern — so the obvious move was to
apply the same fix.

It does not transfer. The two screens hold state for different reasons.

Flight state is entirely derived from upstream: the repository produces a list,
the UI renders it, and nothing the user does changes it except asking for a
refresh. Currency state is edited in place. Selecting currencies, choosing a base,
and typing an amount all call `_state.update {}`, and those edits have to survive.

A `stateIn` flow owns the value it emits. Any re-emission — a resubscribe after
the grace period, or anything that restarts the upstream — would overwrite the
user's selections with a freshly loaded list. Keeping both would mean folding
every user action into the flow as an input, which is a larger restructuring than
this change warranted, and would not obviously be an improvement for a screen
whose state is mostly user-owned.

## Decision

Drop the `init` block, expose `load()`, and call it once from the screen's
`LaunchedEffect(Unit)`. A `hasLoaded` flag makes it idempotent, since that effect
re-runs whenever the composable re-enters the composition — returning from the
flights tab, for instance.

## Consequences

The project now has two loading patterns. That is a real cost: two conventions in
one codebase is the thing ADR 0002 argued against, and a reviewer who spots the
difference without knowing why will read it as inconsistency. Hence this record.

The distinction is not "flights got the good one and currency did not". It is
whether the state is owned by the upstream or by the user:

| | Flights | Currency |
|---|---|---|
| Who owns the state | the repository | the user, after the first load |
| Refetches periodically | yes, every ten seconds | no — rates do not move that fast |
| Fix | `stateIn(WhileSubscribed)` | `load()` from `LaunchedEffect(Unit)` |

`hasLoaded` is a flag, and ADR 0001 removed a flag. They are not the same kind:
`allowAutoRefresh` existed to arbitrate between four independent triggers, while
`hasLoaded` guards a single trigger against re-entry. The first was a symptom of
modelling a condition as events; the second is just idempotence.

What this arrangement does not give us is refresh-on-return. Currency data
fetched at breakfast is still on screen at dinner. Rates move slowly enough that
this is tolerable for now, and the fix — a freshness check inside `load()` — is
small when it stops being tolerable.
