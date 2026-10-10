# Profiles and configuration: findings and a proposed convention

- **What this is.** The findings of [#48](https://github.com/MRISS-Projects/dsh/issues/48): how
  Spring profiles, Maven and deploy-time values should share the work of configuring DSH by
  capability and by tier. Its spec is `specs/stories/48-investigate-spring-and-maven-profiles.md`.
- **What is measured.** Sections 3 and 4. Every Answer there was observed by running a probe on
  2026-10-07, and the output is shown. Where an Answer or a Consequence goes past the output, it
  says so.
- **What is proposed.** Section 6, the convention. **It is proposed, not in force.** ADR-003 and
  ADR-004 decide. Nothing in this document has been implemented.
- **What is only cited.** Section 8, the GCP facts. No GCP project exists yet, so each is taken
  from documentation and labelled **Not verified by a run**.

## 1. Summary

1. **A Spring profile should name a capability, not a tier.** One jar then serves every tier. A
   Maven profile per tier produces a different jar for each (P27), and a Spring profile per tier
   needs a new file for every new name, which is a change to the jar when the files are kept in it
   (section 5).
2. **The tier is one value, a base name, set where the application is deployed.** With
   `PROBE_NAME=dsh-dev` in the environment, every name derived from it followed at run time, from
   an unchanged jar (P10, P27).
3. **A profile supplies defaults, and a property selects each implementation.** `gcp`'s file set
   the transport, and one argument still overrode it (P7). That is what lets a local run put some
   services on emulators and others on GCP.
4. **Maven must not filter Spring configuration with `${…}`.** DSH does today. A Maven property
   with the same name as a Spring placeholder replaced it at build time, and no run-time value
   could change it back (P18). Restricting filtering to `@…@` removed the hazard (P20).
5. **`mongo.*` is that hazard in its harshest form.** A property file that refers to itself fails
   every context with `Circular placeholder reference` unless the build supplies a value (P19).
   The value is not needed at build time at all: the XML takes it from an argument or an
   environment variable first (P9).
6. **A library module must not ship `application*.properties`.** The application's file hid the
   library's whole file; nothing was merged (P0, P17). A library's defaults belong in a file named
   after the module, loaded by `@PropertySource`, which everything else overrides (P30).
7. **Every context test must name its profile.** A profile leaked from the shell or from the Maven
   command line reached the test JVM and changed which beans the test got (P22). Only a test with
   a named `@ActiveProfiles` was immune (P23, P29).
8. **A test `application.properties` replaces the main one entirely** (P24). That is why
   `dsh-rest-api`'s repeats it line for line. `application-test.properties` merges instead (P25).
9. **The build may stamp the version and the commit, and nothing else.** The running application
   read both (P21). The release gate and the production smoke check need exactly these.
10. **`dsh` cannot be a Firestore database id.** An id needs at least four characters (G2). The
    convention derives the id from the base name with a suffix.
11. **Scale to zero constrains the pipeline on GCP.** With zero minimum instances and request-based
    billing, CPU is allocated only while a request is processed (G8). Each stage has to be driven
    by a request, which points at Pub/Sub delivering by push.

All 12 predictions in the spec were confirmed. Six probes were added during the run, and two were
run differently from the plan; the appendix lists them. Five points the documentation does not
settle are open (section 8.3). Two of them need a first deploy.

## 2. DSH today

On `DEVELOP` at `a760552f8`.

| Fact | Evidence |
|---|---|
| Spring profiles are not used. No `@Profile`, no `application-<profile>.properties`, no `spring.profiles.*` setting in any module | a grep over every `pom.xml`, `*.java`, `*.properties` and `*.yml` |
| One activation exists and selects nothing: `--spring.profiles.active=test` | `.github/workflows/api-testing.yml:240`. P31 shows such a profile is accepted in silence |
| The Maven profiles select build behaviour, not an environment, and all are activated by property | `pom.xml`, `dsh-coverage-report/pom.xml`, `dsh-rest-api/pom.xml` |
| Environment values are fixed at build time: `mongo.properties` holds `mongo.host=${mongo.host}` and three more, filled by resource filtering from `settings.xml` | `dsh-data/src/main/resources/mongo.properties`; `docs/devops/README.md`, "How build properties reach a release build" |
| Six workflows carry dummy `mongo.*` values so that the build does not fail | `ci.yml`, `api-testing.yml`, `deploy.yml`, `staging.yml`, `release.yml`, `hotfix.yml` |
| Every module filters `src/main/resources` with both default delimiters, `${…}` and `@…@` | `dsh-rest-api/pom.xml:113-120`; no `delimiter` in either parent POM |
| One Spring placeholder already sits in a filtered file: `${spring.rabbitmq.host:localhost}`, and the port beside it. `dsh-data` takes its own XML context out of filtering | `dsh-rest-api/src/main/resources/enqueue-docId-context.xml:45`; `dsh-data/pom.xml:46-64`. Why the two survive filtering was not probed; presumably no Maven property has a name that includes the `:default` part |
| Spring Boot is `2.7.18`, managed in parent-poms | `products-3.10.0.pom:80` |
| MongoDB and RabbitMQ are wired in XML through `@ImportResource`, unconditionally | `dshApplicationContext.xml`, `enqueue-docId-context.xml` |
| `dsh-rest-api`'s test `application.properties` repeats the main one | the two files; P24 explains why it has to |

## 3. Findings: Spring

Measured on 2026-10-07 with Spring Boot 2.7.18, Apache Maven 3.9.16 and JDK 17.0.20.1 (Eclipse
Adoptium), on Windows 11. The harness is in the appendix. In the outputs, `PROBE` lines are printed
by the harness, and only the lines that matter to the finding are shown.

**Not checked against Spring Boot 3.** `#120` moves the Boot line. The harness takes the version
from one property, so re-running it there is cheap, and section 9 lists it as a candidate.

### S1. How is a profile activated, and which source wins?

**Answer.** An argument beats a system property, which beats an environment variable. An argument
also beats a value in the jar. The environment variable against the jar's value was not run for a
profile; P13 shows that order for an ordinary property. A later source replaces the list; it does
not add to it. A default written in the jar works, is replaced by any activation, and also applies
to every test that names no profile.

**Probe.** P1 to P4, P3b, P4b, P14, P15.

**Observed.**

```text
P2   SPRING_PROFILES_ACTIVE=gcp run                                   PROBE active=gcp
P3   java -Dspring.profiles.active=gcp -jar app-1.jar                 PROBE active=gcp
P3b  SPRING_PROFILES_ACTIVE=gcp java -Dspring.profiles.active=other   PROBE active=other
P4   SPRING_PROFILES_ACTIVE=gcp run --spring.profiles.active=other    PROBE active=other
P4b  java -Dspring.profiles.active=gcp -jar ... --spring.profiles.active=other
                                                                      PROBE active=other
```

With `spring.profiles.active=gcp` in the jar's `application.properties` (P14), and then with
`spring.profiles.default=gcp` instead (P15):

```text
P14a run                                  PROBE active=gcp     PROBE stores=[gcpStore]
P14b run --spring.profiles.active=other   PROBE active=other   PROBE stores=[inMemoryStore]
P15a run                                  PROBE active=        PROBE default=gcp   PROBE stores=[gcpStore]
P15b run --spring.profiles.active=other   PROBE active=other   PROBE default=gcp   PROBE stores=[inMemoryStore]
```

The context test of the same two builds, which names no profile, also ran under `gcp`:

```text
logs/p14-build.log:PROBE stores=[gcpStore]
logs/p15-build.log:PROBE stores=[gcpStore]
```

**Prediction.** Confirmed.

**Consequence for DSH.**

- The deployment can select the capability with one environment variable, and an operator can
  still override it with an argument.
- Wave 7's "make `gcp` the default profile" has two forms in the jar, and both change what every
  unpinned test runs with. The convention proposes a third: the jar's default stays the mode that
  needs no GCP, and the deploy sets `gcp`.
- With `@Profile("!gcp")`, leaving `gcp` means naming some other profile (P14b). Any name does it,
  which P31 shows is also a hazard.

### S2. How do profile-specific files combine?

**Answer.** A profile's file overrides `application.properties`. With two profiles, the one listed
last wins. An `on-profile` document inside `application.properties` applies only under that
profile. A profile's file cannot activate another profile: the startup stops.

**Probe.** P0, P1, P5, P16.

**Observed.**

```text
P0   run                                         PROBE probe.layer=base   PROBE probe.doc=<unset>
P1   run --spring.profiles.active=gcp            PROBE probe.layer=gcp    PROBE probe.doc=gcp-doc
P5a  run --spring.profiles.active=gcp,other      PROBE probe.layer=other
P5b  run --spring.profiles.active=other,gcp      PROBE probe.layer=gcp
```

P16, with `spring.profiles.active=other` added to `application-gcp.properties`:

```text
org.springframework.boot.context.config.InvalidConfigDataPropertyException: Property
'spring.profiles.active' imported from location 'class path resource [application-gcp.properties]'
is invalid in a profile specific resource
```

The build of P16 passed. Its context test names no profile, so it never read the broken file.

**Prediction.** Confirmed.

**Consequence for DSH.**

- Two profiles that set the same key depend on the order they are listed in. The convention keeps
  profile files from overlapping.
- A profile's file is only checked by a test that activates that profile. Each profile needs one
  context test of its own.

### S3. Can one profile switch on another?

**Answer.** Yes, with a profile group. The group's members come after the profile that names them,
so a member's file wins where both set a key.

**Probe.** P6. The base file holds `spring.profiles.group.dev=gcp`.

**Observed.**

```text
P6   run --spring.profiles.active=dev
PROBE active=dev,gcp
PROBE probe.layer=gcp
PROBE probe.name=dsh-dev
PROBE probe.database-id=dsh-dev
PROBE stores=[gcpStore]
```

**Prediction.** Confirmed.

**Consequence for DSH.** A tier profile that pulls in `gcp` works mechanically. But
`application-gcp.properties` overrode `application-dev.properties` (`probe.layer=gcp`), so a tier
file cannot adjust a capability default. Section 5 weighs this.

### S4. How is one of two implementations chosen?

**Answer.** `@Profile("gcp")` and `@Profile("!gcp")` leave exactly one bean. `@ConditionalOnProperty`
chooses service by service, and a profile's file can set that property, so the profile gives the
default and one value overrides it. A value that matches nothing leaves no bean, and the
application still starts unless something injects one.

**Probe.** P0, P1, P7, P8.

**Observed.**

```text
P0   run                                                   PROBE stores=[inMemoryStore]   PROBE transports=[memoryTransport]
P1   run --spring.profiles.active=gcp                      PROBE stores=[gcpStore]        PROBE transports=[pubSubTransport]
P7   run --spring.profiles.active=gcp --probe.transport=memory
                                                           PROBE stores=[gcpStore]        PROBE transports=[memoryTransport]
P8a  run --probe.transport=bogus                           PROBE stores=[inMemoryStore]   PROBE transports=[]
```

P8b, the same value with a bean that injects a `Transport`:

```text
APPLICATION FAILED TO START
Description:
Parameter 1 of constructor in probe.app.StrictConfig required a bean of type 'probe.lib.Transport' that could not be found.
```

**Prediction.** Confirmed.

**Consequence for DSH.**

- ADR-003's "transport selected by profile" and ADR-004's "each service on GCP or on an emulator,
  by configuration" are the same mechanism at two grains. The convention uses both: a property per
  service, and a profile that sets the properties together.
- A mistyped value is silent until something needs the bean (P8a). Each selectable interface needs
  a consumer that is always present, so that the mistake stops the startup.

### S5. Do profiles and run-time values reach the XML contexts?

**Answer.** Yes. A nested `<beans profile="gcp">` in an `@ImportResource` file is honoured. A
`${…}` in the XML takes an argument or an environment variable before the file named by
`<context:property-placeholder>`. That file's own values never reach the `Environment`.

**Probe.** P0, P1, P9.

**Observed.**

```text
P0   run                               PROBE probe.lib.host=<unset>     PROBE bean xmlAlways=from-lib-properties   PROBE bean xmlGcpOnly=<absent>
P1   run --spring.profiles.active=gcp                                                                              PROBE bean xmlGcpOnly=gcp
P9a  run --probe.lib.host=from-arg     PROBE probe.lib.host=from-arg    PROBE bean xmlAlways=from-arg
P9b  PROBE_LIB_HOST=from-env run       PROBE probe.lib.host=from-env    PROBE bean xmlAlways=from-env
```

**Prediction.** Confirmed.

**Consequence for DSH.**

- The MongoDB and RabbitMQ contexts can be put under a profile where they stand, without being
  rewritten in Java first.
- `mongo.host` does not need a build-time value. The harness's XML has the shape of
  `dshApplicationContext.xml`, and it took its value from an environment variable (P9b). `#46`
  already proved the argument form on DSH itself.
- A value that lives only in `mongo.properties` cannot drive a condition or a
  `@ConfigurationProperties` class, because it is not in the `Environment` (P0).

### S6. What happens to configuration files in a library module?

**Answer.** Two files of the same name are not merged: the application's hides the library's, for
`application.properties` and for a profile's file alike. A file named in `spring.config.import` is
loaded with its profile variant and overrides the file that imports it. A `@PropertySource` file
is loaded without a profile variant, and `application.properties` overrides it.

**Probe.** P0, P1, P13, P17, P30. Both modules ship `application.properties` and
`application-gcp.properties`; the library also ships `lib-imported*.properties`, imported by the
application's file, and `lib-sourced*.properties`, named by `@PropertySource`. Three files set
`probe.precedence`.

**Observed.**

```text
P0   run                               PROBE probe.origin=app          PROBE probe.only-in-lib=<unset>
                                       PROBE probe.imported=yes        PROBE probe.sourced=yes
                                       PROBE probe.precedence=imported
P1   run --spring.profiles.active=gcp  PROBE probe.lib-gcp=<unset>     PROBE probe.imported-gcp=yes   PROBE probe.sourced-gcp=<unset>
P13  PROBE_PRECEDENCE=env run          PROBE probe.precedence=env
```

P17, with the application's `application-gcp.properties` deleted, under `gcp`:

```text
PROBE probe.lib-gcp=yes
PROBE probe.layer=base
PROBE probe.transport=<unset>
```

P30, with the `spring.config.import` line removed:

```text
PROBE probe.imported=<unset>
PROBE probe.sourced=yes
PROBE probe.precedence=app
```

**Prediction.** Confirmed.

**Consequence for DSH.**

- A library module ships no `application*.properties`. The moment the deployable has a file of the
  same name, the library's is ignored whole, with no warning (P0). If the deployable has none, the
  library's silently becomes the application's (P17).
- A library's defaults go in a file named after the module and loaded by `@PropertySource`. The
  application's file, the environment and the arguments all override it (P30, P13), which is what
  a default should allow.
- `spring.config.import` is the wrong tool for a library's defaults. The imported file overrode
  the application's own value (P0), and the application has to declare the import.
- A `@PropertySource` file has no profile variant (P1). Values that differ by profile live in the
  deployable's `application-<profile>.properties`.

### S7. How do deploy-time values reach the application?

**Answer.** An environment variable sets the property, and every placeholder derived from it
follows at run time. An argument beats the variable. A required value with no default stops the
startup and names the key; a validated `@ConfigurationProperties` class reports it far more
clearly than `@Value` does. A key with a dash is reached by a variable with an underscore in its
place, and by one without.

**Probe.** P10 to P13, P28. The base file holds `probe.name=dsh`,
`probe.database-id=${probe.name}` and `probe.prefix=${probe.name}/`.

**Observed.**

```text
P10a PROBE_NAME=dsh-dev run                       PROBE probe.name=dsh-dev   PROBE probe.database-id=dsh-dev   PROBE probe.prefix=dsh-dev/
P10b PROBE_NAME=dsh-dev run --probe.name=dsh-arg  PROBE probe.name=dsh-arg   PROBE probe.database-id=dsh-arg   PROBE probe.prefix=dsh-arg/
P11b PROBE_BUCKET=b run --probe.strict=true       PROBE strict bucket=b transport=MemoryTransport
P12a PROBE_DATABASE_ID=x run                      PROBE probe.database-id=x
P12b PROBE_DATABASEID=y run                       PROBE probe.database-id=y
```

P11a, `run --probe.strict=true` with no bucket. The key is named, inside a stack trace, with no
"APPLICATION FAILED TO START" report:

```text
Caused by: java.lang.IllegalArgumentException: Could not resolve placeholder 'probe.bucket' in value "${probe.bucket}"
```

P28a, the same omission against a `@ConfigurationProperties("probe.required")` class with
`@Validated` and `@NotBlank`:

```text
APPLICATION FAILED TO START
Description:
Binding to target org.springframework.boot.context.properties.bind.BindException: Failed to bind properties under 'probe.required' to probe.app.RequiredProps failed:
    Property: probe.required.bucket
    Value: "null"
Action:
Update your application's configuration
```

**Prediction.** Confirmed.

**Consequence for DSH.**

- One environment variable carries the tier, and the names derived from it are written once, in
  one file.
- Required values are bound through a validated `@ConfigurationProperties` class, so that a
  missing one is reported as in P28a and not as in P11a, or as today's circular-placeholder error.
- On Spring Boot 3 the validation annotations are in `jakarta.validation`, not `javax.validation`.
  That is stated from knowledge of the migration, not from a run.

### S8. What configuration does a test get?

**Answer.** A test `application.properties` hides the main one entirely, including its imports. A
profile's file under `src/test/resources`, with `@ActiveProfiles`, merges with the main file
instead. `@TestPropertySource` beats the main file and the profile's file. A test that names a
profile is immune to a profile
leaked from outside; a test that names none, or uses an empty `@ActiveProfiles`, is not.

**Probe.** P22 to P26, P29. The lines are the context test's, read from the build log.

**Observed.**

```text
P24  test application.properties holding probe.origin=test
     PROBE probe.origin=test   PROBE probe.only-in-app=<unset>   PROBE probe.layer=<unset>
     PROBE probe.name=<unset>  PROBE probe.imported=<unset>      PROBE probe.precedence=sourced
P25  application-test.properties + @ActiveProfiles("test")
     PROBE active=test         PROBE probe.origin=test           PROBE probe.only-in-app=yes
     PROBE probe.layer=base    PROBE probe.name=dsh              PROBE probe.imported=yes
P26  P25 + @TestPropertySource(properties = "probe.origin=inline")
     PROBE active=test         PROBE probe.origin=inline         PROBE probe.only-in-app=yes
P23a @ActiveProfiles("other"), mvn -Dspring.profiles.active=gcp
     PROBE active=other        PROBE stores=[inMemoryStore]
P23b @ActiveProfiles("other"), SPRING_PROFILES_ACTIVE=gcp mvn
     PROBE active=other        PROBE stores=[inMemoryStore]
P29  @ActiveProfiles with no value, SPRING_PROFILES_ACTIVE=gcp mvn
     PROBE active=gcp          PROBE stores=[gcpStore]
```

**Prediction.** Confirmed.

**Consequence for DSH.**

- `dsh-rest-api/src/test/resources/application.properties` has to repeat the main file because it
  replaces it. Renamed to a profile's file, it would hold only what differs.
- Every context test names its profile. That is the only thing that kept a leaked `gcp` out (P23
  against P22 and P29).

## 4. Findings: Maven

Same versions and date as section 3.

### M1. What does resource filtering do to a Spring placeholder?

**Answer.** With no Maven property of the same name, the placeholder survives into the jar. With
one, it is replaced at build time, and the derived value can no longer follow a run-time value. A
property that refers to itself reproduces DSH's `Circular placeholder reference`, in every context
including the build's own tests. Restricting filtering to `@…@` stops all of it, and
`@project.version@` still works.

**Probe.** P18, P19, P20.

**Observed.** P18: the base sources, built with `-Dprobe.name=baked`.

```text
application.properties inside the jar:
probe.name=dsh
probe.database-id=baked
probe.prefix=baked/

PROBE_NAME=dsh-dev run:
PROBE probe.name=dsh-dev
PROBE probe.database-id=baked
PROBE probe.prefix=baked/
```

P19: `lib.properties` changed to `probe.lib.host=${probe.lib.host}`, the shape of `mongo.properties`.

```text
build, no property:     maven exit=1
                        [ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
                        Invalid bean definition with name 'xmlAlways' defined in class path resource
                        [lib-context.xml]: Circular placeholder reference 'probe.lib.host' in property definitions
build -DskipTests:      lib.properties inside lib-1.jar: probe.lib.host=${probe.lib.host}
P19a run                Caused by: java.lang.IllegalArgumentException: Circular placeholder reference
                        'probe.lib.host' in property definitions
P19b run --probe.lib.host=from-arg                PROBE bean xmlAlways=from-arg
P19c build -DskipTests -Dprobe.lib.host=baked     lib.properties inside lib-1.jar: probe.lib.host=baked
     run                                          PROBE probe.lib.host=<unset>   PROBE bean xmlAlways=baked
P19d same jar, run --probe.lib.host=from-arg      PROBE bean xmlAlways=from-arg
```

P20: filtering restricted to `@…@`, `probe.version=@project.version@` added, built with
`-Dprobe.name=baked`.

```text
application.properties inside the jar:
probe.name=dsh
probe.database-id=${probe.name}
probe.prefix=${probe.name}/
probe.version=1

PROBE_NAME=dsh-dev run:
PROBE probe.name=dsh-dev
PROBE probe.database-id=dsh-dev
PROBE probe.prefix=dsh-dev/
```

**Prediction.** Confirmed.

**Consequence for DSH.**

- Any `${…}` written into a DSH `application.properties` today is a race between Maven and Spring,
  decided by whether some POM, some `settings.xml` or some `-D` happens to define that name. P18's
  jar was silently inconsistent: the name said `dsh-dev` and the names derived from it said `baked`.
- The convention's derived names (`${dsh.name}`) are exactly the kind of placeholder this hits. So
  the filtering rule comes before the convention, not after it.
- `mongo.properties` needs no filtering. Its four lines can go, with defaults or required values
  supplied at run time (S5, S7). The dummy values in six workflows go with them.
- A value baked into the file named by `<context:property-placeholder>` can still be overridden at
  run time (P19d). A value baked into a derived placeholder in `application.properties` cannot be,
  short of overriding each derived key (P18).

### M2. What may the build legitimately stamp?

**Answer.** The version and the commit. `spring-boot:build-info` wrote both, and the running
application read both.

**Probe.** P21: `build -Dprobe.commit=abc1234`, with `<additionalProperties>` naming `commit`.

**Observed.**

```text
META-INF/build-info.properties inside the jar:
build.artifact=app
build.commit=abc1234
build.group=probe
build.name=app
build.time=2026-10-07T16\:52\:48.127Z
build.version=1

run:
PROBE build.version=1
PROBE build.commit=abc1234
```

**Prediction.** Confirmed.

**Consequence for DSH.** The release gate compares a commit, and the production smoke check reads
a version and a commit. Both can come from this stamp. The probe read it inside the application;
exposing it through the actuator's `info` endpoint is Spring Boot's documented use for it and was
not run. Two builds of the same sources gave two different jars (P27d), so a checksum of the jar
proves nothing about what is in it. `build.time` is one cause. The probe did not separate it from
the timestamps of the archive's own entries.

### M3. How does a Maven run choose a profile without baking it in?

**Answer.** It should not have to, and it can do so by accident. A `-Dspring.profiles.active` on
the `mvn` command line, or `SPRING_PROFILES_ACTIVE` in the shell, reached the test JVM and
changed the beans of a test that named no profile.

**Probe.** P22, with P23 and P29 as the controls (S8).

**Observed.**

```text
P22a mvn clean package -Dspring.profiles.active=gcp    PROBE active=gcp   PROBE probe.layer=gcp   PROBE stores=[gcpStore]
P22b SPRING_PROFILES_ACTIVE=gcp mvn clean package      PROBE active=gcp   PROBE probe.layer=gcp   PROBE stores=[gcpStore]
```

**Prediction.** Confirmed.

**Consequence for DSH.** Once `gcp` exists, a developer with `SPRING_PROFILES_ACTIVE=gcp` exported
runs the whole context-test layer against GCP beans. S8's rule, a named profile on every context
test, is what prevents it.

Where Maven starts the application itself, it passes run-time values as arguments, as
`dsh-rest-api`'s `http-integration-tests` profile does for `--mongo.*` (`#46`). The plugin's
`profiles` parameter does the same for a profile. Neither was probed again here.

### M4. What does a Maven profile per tier produce?

**Answer.** A different jar for each tier, and a broken one when the tier is left out. One jar
with two environment values gave the same two behaviours.

**Probe.** P27. Two profiles in `app/pom.xml`, activated by `-Dtier=dev` and `-Dtier=staging`, each
set `probe.tier.name`; `tier.properties` holds `probe.baked=@probe.tier.name@`.

**Observed.**

```text
P27a build -Dtier=dev        tier.properties inside the jar: probe.baked=dsh-dev
P27b build -Dtier=staging    tier.properties inside the jar: probe.baked=dsh-staging
P27c build with no tier      tier.properties inside the jar: probe.baked=@probe.tier.name@   (maven exit=0)
P27d base jar, built twice   sha256 7e2535c7bf455e1f… then 93631dc1d148ee5a…
P27e one jar                 PROBE_NAME=dsh-dev run       PROBE probe.database-id=dsh-dev       PROBE probe.prefix=dsh-dev/
                             PROBE_NAME=dsh-staging run   PROBE probe.database-id=dsh-staging   PROBE probe.prefix=dsh-staging/
```

**Prediction.** Confirmed. P27c was not predicted: a build with no tier succeeds and ships the
unresolved token.

**Consequence for DSH.** With a tier baked in, the jar that passed on staging is not the jar that
is released, in more than its version. The gate "staging passed this commit" then says less than
it appears to.

## 5. The tier model

Three approaches, compared on the criteria the spec set.

- **A. Maven profile per tier.** The build sets the names.
- **B. Spring profile per tier, in the jar**, with a group that adds `gcp`.
- **C. Capability profiles only; the tier is a value** set where the application is deployed.

| Criterion | A | B | C |
|---|---|---|---|
| One artifact for every tier | **No.** The configuration inside differs (P27a, P27b) | Yes (P6) | Yes (P27e) |
| An end user deploys under a name of their own without rebuilding | **No.** A baked name ignores the run-time one (P18) | **No**, as B is defined. A new name needs a new file in the jar, or falls back on C (P10). Spring Boot can also read a profile's file from outside the jar; that was not probed | Yes (P10a) |
| Each resource name is defined once | Yes, in the POM | Partly. Each tier file repeats every key, and it cannot adjust a `gcp` default (P6) | Yes. The derivations are one file (P10a) |
| A missing value fails at startup and names the key | **No.** The build succeeds with the token unresolved (P27c), or every context fails with a circular-placeholder error (P19) | **No.** A profile that matches no file is accepted in silence (P31) | Partly. A value with no default does, with a validated class (P28a). The base name and the `gcp` profile are not covered; see below |
| A new tier needs no change to the jar | **No** | **No**, with the tier's file in the jar | Yes |
| Works the same for a release and a hotfix | Yes, but each build must be given the tier | Yes | Yes |

**Recommendation: C.** It is the only approach that keeps one artifact, lets an end user choose a
name, and can fail loudly when a value is missing.

**What C does not catch.** Two omissions start cleanly, and both matter on an operated tier:

- **A deployment that leaves out the base name.** It defaults to `dsh` (R5), as the harness's did
  (P0), and `dsh` is production's name. A `dsh-dev` deploy without `DSH_NAME` derives production's
  database, topics and prefix.
- **A deployment that leaves out `gcp`, or mistypes it.** It runs without GCP (R2), and Spring says
  nothing about a profile that matches nothing (P31).

The default is what lets an end user deploy with no name at all, so it is not removed here. The
deploy has to set both values every time, and something has to check them: the service name
against the base name the application reports, for one. That is left to ADR-004.

What approach A was valued for, one switch that names everything consistently, is kept. The switch
moves from the build to the deployment, and the derivations move from the POM to one properties
file.

## 6. Proposed convention

**Proposed, not in force. ADR-003 and ADR-004 decide.** Names in this section are placeholders for
the ADRs to settle; the rules are what the findings support.

### 6.1 Rules

| # | Rule | From |
|---|---|---|
| R1 | A profile names a capability, never a tier. There is no `dev`, `staging` or `production` profile, in Spring or in Maven | section 5 |
| R2 | With no profile, the application reaches nothing on GCP. Each service uses the implementation the plan already has outside GCP (§6.3). `gcp` selects the GCP implementations. The jar does not set a default profile; the deployment sets `SPRING_PROFILES_ACTIVE=gcp` | S1 |
| R3 | A profile's file sets defaults. A property per service selects the implementation, and may be overridden alone | S4 |
| R4 | An emulator is `gcp` with the endpoint values pointed at it. It is not a third set of beans. Only Firestore's emulator properties were read (G2). Cloud Storage has no emulator (G6), and named databases in the Firestore emulator are open (§8.3) | S7, G2, G6 |
| R5 | The tier is one value, the base name. It defaults to `dsh`. Every resource name derives from it in one file, at run time | S7 |
| R6 | A required value is bound through a validated `@ConfigurationProperties` class. It has no default, and its absence stops the startup | S7 |
| R7 | Only the deployable module has `application*.properties`. A library module's defaults are in a file named after the module, loaded by `@PropertySource`, and do not vary by profile | S6 |
| R8 | The build supplies no environment value. It stamps the version and the commit | M1, M2 |
| R9 | Spring configuration files are filtered with `@…@` only, or not at all | M1 |
| R10 | Every context test names its profile with `@ActiveProfiles`. Test values go in a profile's file, never in a test `application.properties` | S8, M3 |
| R11 | Each profile has a context test that activates it. Each selectable interface has a consumer that is always present | S2, S4 |
| R12 | Profile files do not set the same key | S2 |

### 6.2 The files

```text
dsh-rest-api (the deployable; whichever module ADR-003 makes it)
  src/main/resources/
    application.properties        the base name, the derivations, the no-GCP defaults
    application-gcp.properties    the GCP defaults: which implementation each service uses
  src/test/resources/
    application-test.properties   only what a test changes

dsh-data (a library)
  src/main/resources/
    dsh-data.properties           defaults, loaded by @PropertySource; no profile variant
```

### 6.3 The keys

| Key | Default | Set by | Notes |
|---|---|---|---|
| `dsh.name` | `dsh` | the deployment, as `DSH_NAME` | the tier: `dsh-dev`, `dsh-staging`, `dsh` |
| `dsh.firestore.database-id` | `${dsh.name}-db` | derived | a suffix is needed: an id has at least four characters (G2) |
| `dsh.pubsub.prefix` | `${dsh.name}-` | derived | topics and subscriptions are `<prefix><channel>` (G3) |
| `dsh.storage.bucket` | none, required when `dsh.file-store` is `gcs` | the deployment | bucket names are global, so DSH cannot choose it (G4) |
| `dsh.storage.prefix` | `${dsh.name}/` | derived | the tier's sub-folder in the one bucket |
| `dsh.persistence` | `mongo`; `gcp` sets `firestore` | profile, or alone | one property per service (R3). `mongo` is `MongoDocumentDao`, the only implementation today, which Wave 2 keeps |
| `dsh.transport` | `memory`; `gcp` sets `pubsub` | profile, or alone | the in-memory channels of Wave 2 |
| `dsh.file-store` | `local`; `gcp` sets `gcs` | profile, or alone | `local` is `LocalFileStorageService`, Wave 1 task 10. No official emulator exists (G6) |

Spring Cloud GCP's own keys take their values from these, for example
`spring.cloud.gcp.firestore.database-id=${dsh.firestore.database-id}` (G2).

The defaults name implementations the plan already has (`specs/product/PRD.md`, Waves 1 and 2).
No in-memory persistence and no in-memory file store is planned, and this convention adds none.
That leaves one question for ADR-003. Wave 7 removes what remains of the legacy implementations,
and a run with no profile then has no persistence. The candidates are an in-memory implementation,
which no wave plans, and the Firestore emulator, which R4 makes a `gcp` run.

### 6.4 What changes for what exists today

- **`mongo.*`.** The four filtered lines are removed and the values arrive at run time. Whether
  that is worth doing before the wave that removes MongoDB is a follow-up candidate (section 9).
- **The `test` profile in `api-testing.yml`.** It selects nothing. Under R10 `test` gains a
  meaning for context tests, and a forked server started for external tests should not carry it.
- **The XML contexts.** They can be placed under a profile as they are (S5). Today both load with
  no profile. Under R2 MongoDB stays the default persistence, so its context still loads then.
  RabbitMQ does not stay: the default transport is in memory, and ADR-001's task 1.3 already puts
  RabbitMQ under a `rabbitmq` profile. ADR-003 names how each is selected.
- **Wave 7, "make `gcp` the default profile".** Under R2 this becomes "the deployment sets `gcp`",
  and nothing in the jar changes.

## 7. Test layers and configuration

| Layer | Target | Profile | Where its values come from |
|---|---|---|---|
| 1. Unit | no Spring context | none | nothing to configure. A leaked profile cannot reach it |
| 2. Spring context | a context in the test JVM, `@MockBean` for every external service | named by `@ActiveProfiles` on every test (R10) | `application.properties`, then `application-test.properties` (P25) |
| 3. Over the wire | the application forked by Maven, real services in Docker | passed as an argument by the Maven profile | arguments, as `--mongo.*` are today (`#46`); emulators replace the containers with `#49` |
| 4. External client | the same forked server | the same | the base URL only |
| 5a. Acceptance, deployed | `dsh-dev` after a `DEVELOP` deploy; `dsh-staging` after a staging deploy | `gcp`, set by the deployment | the layer-3 or layer-4 tests with their base URL pointed at the deployment; they write data |
| 5b. Smoke, production | `dsh`, after a release or hotfix deploy | `gcp`, set by the deployment | health, and the released version and commit (M2). It reads and writes no document |

Rows 1 to 4 show each layer as the convention would have it, not as it is. Today no layer-2 test
has `@ActiveProfiles`, no profile is passed to the forked server, and layer 4 has no collection:
`api-testing.yml` starts the application with `java -jar`, not through Maven (`#137`).

Rows 5a and 5b start from a stopped service, so their first request is a cold start, and their
timeouts must allow for one (G8).

**The gate, for a release and for a hotfix alike.** Every test that reads or writes a document
runs against `dsh-dev` or `dsh-staging`, never against production. The branch being released is
deployed to `dsh-staging` and layer 5a passes there. When the release or hotfix is dispatched, the
gate checks that the staging revision that passed was built from the head of that branch, by the
commit in its build stamp. If it was not, staging runs first. The release then builds the tag and
deploys `dsh`, and layer 5b checks the version and the commit and nothing else. The production
image is a rebuild, one release commit ahead of what staging tested, differing only in POM
versions; 5b is what covers that rebuild.

Left open, for `#148` and ADR-004:

- How a hotfix branch reaches `dsh-staging` while a release candidate may occupy it.
- How layer 5 authenticates to a Cloud Run service.
- How the tests clean up what they write on `dsh-dev` and `dsh-staging`.
- Whether the commit is also recorded as a label on the revision. The gate above reads it from the
  build stamp (M2), which means calling the service. The label was not weighed here.

## 8. GCP naming in one project

**Not verified by a run.** No GCP project exists yet. Every fact below is taken from the page
named, read on 2026-10-07. The words in quotation marks are the page's; the rest is a paraphrase.
`cloud.google.com` documentation now redirects to
`docs.cloud.google.com`.

| Id | Fact | Source |
|---|---|---|
| G1 | "Service names must be 49 characters or less and must be unique per region and project." | <https://docs.cloud.google.com/run/docs/deploying> |
| G1 | A revision name supplied by the user "must" start with `SERVICE-`, contain "only lowercase letters, numbers and `-`", not end with a `-`, and not exceed 63 characters | <https://docs.cloud.google.com/run/docs/configuring/services/labels> |
| G1 | `--revision-suffix`: "Revision names always start with the service name automatically." | <https://docs.cloud.google.com/sdk/gcloud/reference/run/deploy> |
| G1 | A revision's name is "The unique name of this Revision." | <https://docs.cloud.google.com/run/docs/reference/rest/v2/projects.locations.services.revisions> |
| G2 | "You can create multiple Firestore databases per project", to "a maximum of 100" | <https://docs.cloud.google.com/firestore/docs/manage-databases> |
| G2 | A database id has lowercase letters, numbers and hyphens only; "The first character must be a letter"; "Minimum of 4 characters"; "Maximum of 63 characters" | the same page |
| G2 | "The free tier applies to only one Firestore database per project." | <https://docs.cloud.google.com/firestore/quotas> |
| G2 | `spring.cloud.gcp.firestore.database-id`: "You can specify which database will be used. If not specified, the database id will be '(default)'." The emulator is selected by `spring.cloud.gcp.firestore.emulator.enabled` and `host-port`. The page carries no version; whether the Spring Cloud GCP line that pairs with Spring Boot 2.7 has `database-id` was not checked | <https://googlecloudplatform.github.io/spring-cloud-gcp/reference/html/firestore.html> |
| G3 | A Pub/Sub id has 3 to 255 characters, must "Start with a letter", and must "Not begin with the string `goog`" | <https://docs.cloud.google.com/pubsub/docs/pubsub-basics> |
| G4 | "Every bucket name must be globally unique." 3 to 63 characters; lowercase letters, numbers, dashes, underscores and dots | <https://docs.cloud.google.com/storage/docs/buckets> |
| G4 | "Managed folders are a type of folder on which you can grant IAM roles", and the access "applies to any object within the bucket that uses the managed folder path as a prefix". They "can only be created in buckets that have uniform bucket-level access enabled." | <https://docs.cloud.google.com/storage/docs/managed-folders> |
| G4 | A lifecycle rule can be limited by `matchesPrefix` and `age`, and its `Delete` action removes the object | <https://docs.cloud.google.com/storage/docs/lifecycle> |
| G5 | A revision deployed with `--no-traffic --tag` can be tested "at a specific URL, without serving traffic", such as `https://green---myservice-abcdef.a.run.app` | <https://docs.cloud.google.com/run/docs/rollouts-rollbacks-traffic-migration> |
| G5 | The tag name is "your lowercase tag name". No other rule is given on that page | the same page |
| G6 | `gcloud beta emulators` offers bigtable, datastore, firestore, pubsub and spanner. Cloud Storage is not among them | <https://docs.cloud.google.com/sdk/gcloud/reference/beta/emulators> |
| G7 | A container tag is `[\w][\w.-]{0,127}`: dots, upper case and a leading digit are all allowed | <https://pkg.go.dev/github.com/distribution/reference> |
| G7 | Artifact Registry's own example of a tag is `v1.1` | <https://docs.cloud.google.com/artifact-registry/docs/docker/names> |
| G8 | "By default, container instances have service-level minimum instances turned off, with a setting of `0`." | <https://docs.cloud.google.com/run/docs/configuring/min-instances> |
| G8 | Request-based billing: "CPU is only allocated during request processing". Instance-based billing is what "allocates CPU even outside of request processing, letting you execute short-lived background tasks ... after returning responses." | <https://docs.cloud.google.com/run/docs/configuring/billing-settings> |
| G8 | `--cpu-boost`: "allocate extra CPU to containers on startup to reduce the perceived latency of a cold start request. Enabled by default when unspecified on new services." | <https://docs.cloud.google.com/sdk/gcloud/reference/run/deploy> |
| G8 | A Pub/Sub push subscription posts each message to the service's URL, and "Success codes, such as HTTP `200` or `204`, acknowledge complete processing" | <https://docs.cloud.google.com/run/docs/triggering/pubsub-push> |
| G8 | Artifact Registry cleanup policies "automate artifact retention and removal", by tag state, tag prefix and age, or by keeping the most recent versions | <https://docs.cloud.google.com/artifact-registry/docs/repositories/cleanup-policy> |
| G8 | "Revisions that are not receiving requests don't consume any resources and are not billed." The page adds that a revision with minimum instances configured has billing considerations. Read on 2026-10-10 | <https://docs.cloud.google.com/run/docs/managing/revisions> |
| G8 | "There is a maximum of 1000 revisions per service: If you exceed that limit, older revisions are automatically deleted." A revision cannot be deleted while it is able to receive traffic, or is the latest or the only one of its service. Deleting one does not delete its container image. Read on 2026-10-10 | the same page |

### 8.1 The names, for one project

| Resource | Rule | `dsh-dev` | `dsh-staging` | `dsh` |
|---|---|---|---|---|
| Cloud Run service | the base name (G1) | `dsh-dev` | `dsh-staging` | `dsh` |
| Cloud Run revision | service, then the version with dashes; `-dev-<n>` for a snapshot, `-rc-<n>` for a release candidate (G1) | `dsh-dev-0-4-0-dev-54` | `dsh-staging-0-4-0-rc-54` | `dsh-0-4-0` |
| Firestore database | the base name and `-db` (G2) | `dsh-dev-db` | `dsh-staging-db` | `dsh-db` |
| Pub/Sub topic or subscription | the base name, a dash, the channel (G3) | `dsh-dev-<channel>` | `dsh-staging-<channel>` | `dsh-<channel>` |
| Bucket | one, named by the deployment (G4) | the same bucket | the same bucket | the same bucket |
| Object prefix | the base name and `/` (G4) | `dsh-dev/` | `dsh-staging/` | `dsh/` |
| Image | `dsh`, tagged with the version (G7) | `dsh:0.4.0-dev.54` | `dsh:0.4.0-rc.54` | `dsh:0.4.0` |

### 8.2 What the documentation settles about the owner's naming rule

- **`rc`, not `RC`.** A revision name holds lowercase letters, numbers and dashes only (G1).
- **No `dsh` prefix is needed on the suffix.** A revision name must start with the service name,
  and `--revision-suffix` puts it there. A suffix that starts with a digit is therefore fine, and
  `dsh-0-4-0` satisfies every stated rule (G1).
- **The image tag can keep its dots.** The tag grammar allows dots and a leading digit (G7). So the
  public image can be `dsh:0.4.0`, which is what a user pulling it expects, while the revision
  uses dashes because it must. The table above shows that option, as a proposal. The owner's rule
  as given, one dashed string for both, also satisfies the grammar; it gives up the conventional
  tag for having a single string. ADR-004 decides.
- **A production database cannot be called `dsh`.** Three characters is one short (G2). The table
  adds `-db` to every tier, so that one rule covers all three.

### 8.3 What the documentation leaves open

- **Reusing a revision name.** The API calls the name unique (G1), but no page read says what a
  deploy does when the suffix already exists. A re-run of a workflow keeps its run number, and a
  second deploy of an unchanged release keeps its version, so both cases will meet this. Until it
  is tried, assume the deploy fails.
- **A traffic tag's first character.** The Cloud Run page says only "lowercase" (G5). A search
  result attributed the words "1-63 characters long, and comply with RFC1035" to a Compute Engine
  reference for the same tag, but that page could not be read to confirm them, so they are not a
  G fact. An RFC 1035 label starts with a letter. So a smoke-check tag should start with one, such
  as `v0-4-0`, until a deploy settles it.
- **Named databases in the Firestore emulator.** `gcloud emulators firestore start` documents no
  option for them and does not say whether a database id is honoured. Local mode depends on this.
- **GHCR's tag rules.** Only the common grammar was read (G7), not GitHub's own page.
- **What an unused tier costs in Firestore, Pub/Sub and Artifact Registry.** Not established here.
  For Firestore only the free-tier sentence was read (G2), not what an idle database costs beyond
  what it stores.

### 8.4 Constraints these facts put on the plan

- **Three tiers in one bucket are isolated only by a managed folder.** A role granted on the bucket
  is not limited to a prefix. That is an inference: the managed folder is what the documentation
  offers for granting by prefix (G4), and no page read states the first half in those words.
  Granting each tier's service account its role on its own managed folder needs uniform
  bucket-level access on the bucket (G4). Without it, the `dsh-dev` service can overwrite
  production's objects.
- **Only one of the three Firestore databases is free.** The free tier covers one database per
  project (G2), so the two others are billed from their first operation. What an idle database
  costs is open (§8.3). Which tier holds the free one is a choice.
- **The `gcp` transport must be driven by requests.** With zero minimum instances and request-based
  billing, work that continues after the response has no CPU (G8), and instance-based billing is
  the documented way to get it, at the cost the rule forbids. A push subscription delivers each
  message as a request and takes the response code as the acknowledgement (G8), which fits. A
  subscriber that polls does not. In-memory channels stay right for the mode with no profile.
- **Old `dev` and `rc` artifacts can be removed automatically.** Cleanup policies by tag prefix
  cover the images, and a lifecycle rule by prefix and age covers the test objects (G4, G8).
- **Old revisions need no retention for the cost rule.** A revision that receives no requests is
  not billed, and Cloud Run deletes the oldest once a service has more than 1,000 (G8). Whether to
  delete them sooner is a choice for ADR-004. Deleting a revision does not delete its image (G8),
  which the cleanup policy above covers.

## 9. Consequences for the waves, and follow-up candidates

**This story does not edit the PRD.** The PRD changes go through `dsh-plan-wave`. The issues named
in §9.2 were opened or changed on 2026-10-10, after the owner approved them.

### 9.1 Consequences

| For | Consequence | From |
|---|---|---|
| ADR-003 | Selection is a property per service, with `gcp` as the profile that sets them together | S4, R3 |
| ADR-003 | On GCP each stage is driven by a request: Pub/Sub by push. Wave 5's "Pub/Sub channel adapters" is restated once the ADR decides | G8, §8.4 |
| ADR-003 | The XML contexts can be put under a profile where they stand | S5 |
| ADR-003 | The defaults with no profile are the implementations the plan has: MongoDB, the local file store, in-memory channels. What a run with no profile persists to once Wave 7 removes MongoDB is open: an in-memory implementation, which no wave plans, or the Firestore emulator | §6.3 |
| ADR-004 | The operated tiers are three runs of one deploy, in one project, each given a base name | section 5 |
| ADR-004 | The naming table of §8.1, and the five open points of §8.3. A first deploy settles two of them: reusing a revision name, and a traffic tag's first character | section 8 |
| ADR-004 | The deploy sets the base name and `gcp` every time, and something checks both. A deployment that leaves either out starts cleanly: the first as production, the second without GCP | section 5 |
| ADR-004 | The bucket needs uniform bucket-level access and a managed folder per tier | §8.4 |
| ADR-004 | The cost rule, service by service, with the retention that removes old artifacts; and which tier holds the free Firestore database. Anything with a fixed hourly cost, such as a load balancer in front of the service, has to be justified against the rule | §8.4 |
| ADR-004 | One image, tagged with the dotted version; revisions named with dashes | §8.2 |
| Wave 4 | Its first `gcp` code needs somewhere real to run. The image and a minimal deploy move forward from Wave 8, which keeps the end-user packaging | decision 2 of the spec |
| Wave 7 | Task 3, "make `gcp` the default profile", becomes "the deployment sets `gcp`". Task 1 becomes layer 5a | R2, section 7 |
| `testing-patterns.md` | Gains layer 5, and R10's rule for context tests, when ADR-004 lands | section 7 |
| `#148` | The release skill gains the commit gate and the production smoke check, for release and hotfix alike | section 7 |
| parent-poms | None of `project-staging.yml`, `project-release.yml` and `project-hotfix.yml` declares an output, at `ae7aaf83`. A DSH deploy job that runs after one needs the build number and the version. `parent-poms#91` covers the release tag and version, and `parent-poms#109` the staging build number and version | a grep for `outputs:` in the three files |

### 9.2 Follow-up candidates

Each was a draft for the owner to accept, change or drop. The owner accepted all six on
2026-10-10, and each names the issue that now carries it.

1. **Stop filtering Spring configuration with `${…}`.** Restrict `application*.properties`, the XML
   contexts and the files they read to `@…@`, or leave them unfiltered. It has to be scoped to those
   files: other filtered resources in DSH use `${…}` on purpose, the site Markdown among them. It
   comes before any `${dsh.name}` is written (M1). Now `#158`.
2. **Move `mongo.*` to run-time configuration.** Remove the four self-referring lines from
   `mongo.properties`, bind the values through a validated class, and delete the dummy values from
   six workflows and from the README's `settings.xml` instructions. The alternative is to leave it
   for the wave that removes MongoDB, and record that. Now `#157`, whose spec decides how far to
   go.
3. **Remove `--spring.profiles.active=test` from `api-testing.yml`**, or fold it into `#137`,
   which reworks that workflow. Added to `#137`, as its AC005.
4. **Rename `dsh-rest-api`'s test `application.properties` to `application-test.properties`**,
   keeping only what differs, and give its context tests `@ActiveProfiles("test")`. `#140` already
   edits both files and could carry it. Added to `#140`.
5. **parent-poms: expose the build number and the version as workflow outputs**, beside
   `parent-poms#91`. Now `parent-poms#109`, for the staging workflow.
6. **Re-run the harness on the Spring Boot line `#120` chooses.** One property selects the
   version. P28's validation import changes package on Boot 3. Added to `#120`, as its AC007.

## 10. Appendix: the probe harness

A throwaway two-module Maven project, kept outside the repository and never installed. `lib`
stands for `dsh-data`: a library with an XML context and a filtered properties file. `app` stands
for `dsh-rest-api`: the deployable. It imports `spring-boot-dependencies:2.7.18` and has no parent.
Both modules filter `src/main/resources` with the default delimiters, as DSH's do.

`app` prints one report at startup, and every probe is that report under different arguments,
environment or files. The full sources are in the story's spec,
`specs/stories/48-investigate-spring-and-maven-profiles.md` §5.2, and were used unchanged; they
are not repeated here. Each probe below started from those sources and a clean tree.

`run` starts `app/target/app-1.jar` with JDK 17 and keeps the `PROBE` lines. `build` is
`mvn -B clean package`.

| Probe | What was run | Finding |
|---|---|---|
| P0 | `build`, then `run`. The output matched the spec's predicted baseline line for line | S2, S4, S5, S6 |
| P1 | `run --spring.profiles.active=gcp` | S1, S2, S4, S5, S6 |
| P2 | `SPRING_PROFILES_ACTIVE=gcp run` | S1 |
| P3 | `java -Dspring.profiles.active=gcp -jar app-1.jar` | S1 |
| P3b | added: `SPRING_PROFILES_ACTIVE=gcp java -Dspring.profiles.active=other -jar app-1.jar` | S1 |
| P4 | `SPRING_PROFILES_ACTIVE=gcp run --spring.profiles.active=other` | S1 |
| P4b | added: `java -Dspring.profiles.active=gcp -jar app-1.jar --spring.profiles.active=other` | S1 |
| P5 | `run --spring.profiles.active=gcp,other`, then `other,gcp` | S2 |
| P6 | `run --spring.profiles.active=dev` | S3 |
| P7 | `run --spring.profiles.active=gcp --probe.transport=memory` | S4 |
| P8 | `run --probe.transport=bogus`; then the same with `--probe.strict=true --probe.bucket=b` | S4 |
| P9 | `run --probe.lib.host=from-arg`; `PROBE_LIB_HOST=from-env run` | S5 |
| P10 | `PROBE_NAME=dsh-dev run`; then the same with `--probe.name=dsh-arg` | S7 |
| P11 | `run --probe.strict=true`; `PROBE_BUCKET=b run --probe.strict=true` | S7 |
| P12 | `PROBE_DATABASE_ID=x run`; `PROBE_DATABASEID=y run` | S7 |
| P13 | `PROBE_PRECEDENCE=env run` | S6 |
| P14 | `spring.profiles.active=gcp` added to `app`'s `application.properties`; rebuild; `run`; `run --spring.profiles.active=other` | S1 |
| P15 | `spring.profiles.default=gcp` added in the same place; the same two runs | S1 |
| P16 | `spring.profiles.active=other` added to `app`'s `application-gcp.properties`; rebuild; `run --spring.profiles.active=gcp` | S2 |
| P17 | `app`'s `application-gcp.properties` deleted; rebuild; `run --spring.profiles.active=gcp` | S6 |
| P18 | `build -Dprobe.name=baked`; the file read from the jar; `PROBE_NAME=dsh-dev run` | M1 |
| P19 | `lib.properties` changed to `probe.lib.host=${probe.lib.host}`; build; runs with and without an argument; `build -Dprobe.lib.host=baked` | M1 |
| P20 | `maven-resources-plugin` restricted to the `@` delimiter; `probe.version=@project.version@` added; `build -Dprobe.name=baked`; `PROBE_NAME=dsh-dev run` | M1 |
| P21 | `build -Dprobe.commit=abc1234`; `build-info.properties` read from the jar; `run` | M2 |
| P22 | `build -Dspring.profiles.active=gcp`; `SPRING_PROFILES_ACTIVE=gcp build`; the test's lines read from each log | M3, S8 |
| P23 | `@ActiveProfiles("other")` on the test; the two builds of P22 | S8, M3 |
| P24 | a test `application.properties` holding `probe.origin=test`; rebuild | S8 |
| P25 | a test `application-test.properties` holding the same, and `@ActiveProfiles("test")`; rebuild | S8 |
| P26 | P25 and `@TestPropertySource(properties = "probe.origin=inline")`; rebuild | S8 |
| P27 | two profiles in `app/pom.xml` activated by `tier`, and a filtered `tier.properties`; `build -Dtier=dev`, `build -Dtier=staging`, `build`; then the base jar built twice and run with two values of `PROBE_NAME` | M4 |
| P28 | added: a `@ConfigurationProperties("probe.required")` class with `@Validated` and `@NotBlank`, and `spring-boot-starter-validation`; `build -DskipTests`; `run`; `PROBE_REQUIRED_BUCKET=b run` | S7 |
| P29 | added: `@ActiveProfiles` with no value on the test; `SPRING_PROFILES_ACTIVE=gcp build` | S8 |
| P30 | added: the `spring.config.import` line removed; rebuild; `run` | S6 |
| P31 | added: `run --spring.profiles.active=typo`, a profile nothing names | section 5 |

**Departures from the spec's plan**, each because the probe as written did not ask its whole
question:

- **P3b and P4b** were added. P3 and P4 showed that a system property and an argument work, but
  not that one beats the other.
- **P19** needed `-DskipTests` for its run-time steps. Its first build failed, as it should: the
  context test hit the circular placeholder. That failure is itself the finding, and it is shown.
- **P20** configured the plugin once in the root POM, which both modules inherit, where the spec
  said in both modules.
- **P28** was added to compare `@Value`'s failure with a validated class's.
- **P29** was added after P23, to see whether an empty `@ActiveProfiles` also pins a test. It does
  not.
- **P30** was added. No planned probe showed `application.properties` beating a `@PropertySource`
  file directly, which the prediction for S6 claims.
- **P31** was added for section 5: what a profile name that matches nothing does.
