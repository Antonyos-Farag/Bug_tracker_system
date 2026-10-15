# 🏗️ Architecture

This document explains how the Bug Tracker System is put together: its layers, its classes,
how they interact, and why the design looks the way it does.

---

## 1. The Three Layers

Although the project has no framework, the classes fall naturally into three layers.
Dependencies only point **downward** — the model/persistence layer knows nothing about JavaFX,
and the services know nothing about which UI is on screen.

```
┌──────────────────────────────────────────────────────────────────────┐
│                              UI LAYER                                │
│                                                                      │
│  JavaFX path:  Main ──▶ BugTrackerApp (login scene → dashboard scene)│
│  Console path: ConsoleMain ──▶ Application (login loop → role menu)  │
│                              │                                       │
│                role menus:   ├── Tester.showMenu()                   │
│                              ├── Developer.showMenu()                │
│                              ├── PM.showMenu()                       │
│                              └── Admin.showMenu()                    │
│  helpers: Screenshot (Swing file chooser for console attachments)    │
└──────────────────────────────┬───────────────────────────────────────┘
                               │ uses
┌──────────────────────────────▼───────────────────────────────────────┐
│                           SERVICE LAYER                              │
│                                                                      │
│  AuthService    login(username, password) → User | null              │
│  BugService     static List<Bug>  — getAllBugs / addBug / save       │
│  UserService    static List<User> — add / update / delete / find     │
│  EmailService   sendEmail(to, subject, message) via SMTP             │
└──────────────────────────────┬───────────────────────────────────────┘
                               │ reads / writes through
┌──────────────────────────────▼───────────────────────────────────────┐
│                     MODEL + PERSISTENCE LAYER                        │
│                                                                      │
│  Bug (12 fields)      User ◀── Tester / Developer / PM / Admin       │
│  FileHandler  ⇄  users.txt , Bug.txt   (one CSV line per record)     │
└──────────────────────────────────────────────────────────────────────┘
```

### Why this shape?

- **Two UIs, one core.** `BugTrackerApp` (GUI) and `Application` + role `showMenu()` methods
  (console) call exactly the same services. Adding a third UI (e.g. a web front-end) would not
  touch the business logic.
- **Static services as an in-memory database.** `BugService` and `UserService` are effectively
  singletons (static state). Any class can reach the canonical lists without dependency
  injection — simple, at the cost of testability (see *Limitations* in the README).
- **Write-through persistence.** Every mutation (`addBug`, `assignBug`, `setStatus`,
  `addUser`, …) is immediately followed by `FileHandler.save…(list)`. There is no
  "dirty" state: the files always mirror memory.

---

## 2. Class Diagram

```mermaid
classDiagram
    direction LR

    class User {
        #String username
        #String password
        #String email
        +getRole() String
        +getUsername() String
        +getPassword() String
        +getEmail() String
        +setEmail(String)
    }
    class Tester {
        +getRole() "Tester"
        +reportBug(Bug)
        +assignBug(Bug, String)
        +viewOpenBugs()
        +viewClosedBugs()
        +showMenu()
    }
    class Developer {
        +getRole() "Developer"
        +showMenu()
        +viewAssignedBugs()
        +updateBugStatus(Scanner)
    }
    class PM {
        +getRole() "PM"
        +showMenu()
        +monitorBugsByStatus()
        +showDeveloperPerformance()
        +showTesterPerformance()
    }
    class Admin {
        +getRole() "Admin"
        +showMenu()
        +viewAllBugs()
        +addUser(u, p, role)
        +updateUser(u, p, role)
        +deleteUser(u)
    }

    User <|-- Tester
    User <|-- Developer
    User <|-- PM
    User <|-- Admin

    class Bug {
        -int id
        -String bugName
        -String description
        -String bugType
        -String priority
        -String bugLevel
        -String projectName
        -Date bugDate
        -String status
        -String assignedTo
        -String reportedBy
        -String screenshotPath
        +printBug()
        +toString() String
    }

    class AuthService   { +login(String, String) User }
    class UserService   { +getAllUsers()$ +addUser()$ +updateUser()$ +deleteUser()$ +findUserByUsername()$ +findEmailByUsername()$ }
    class BugService    { +getAllBugs()$ +addBug()$ +saveBugsToFile()$ }
    class EmailService  { +sendEmail(to, subject, msg)$ }
    class FileHandler   { +loadUsers()$ +saveUsers()$ +loadBugs()$ +saveBugs()$ }

    UserService o-- User  : static list
    BugService  o-- Bug   : static list
    AuthService ..> UserService : validates against
    UserService ..> FileHandler : load / save
    BugService  ..> FileHandler : load / save
    FileHandler ..> User : creates by role
    FileHandler ..> Bug  : parses lines

    class BugTrackerApp { +start(Stage) }
    class Application   { +start() }
    class Main          { +main(String[])$ }
    class ConsoleMain   { +main(String[])$ }

    Main ..> BugTrackerApp : launches
    ConsoleMain ..> Application : launches
    BugTrackerApp ..> AuthService
    BugTrackerApp ..> BugService
    BugTrackerApp ..> UserService
    Application ..> AuthService
    Application ..> Tester  : dispatches by instanceof
    Application ..> Developer
    Application ..> PM
    Application ..> Admin
    Tester ..> BugService
    Tester ..> EmailService : notify developer
    Tester ..> Screenshot : attach
    Developer ..> BugService
    Developer ..> EmailService : notify tester
    PM ..> BugService
    PM ..> UserService
    Admin ..> BugService
    Admin ..> UserService
```

**Key inheritance idea:** roles *are* users. `UserService.createUser(username, password, role)`
is a small factory that maps the stored role string (`tester` / `developer` / `pm` / `admin`)
to the concrete subclass, so the rest of the program can treat everyone as `User` and narrow
with `instanceof` only where behaviour differs.

---

## 3. Startup & Data Flow

```mermaid
flowchart TD
    A[Main.main] -->|Application.launch| B[BugTrackerApp.start]
    B --> C[Login Scene]
    C -->|Sign In| D[AuthService.login]
    D -->|reads| E[UserService static list]
    E -->|hydrated at class-load from| F[(users.txt)]
    E -->|seeded with defaults if empty| E
    D -->|null| C
    D -->|User object| G[Dashboard Scene]
    G --> H[BugService static list]
    H -->|hydrated at class-load from| I[(Bug.txt)]
    G -->|every mutation| J[FileHandler.saveUsers / saveBugs]
    J --> F
    J --> I
```

Two details worth knowing:

1. **Lazy class initialization does the loading.** `UserService` and `BugService` each have a
   `static { … }` block that calls `FileHandler.load…()` the first time the class is touched.
   If the file is missing or empty, demo records are inserted (and written back on the first
   save) — the app always starts with something to show.
2. **The GUI holds a mirror list.** `BugTrackerApp` copies `BugService.getAllBugs()` into an
   `ObservableList<Bug>` wrapped in `FilteredList` → `SortedList` for the table. `refresh()`
   re-syncs the mirror after every change; the *same* `Bug` objects are shared, so setters
   called in dialogs are instantly visible.

---

## 4. Sequence: Assign a Bug (with email)

```mermaid
sequenceDiagram
    autonumber
    actor T as Tester (GUI)
    participant App as BugTrackerApp
    participant BS as BugService
    participant US as UserService
    participant FH as FileHandler
    participant ES as EmailService
    participant D as Developer's inbox

    T->>App: select bug → click "Assign"
    App->>App: instanceof check (Tester/Admin only)
    App->>US: getAllUsers() → keep Developers
    App-->>T: ChoiceDialog with developer usernames
    T->>App: pick "tony"
    App->>BS: bug.setAssignedTo("tony") + saveBugsToFile()
    BS->>FH: saveBugs(list)
    FH-->>BS: Bug.txt rewritten
    Note over App,ES: Console flow (Tester.assignBugFromInput)<br/>additionally sends the email inline:
    App->>US: findEmailByUsername("tony")
    App->>ES: sendEmail(email, "New Bug Assigned to You", details)
    ES->>D: SMTP over SSL (port 465)
```

> **GUI vs console difference:** in the *console* Tester menu, assigning a bug immediately
> triggers `sendEmailToDeveloper(...)`. The *JavaFX* dialog path updates and saves the bug but
> does not call `EmailService` — one of the small behavioural divergences between the two UIs.

---

## 5. Sequence: Close a Bug (console Developer flow)

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer (console)
    participant Menu as Developer.showMenu
    participant BS as BugService
    participant FH as FileHandler
    participant US as UserService
    participant ES as EmailService
    actor Tester as Tester's inbox

    Dev->>Menu: option 2 → Bug ID
    Menu->>BS: find bug by ID
    Menu->>Menu: guard — assignedTo must equal username
    Menu-->>Dev: "1. In Progress  2. Closed"
    Dev->>Menu: choose 2 (Closed)
    Menu->>BS: bug.setStatus("Closed") + saveBugsToFile()
    BS->>FH: saveBugs(list) → Bug.txt
    Menu->>US: findEmailByUsername(bug.reportedBy)
    Menu->>ES: sendEmail(testerEmail, "Bug Closed Notification", …)
    ES->>Tester: "Bug #4 has been marked as CLOSED by developer tony"
```

The same guards exist in the GUI (`BugTrackerApp.updateStatus`): only a `Developer`, only for a
bug whose `assignedTo` matches their username (case-insensitive), with statuses restricted to
*In Progress* / *Closed*.

---

## 6. Persistence Format (summary)

`FileHandler` is the only class that touches the disk:

| File | Written by | Line format |
|---|---|---|
| `users.txt` | `saveUsers` | `username,password,role` (role via `getStoredRole`, an `instanceof` reverse-mapping) |
| `Bug.txt` | `saveBugs` | `id,name,description,type,priority,level,project,date(yyyy-MM-dd),status,assignedTo,reportedBy,screenshotPath` |

On load, lines are split with an explicit limit (`split(",", 3)` / `split(",", 12)`), short or
blank lines are skipped, dates that fail to parse become `null`, unknown roles are ignored, and
the legacy status `complete` is normalized to `Closed`. Full details:
[DATA_FILES.md](DATA_FILES.md).

---

## 7. Design Decisions & Trade-offs

| Decision | Benefit | Trade-off |
|---|---|---|
| Default package, flat class layout | Matches the course project; zero import noise | Not scalable; no namespace separation |
| Roles via inheritance (`User` subclasses) | Natural OOP; `instanceof` dispatch is easy to read | Adding a role touches `UserService`, `FileHandler`, `Application`, GUI guards |
| Static service lists | Global access, no wiring | Hard to unit-test; one JVM = one "database" |
| Flat files instead of SQL | Zero setup; files are human-readable and diffable | No transactions, no escaping of commas, no concurrency control |
| Write-through saves after every change | Files never stale | Full-file rewrite per mutation (fine at this scale) |
| Env vars for SMTP config | No secrets in the repo | Email silently unavailable until configured (graceful `try/catch` message) |
| Two UIs over shared services | Demonstrates UI/business-logic separation | Minor feature drift between GUI and console (e.g. email on assign) |
