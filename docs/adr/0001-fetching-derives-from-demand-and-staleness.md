# 1. Fetching derives from demand and staleness

Accepted · 2026-07-26

## Context

The flight board has to stay close to reality, so something has to decide when to
call the API. The original code answered that with four separate events, each of
which called `load()`:

- the ViewModel being constructed (`init { load() }`)
- the screen appearing (`DisposableEffect` → `enableAutoRefresh()`)
- a ten-second timer firing
- pull-to-refresh

Because four independent things could all trigger a fetch, they needed an
`allowAutoRefresh` flag to coordinate. They still overlapped: `init` and
`enableAutoRefresh()` both fired when the screen opened, so every visit hit the
API twice.

Two cases were missing entirely. `DisposableEffect(Unit)` fires when a composable
leaves the composition — which does not happen when the app goes to the
background. So the timer kept firing every ten seconds for a UI that had stopped
collecting, and coming back to the foreground did not refresh; the user saw
whatever was last fetched until the next tick.

Approximating a condition with events is what forced the flag, and what let those
two cases slip through: you only handle the events you thought of.

## Decision

State the condition instead:

> **fetch = someone is watching ∧ the data is stale**

Both factors are state rather than events, so both are expressed directly:

```kotlin
val state = flow {
    while (true) {
        emit(load())
        withTimeoutOrNull(FRESHNESS) { invalidated.receive() }
    }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE), Loading)
```

`WhileSubscribed` is the demand factor. The wait inside the loop is the staleness
factor: the freshness window lapsing, or the user invalidating the data through
`refresh()`.

## Consequences

Nothing fetches until something collects the state, so construction stops being a
trigger and the duplicate initial request disappears. Fetching now stops when the
app is backgrounded and resumes on return, because collection is the signal —
neither case needs its own handler.

"A manual refresh restarts the countdown" needs no special handling: the loop
re-enters and the window is measured from the new fetch. `ReTimer` — a
conflated-channel timer that could be reset from outside without cancelling a
coroutine — existed to make that work under the event model, and was deleted.

`FlightsScreen` no longer takes `onScreenVisible`/`onScreenHidden`. The
`entry<NavRoute.Flights>` block in `navigation/AppNavDisplay.kt` collects state
and passes the collected `FlightArrivalsUiState` to the screen; that composition
scope is the whole contract.

The trade-off is that `WhileSubscribed(5_000)` does not remember when the last
fetch happened, so leaving for more than five seconds always refetches even if
the data is technically still fresh. For a ten-second window that is fine — being
away that long means the data is more than half stale. Tracking `lastFetchAt`
would fix it and bring back the bookkeeping this decision removed.

`CurrencyViewModel` deliberately does **not** follow this pattern; see ADR 0003.
