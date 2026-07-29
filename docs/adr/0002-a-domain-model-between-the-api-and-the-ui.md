# 2. A domain model between the API and the UI

Accepted · 2026-07-26

## Context

Two conventions were in play at once. Currency had a domain model and its
repository returned it. Flights did not: `FlightsRepository` — declared in
`:core:domain` — imported the DTO and returned `Result<List<InstantScheduleDomesticArrivalDtoItem>>`,
which the UI mapper then consumed directly.

That mattered more than it looks, because the project had picked a side. Putting
the repository interface in `:core:domain` is the dependency-inversion arrangement from
Clean Architecture, where source dependencies point inward and the DTO is an
outer-layer concern. Declaring the interface there while its signature returns a
DTO is inversion in form only: `:core:domain` still knows about `:core:data`. One codebase
with two conventions is worse than either convention applied consistently,
because a reviewer cannot tell which one is intended.

The cost showed up in `FlightUiMapper`, which did two unrelated jobs. Turning
`"抵達"` / `"ARRIVED"` / `"延誤"` into a known set of states is business meaning.
Turning a missing time into `"--:--"` is presentation. Both lived in the
presentation layer, so a change to the API's field names would have reached the
UI.

## Decision

Introduce `FlightArrival` and `FlightStatus` in `:core:domain`; have
`:core:data`'s `FlightsRepositoryImpl` map DTO → domain; leave `FlightUiMapper` with formatting
only.

Field types follow from what the source actually carries:

- **Times are `LocalTime?`.** The API gives `"09:00"` with no date and no zone,
  and an arrivals board shows local wall-clock time anyway. `Instant` would
  require inventing a date and a zone — and would get cross-midnight arrivals
  wrong. `String` would be under-specified: the list is sorted by time, and
  sorting is a value operation, so it needs a value type. That `"09:00" < "10:00"`
  happens to hold as a string comparison is an accident of zero-padding.
- **Status is a sealed type**, with `Unknown(raw)` keeping the original wording
  rather than discarding it. Colour and icon stay in presentation: which state a
  flight is in is a fact, how it is drawn is not.

The rule this leaves behind: a field that is only ever printed can stay a
`String` (`airBoardingGate` does); a field that is compared, validated or
computed with needs a value type.

## Consequences

`:core:domain` no longer imports anything from `:core:data`, and the DTO exists only inside
`:core:data`'s `datasource/`. Parsing and normalisation happen once, at the boundary, rather
than at each point of use.

`FlightsRepositoryImpl` now has a reason to exist. It used to hand the data
source through untouched, which is an indirection a reviewer would rightly ask
about.

Mapping surfaced a bug. The old code assumed `airLineNum` might repeat the
`airLineCode` prefix and stripped it. In the live data `airLineNum` is already a
complete IATA number (`B78690`) while `airLineCode` is the unrelated ICAO code
(`UIA`), so the branch never matched — across all 34 records — and the UI rendered
`UIA B78690`, an identifier that does not exist. A regression test pins it.

The cost is one more model and one more mapping step per feature. For a
two-screen app that is close to the point where it stops paying for itself; it is
justified here because the project claims this arrangement in its own
documentation, and consistency with a stated rule is worth more than the layer
saved.
