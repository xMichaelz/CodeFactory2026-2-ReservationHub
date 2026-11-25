# ReservaHub — Acceptance tests (Serenity BDD + Screenplay + Cucumber)

Automated acceptance tests for the ReservaHub web application, written with **Serenity BDD**, the **Screenplay** pattern and **Cucumber**. The Gherkin is in **English**; the quoted values inside the steps (button labels, messages, day names) stay in Spanish because they are exactly what the application UI shows.

Every scenario is tagged with the user story it verifies (`@HU-xx`) and, when it maps to an acceptance criterion of that story in Azure DevOps, with the criterion id (`@CA-HUxx-nn`). The criteria live in the HU work items of the board (EAP10); this suite is their executable version.

- **72 scenarios** across 20 feature files, covering the 21 user stories (HU-01 … HU-21).
- **3 end-to-end journeys** (`features/e2e`) that cross several user stories and both roles.
- 50 of the 119 acceptance criteria are verified through the browser (see the matrix below). Some of the others are covered at API/integration level by the backend tests (see `docs/quality/sprint-3.feature`); the rest are still pending automation — see the coverage notes.

## Requirements

- JDK 17 or later (tested with JDK 21)
- Google Chrome
- The application running. From the repository root:

  ```bash
  docker compose up -d --build
  ```

  Frontend at http://localhost:3001 (the tests point there by default).

Gradle does not need to be installed: use the wrapper (`gradlew` / `gradlew.bat`).

## Running

```bash
cd acceptance-tests

./gradlew clean test                                           # everything, visible browser
./gradlew clean test -Dheadless.mode=true                      # no window
./gradlew clean test -Dcucumber.filter.tags="@HU-12"           # a single user story
./gradlew clean test -Dcucumber.filter.tags="@CA-HU15-02"      # a single acceptance criterion
./gradlew clean test -Dcucumber.filter.tags="@client"          # one area
./gradlew clean test -Dcucumber.filter.tags="@e2e"             # only the complete journeys
./gradlew clean test -Dcucumber.filter.tags="@provider and @reports"
./gradlew clean test -Dcucumber.filter.name="books the first free slot"
./gradlew clean test -Dreservahub.url=http://localhost:5173    # against `npm run dev`
```

On Windows (PowerShell/cmd) use `gradlew.bat` with the same arguments.

`DEMO_DELAY` (milliseconds) sets the demo pauses between actions: `DEMO_DELAY=1500` to present slowly, `DEMO_DELAY=0` to run as fast as possible.

### Report

Serenity generates the HTML report with screenshots at `target/site/serenity/index.html`. The plugin adds it when `test` finishes; if it does not appear, generate it with `./gradlew aggregate`. Because of the tags, the report can be browsed by user story and by tag.

## Structure

```text
acceptance-tests/
├── build.gradle · settings.gradle · gradle.properties · serenity.properties · gradlew
└── src/
    ├── main/java/co/edu/udea/calidad/reservahub/
    │   ├── exceptions/       # ApiPreconditionException, ActorStateException
    │   ├── interactions/     # OpenCleanSession, OpenPage, Pause, SetFieldValue, ChooseDate, WaitUntilLoaded
    │   ├── models/           # ClientAccount, ProviderAccount, ServiceInfo, ResourceInfo, Reservation
    │   ├── questions/        # TextOf, TextsOf, CurrentPage, ToastMessage, ... (what the actor observes)
    │   ├── tasks/            # LoginAs, RegisterAsClient, NavigateTo, ... (what the actor does in the UI)
    │   │   └── api/          # data setup through the API: accounts, services, schedules, reservations
    │   ├── userinterfaces/   # Targets per page: CommonUI, AccessUI, ...
    │   └── utils/            # Pages (routes), TestData (unique data), ActorMemory (actor memory)
    └── test/
        ├── java/co/edu/udea/calidad/reservahub/
        │   ├── runners/TestRunner.java
        │   └── stepdefinitions/   # Hooks, Actors, common steps, preconditions and one per area
        └── resources/
            ├── serenity.conf
            └── features/
                ├── access/        # login, logout, lockout, client/provider registration, role permissions
                ├── client/        # home, catalog, booking, my reservations, history
                ├── provider/      # dashboard, services, resources, schedules, agenda, book for client,
                │                  # business history, reports
                └── e2e/           # complete journeys: client, provider and both together
```

## How the scenarios are written

- **Actors**: "the client", "the provider", "another client" and "someone" (a visitor without an account), defined in `stepdefinitions/Actors.java`. `Then` steps ("they should ...") are answered by the actor who acted last (the one in the spotlight).
- **`Given` through the API, `When`/`Then` through the UI**: the preconditions (`PreconditionStepDefinitions`) create accounts, services, schedules and reservations by calling the API with the `CallAnApi` ability. It is faster and does not depend on other screens. What is being tested happens in the browser.
- **Unique data**: emails, services and resources carry a per-run suffix (`TestData.unique()`), so the suite can be run many times against the same database.
- **Always-open agenda**: the provider created by the preconditions works 00:00–23:30 every day, so free slots exist at any time of day the tests run. Dates are computed in the `America/Bogota` zone, like the API.
- **Stable selectors**: the `Target`s use the frontend's `name` and `data-testid` attributes. CSS selectors carry the `css:` prefix and XPaths start with `//`.

## Tags

| Tag | What it selects |
|-----|-----------------|
| `@HU-xx` | every scenario that verifies user story HU-xx (e.g. `@HU-12`) |
| `@CA-HUxx-nn` | the scenarios that verify acceptance criterion *nn* of HU-xx, using the same id as the criteria in the board |
| `@access` | `@login`, `@registration` (`@client_registration`, `@provider_registration`), `@permissions` |
| `@client` | everything the client does: `@client_home`, `@catalog`, `@booking`, `@my_reservations`, `@client_history` |
| `@provider` | everything the provider does: `@provider_dashboard`, `@services`, `@resources`, `@schedules`, `@agenda`, `@book_for_client`, `@business_history`, `@reports` |
| `@e2e` | complete journeys: `@e2e_client`, `@e2e_provider`, `@e2e_full_booking` |

## End-to-end journeys

| Feature | Journey | User stories it crosses |
|---------|---------|-------------------------|
| `e2e/client_e2e.feature` | register → search the catalog → book → reschedule → cancel → review history | HU-01, 11, 12, 16, 14, 17 |
| `e2e/provider_e2e.feature` | register the business → resource → service → link → schedules → block → book for a client → agenda → demand and occupancy reports | HU-02, 05, 08, 09, 10, 13, 19, 20 |
| `e2e/full_booking_e2e.feature` | the provider publishes a service → a client books it → the provider cancels with a reason → both sides see the result | HU-01, 02, 05, 08, 11, 12, 15, 17, 18 |

## Quality principles (FIRST)

| Principle | How the suite applies it |
|-----------|--------------------------|
| **Fast** | Preconditions go through the API instead of the UI; only the behavior under test uses the browser. `-Dheadless.mode=true` removes the window and `DEMO_DELAY=0` removes the demo pauses. |
| **Independent** | Each scenario creates its own accounts and data with a unique suffix and starts from a clean browser session (`serenity.restart.browser.for.each = scenario`). No scenario depends on another one or on the execution order. |
| **Repeatable** | Unique data per run, dates computed relative to "today" in `America/Bogota`, and an always-open agenda, so the same suite passes on any day, at any hour, on the same database. |
| **Self-validating** | Every scenario ends in `Then` steps with explicit assertions (`seeThat(...)`); the result is pass/fail in the console and in the Serenity report, with no manual inspection. |
| **Timely** | Scenarios are written from the acceptance criteria of each user story (tags `@HU-xx` / `@CA-HUxx-nn`), so each story ships with its automated verification. |

## Traceability: user story → acceptance criteria → scenarios

| Epic | HU | User story | Acceptance criteria covered by the UI suite | Scenarios |
|------|----|------------|---------------------------------------------|-----------|
| EP-01 | HU-01 | Client registration | 3 of 4 (CA-01, CA-02, CA-03) | 7 |
| EP-01 | HU-02 | Provider registration | 1 of 4 (CA-01) | 4 |
| EP-01 | HU-03 | Login | 6 of 9 (CA-01, CA-02, CA-03, CA-04, CA-05, CA-07) | 10 |
| EP-01 | HU-04 | Logout | 2 of 2 (CA-01, CA-02) | 3 |
| EP-02 | HU-05 | Create service | 1 of 3 (CA-01) | 10 |
| EP-02 | HU-06 | Edit service | 1 of 2 (CA-01) | 1 |
| EP-02 | HU-07 | Deactivate service | 1 of 2 (CA-01) | 2 |
| EP-02 | HU-08 | Define opening hours | 3 of 3 (CA-01, CA-02, CA-03) | 7 |
| EP-02 | HU-09 | Block a specific period | 2 of 5 (CA-01, CA-03) | 3 |
| EP-02 | HU-10 | Register resource | 2 of 5 (CA-01, CA-05) | 5 |
| EP-03 | HU-11 | Check availability | 1 of 9 (CA-01) | 3 |
| EP-03 | HU-12 | Create reservation | 1 of 8 (CA-01) | 4 |
| EP-03 | HU-13 | Book on behalf of a client | 2 of 6 (CA-01, CA-03) | 4 |
| EP-03 | HU-14 | Cancel own reservation | 2 of 6 (CA-01, CA-06) | 3 |
| EP-03 | HU-15 | Cancel a client's reservation | 2 of 6 (CA-01, CA-02) | 4 |
| EP-03 | HU-16 | Reschedule reservation | 2 of 11 (CA-01, CA-02) | 3 |
| EP-04 | HU-17 | Client reservation history | 5 of 7 (CA-01, CA-03, CA-05, CA-06, CA-07) | 11 |
| EP-04 | HU-18 | Business reservation history | 3 of 6 (CA-01, CA-02, CA-05) | 4 |
| EP-04 | HU-19 | Agenda occupancy | 3 of 9 (CA-01, CA-07, CA-09) | 4 |
| EP-04 | HU-20 | Service demand | 3 of 6 (CA-01, CA-05, CA-06) | 4 |
| EP-04 | HU-21 | Cancellations | 4 of 6 (CA-01, CA-02, CA-05, CA-06) | 4 |

Detail per user story:

**HU-01 – Client registration**

- `access/client_registration.feature` — Successful registration lands on the catalog with the session started
- `access/client_registration.feature` — Registration is rejected with a short password
- `access/client_registration.feature` — Registration is rejected with a password without numbers
- `access/client_registration.feature` — Registration is rejected if the passwords do not match
- `access/client_registration.feature` — Registration is rejected with an already registered email
- `e2e/client_e2e.feature` — Register, book, reschedule, cancel and review the history
- `e2e/full_booking_e2e.feature` — The provider publishes a service, the client books it and the provider cancels it with a reason

**HU-02 – Provider registration**

- `access/provider_registration.feature` — Successful registration lands on the business dashboard
- `access/provider_registration.feature` — Registration is rejected with an already registered business name
- `e2e/full_booking_e2e.feature` — The provider publishes a service, the client books it and the provider cancels it with a reason
- `e2e/provider_e2e.feature` — Register the business, set it up, book for a client and review the reports

**HU-03 – Login**

- `access/login.feature` — The client logs in and lands on their home page
- `access/login.feature` — The provider logs in and lands on the business dashboard
- `access/login.feature` — Login is rejected with a wrong password
- `access/login.feature` — Login is rejected with an unregistered email
- `access/login.feature` — The account is locked after 5 failed attempts
- `access/login.feature` — A successful login resets the failed attempts counter
- `access/role_permissions.feature` — Without a session, a client page redirects to login
- `access/role_permissions.feature` — Without a session, a provider page redirects to login
- `access/role_permissions.feature` — A client cannot open the provider pages
- `access/role_permissions.feature` — A provider cannot open the client pages

**HU-04 – Logout**

- `access/login.feature` — Logging out returns to the login page and protects private pages
- `access/role_permissions.feature` — Without a session, a client page redirects to login
- `access/role_permissions.feature` — Without a session, a provider page redirects to login

**HU-05 – Create service**

- `client/catalog.feature` — The catalog shows the provider's active service
- `client/catalog.feature` — Searching by service name finds it
- `client/catalog.feature` — A search with no matches shows the empty notice
- `client/catalog.feature` — Filtering by provider shows only that business's services
- `e2e/full_booking_e2e.feature` — The provider publishes a service, the client books it and the provider cancels it with a reason
- `e2e/provider_e2e.feature` — Register the business, set it up, book for a client and review the reports
- `provider/provider_dashboard.feature` — A new provider sees the pending setup checklist
- `provider/provider_dashboard.feature` — With a service and schedules the dashboard counts one active service
- `provider/services.feature` — The provider creates a service and it is active
- `provider/services.feature` — A service with an already existing name cannot be created

**HU-06 – Edit service**

- `provider/services.feature` — The provider changes the price of a service

**HU-07 – Deactivate service**

- `client/catalog.feature` — A deactivated service does not appear in the catalog
- `provider/services.feature` — The provider deactivates a service

**HU-08 – Define opening hours**

- `e2e/full_booking_e2e.feature` — The provider publishes a service, the client books it and the provider cancels it with a reason
- `e2e/provider_e2e.feature` — Register the business, set it up, book for a client and review the reports
- `provider/provider_dashboard.feature` — A new provider sees the pending setup checklist
- `provider/provider_dashboard.feature` — With a service and schedules the dashboard counts one active service
- `provider/schedules.feature` — The provider adds a window and sees it on its day
- `provider/schedules.feature` — A window that overlaps another one on the same day cannot be added
- `provider/schedules.feature` — The window start time must be before the end time

**HU-09 – Block a specific period**

- `e2e/provider_e2e.feature` — Register the business, set it up, book for a client and review the reports
- `provider/schedules.feature` — The provider blocks part of a window with a reason
- `provider/schedules.feature` — The block must fall within the window's hours

**HU-10 – Register resource**

- `e2e/provider_e2e.feature` — Register the business, set it up, book for a client and review the reports
- `provider/resources.feature` — The provider registers a resource and it appears in their list
- `provider/resources.feature` — A resource with an already existing name cannot be registered
- `provider/resources.feature` — The provider cannot register a resource with quantity 0
- `provider/services.feature` — The provider links a resource to a service

**HU-11 – Check availability**

- `client/book_service.feature` — The client navigates to the next week of availability
- `e2e/client_e2e.feature` — Register, book, reschedule, cancel and review the history
- `e2e/full_booking_e2e.feature` — The provider publishes a service, the client books it and the provider cancels it with a reason

**HU-12 – Create reservation**

- `client/book_service.feature` — The client books the first free slot and sees the confirmation
- `client/book_service.feature` — The booking appears as confirmed in My reservations
- `e2e/client_e2e.feature` — Register, book, reschedule, cancel and review the history
- `e2e/full_booking_e2e.feature` — The provider publishes a service, the client books it and the provider cancels it with a reason

**HU-13 – Book on behalf of a client**

- `e2e/provider_e2e.feature` — Register the business, set it up, book for a client and review the reports
- `provider/book_for_client.feature` — Looking up an email that does not belong to any client
- `provider/book_for_client.feature` — Looking up a registered client shows their name
- `provider/book_for_client.feature` — The reservation made for the client ends up in the agenda

**HU-14 – Cancel own reservation**

- `client/my_reservations.feature` — The client cancels their reservation giving a reason
- `client/my_reservations.feature` — The client cancels their reservation without giving a reason
- `e2e/client_e2e.feature` — Register, book, reschedule, cancel and review the history

**HU-15 – Cancel a client's reservation**

- `e2e/full_booking_e2e.feature` — The provider publishes a service, the client books it and the provider cancels it with a reason
- `provider/agenda.feature` — The provider cannot cancel a reservation without writing a reason
- `provider/agenda.feature` — A reason made only of spaces is not valid for cancelling
- `provider/agenda.feature` — The provider cancels a reservation giving the reason

**HU-16 – Reschedule reservation**

- `client/my_reservations.feature` — The client reschedules their reservation to another free slot
- `e2e/client_e2e.feature` — Register, book, reschedule, cancel and review the history
- `provider/agenda.feature` — The provider reschedules a reservation to another slot

**HU-17 – Client reservation history**

- `access/role_permissions.feature` — Without a session, the client history page redirects to login
- `access/role_permissions.feature` — A provider cannot open the client history
- `client/client_home.feature` — The home page shows the greeting and the next booked appointment
- `client/history.feature` — The history shows the pending and the cancelled reservation
- `client/history.feature` — The "Cancelada" tab shows only cancelled reservations
- `client/history.feature` — The "Pendiente" tab shows only upcoming reservations
- `client/my_reservations.feature` — The client sees their confirmed reservation
- `client/my_reservations.feature` — A date range with no reservations shows the empty notice
- `client/my_reservations.feature` — A range longer than 31 days is rejected before querying
- `e2e/client_e2e.feature` — Register, book, reschedule, cancel and review the history
- `e2e/full_booking_e2e.feature` — The provider publishes a service, the client books it and the provider cancels it with a reason

**HU-18 – Business reservation history**

- `access/role_permissions.feature` — A client cannot open the business history
- `e2e/full_booking_e2e.feature` — The provider publishes a service, the client books it and the provider cancels it with a reason
- `provider/agenda.feature` — The agenda shows the reservation with the client's name
- `provider/business_history.feature` — The history lists confirmed and cancelled reservations with their client

**HU-19 – Agenda occupancy**

- `access/role_permissions.feature` — A client cannot open the provider reports
- `e2e/provider_e2e.feature` — Register the business, set it up, book for a client and review the reports
- `provider/reports.feature` — The occupancy report shows the service
- `provider/reports.feature` — The range of the reports cannot exceed 31 days

**HU-20 – Service demand**

- `access/role_permissions.feature` — A client cannot open the provider reports
- `e2e/provider_e2e.feature` — Register the business, set it up, book for a client and review the reports
- `provider/reports.feature` — The demand report counts the confirmed reservations
- `provider/reports.feature` — The range of the reports cannot exceed 31 days

**HU-21 – Cancellations**

- `access/role_permissions.feature` — A client cannot open the provider reports
- `provider/reports.feature` — The cancellations report separates the client's and the provider's cancellations
- `provider/reports.feature` — Cancellations can be filtered by service
- `provider/reports.feature` — The range of the reports cannot exceed 31 days

## Coverage notes

The criteria not tagged above are not covered by this UI suite. They are either verified at API/integration level (reports, concurrency, authorization by token and role) or still pending automation in the browser (for example: concurrency on the last slot, resource capacity, session expiry after 60 minutes, lockout release after 15 minutes). Tag a new scenario with its `@CA-HUxx-nn` id and the matrix above will pick it up.
