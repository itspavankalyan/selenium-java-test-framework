# Selenium Java Test Automation Framework

[![CI](https://github.com/itspavankalyan/selenium-java-test-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/itspavankalyan/selenium-java-test-framework/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17-orange)
![Selenium](https://img.shields.io/badge/Selenium-4.27-green)
![TestNG](https://img.shields.io/badge/TestNG-7.10-blue)

A layered test automation framework built to demonstrate five core SDET/QA
automation skills in one integrated project, rather than as five disconnected
scripts:

1. **Page Object Model (POM) UI framework**
2. **Data-driven testing**
3. **REST API test automation with request chaining**
4. **Database-level validation**
5. **CI/CD pipeline integration**

## Why this project exists

Most portfolio automation repos are a grab-bag of unrelated scripts — a login
test here, an isolated API GET there. This repo is built the other way
around: one framework, where each layer builds on the one below it, closer to
how a real test framework grows inside a company. The goal is a repo that
holds up under a code review, not just a repo that runs.

## Architecture

```
selenium-java-test-framework/
├── pom.xml                          # Maven build, dependency versions, Surefire config
├── testng.xml                       # Full suite (all 3 layers)
├── testng-ui.xml / -api.xml / -db.xml  # Per-layer suites (used by CI)
├── .github/workflows/ci.yml         # GitHub Actions pipeline
├── src/test/resources/
│   ├── config.properties            # Central config (browser, base URLs, DB, etc.)
│   ├── testdata/login_test_data.csv # Data-driven login scenarios
│   └── db/schema.sql                # H2 audit table DDL
└── src/test/java/com/portfolio/
    ├── framework/
    │   ├── base/       # DriverFactory, BaseTest (ThreadLocal WebDriver), ScreenshotUtil
    │   ├── config/     # ConfigReader
    │   ├── pages/       # Page objects (BasePage, LoginPage, InventoryPage)
    │   ├── utils/       # CsvTestDataReader, LoginCredentials
    │   ├── api/         # BookingApiClient + request/response models
    │   └── db/          # DbConnectionManager, BookingAuditRepository
    └── tests/
        ├── ui/   LoginTest.java                    (Layer 1 + 2)
        ├── api/  BookingApiChainTest.java           (Layer 3)
        └── db/   BookingDatabaseValidationTest.java (Layer 4)
```

## The five layers

### 1 & 2 — Page Object Model + data-driven UI tests

**Target:** [saucedemo.com](https://www.saucedemo.com), a site purpose-built
for Selenium practice.

`LoginTest` runs a single `@Test` method through a TestNG `@DataProvider`
backed by [`login_test_data.csv`](src/test/resources/testdata/login_test_data.csv)
— 14 scenarios covering saucedemo's known accounts (standard, locked-out,
problem, performance-glitch, error, visual users), invalid credentials,
missing-field validation, and case/whitespace boundary cases. Adding a new
scenario is a one-line CSV addition, not a new test method.

Locators live only inside page objects (`LoginPage`, `InventoryPage`) — tests
never touch a `By` selector directly. `BaseTest` stores the `WebDriver` in a
`ThreadLocal`, so the suite is safe to run in parallel without one test's
browser session leaking into another's.

### 3 — REST API test chaining

**Target:** [restful-booker](https://restful-booker.herokuapp.com), a public
practice API purpose-built for API test automation (also used to model the
"real backend" pattern in `BookingApiClient`).

`BookingApiChainTest` runs one continuous flow rather than four isolated
CRUD tests: **authenticate → create → get → full update (PUT) → partial
update (PATCH) → delete → confirm 404**. Each step feeds its output (the
booking id, the auth token) into the next via `dependsOnMethods`, which is
what actually proves the API behaves correctly *across* calls — isolated
tests can't catch a bug where, say, an id from `create` doesn't round-trip
through `get`.

> **Debugging note left in on purpose:** during development, `createBooking`
> intermittently returned `418 I'm a teapot`. Diffing the raw wire request
> against a working `curl` call showed the actual cause: RestAssured's
> `.contentType(ContentType.JSON)` helper sends a multi-value `Accept` header
> that restful-booker's server rejects. The fix (see `BookingApiClient.baseRequest()`)
> was to set literal `Accept`/`Content-Type` headers instead of retrying —
> root-causing a flaky-looking failure instead of masking it.

### 4 — Database validation

**Target:** a local, file-backed H2 database owned by the framework.

`BookingDatabaseValidationTest` creates a booking through the API, then
performs a genuine JDBC write + read against a real database to confirm the
data landed correctly — the same shape of check a real team runs when
reconciling an API against a downstream data warehouse or read replica.

**Honest scope note:** restful-booker is a shared public demo API and, for
good reason, doesn't expose its own backing database to external clients. To
avoid faking this layer, the framework owns a real local H2 database
(`booking_audit` table, see [`schema.sql`](src/test/resources/db/schema.sql))
that plays the role of that downstream system. Every insert/update/delete/
query in this layer is a real JDBC round-trip against a real database — only
the *source of truth being reconciled against* is a stand-in for
infrastructure a public demo API can't reasonably provide.

### 5 — CI/CD pipeline

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push/PR
to `main` as three independent jobs — one per layer — so a UI flake doesn't
mask whether the API and DB layers are green:

- **ui-tests** — headless Chrome against saucedemo.com; uploads a screenshot
  artifact automatically on any failure (see `ScreenshotUtil`)
- **api-tests** — the restful-booker chain
- **db-tests** — the H2 validation flow

Each job uploads its Surefire report as a build artifact regardless of
outcome.

## Running locally

```bash
# Full suite (all three layers)
mvn test

# One layer at a time
mvn test -Dsurefire.suiteXmlFiles=testng-ui.xml
mvn test -Dsurefire.suiteXmlFiles=testng-api.xml
mvn test -Dsurefire.suiteXmlFiles=testng-db.xml

# Override any config.properties value at runtime, e.g. run headed Chrome locally
mvn test -Dsurefire.suiteXmlFiles=testng-ui.xml -Dheadless=false -Dbrowser=chrome
```

Requires Java 17+ and Maven. No local browser driver setup needed —
`WebDriverManager` resolves the correct chromedriver/geckodriver at runtime.

## Tech stack

| Concern              | Choice                                    |
|-----------------------|-------------------------------------------|
| Language / build      | Java 17, Maven                            |
| UI automation         | Selenium WebDriver 4, WebDriverManager    |
| Test runner           | TestNG (data providers, dependsOnMethods) |
| API testing           | REST Assured                              |
| Database              | H2 (file mode), plain JDBC                |
| Test data             | OpenCSV, Jackson                          |
| Logging               | SLF4J + Logback                           |
| CI                    | GitHub Actions                            |
