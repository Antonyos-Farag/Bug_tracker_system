# 🔍 Code Walkthrough

A file-by-file explanation of every class in `src/main/java`. Read this next to the source;
each section lists the class's responsibility, its important members, and how it connects to
the rest of the system.

**Suggested reading order:** `User` → `Bug` → `FileHandler` → `UserService` / `BugService` →
`AuthService` → role classes (`Tester`, `Developer`, `PM`, `Admin`) → UIs (`BugTrackerApp`,
`Application`) → helpers (`EmailService`, `Screenshot`, `Main`, `ConsoleMain`).

---

## Model Layer

### `User.java` — the base account model

The superclass of every account in the system.

```java
protected String username;
protected String password;
protected String email;
```

- Fields are `protected` so role subclasses (e.g. `Tester.reportBugFromInput`, which stamps
  `username` as the reporter) can use them directly.
- **Two constructors:** `(username, password)` derives a default email
  `username + "@gmail.com"`; the three-arg version allows an explicit address. This is why
  email notifications work out of the box without an email column in `users.txt`.
- `getRole()` returns the generic string `"User"` and is **overridden by every subclass** —
  the UI shows it in the welcome label, and the PM's reports compare against it
  (`"Developer".equalsIgnoreCase(u.getRole())`).
- No setters for username/password: changing them means replacing the whole object
  (see `UserService.updateUser`).

### `Bug.java` — the bug record

A plain data class (JavaBean) with **12 fields**, matching the 12 CSV columns in `Bug.txt`:

| Field | Meaning | Typical values |
|---|---|---|
| `id` | unique numeric key | `1`, `2`, … |
| `bugName` | short title | *Login Crash* |
| `description` | free text | *App crashes on login* |
| `bugType` | category | `UI` / `Backend` / `Performance` |
| `priority` | urgency | `High` / `Medium` / `Low` |
| `bugLevel` | severity | `Critical` / `Major` / `Minor` |
| `projectName` | which product | *BugTrack*, *GTA* |
| `bugDate` | report date | `java.util.Date`, formatted `yyyy-MM-dd` |
| `status` | workflow state | `Open` / `In Progress` / `Closed` (legacy `Complete`) |
| `assignedTo` | developer username | may be empty until assigned |
| `reportedBy` | tester username | set at creation |
| `screenshotPath` | absolute image path | may be empty |

- Full constructor + getter/setter for every field (JavaFX's `PropertyValueFactory` and the
  `FileHandler` depend on these names).
- `printBug()` — console pretty-print of all fields, used by the Tester's open/closed listings.
- `toString()` — one-line summary `Bug #id | name | Priority… | Status… |` used by
  `Admin.viewAllBugs()`.

---

## Persistence Layer

### `FileHandler.java` — CSV read/write for both files

The **only** class that performs file I/O. Constants: `USERS_FILE = "users.txt"`,
`BUGS_FILE = "Bug.txt"`, dates via a shared `SimpleDateFormat("yyyy-MM-dd")`.

**Users side**

- `saveUsers(List<User>)` — writes one `username,password,role` line per user with a
  `BufferedWriter` (try-with-resources; `IOException` is caught and printed, never thrown).
- `loadUsers()` — returns an empty list if the file doesn't exist; otherwise reads line by
  line, skips blanks, `split(",", 3)` (limit 3 so passwords could theoretically contain
  commas), skips malformed lines, and rebuilds objects with `createUser`.
- `createUser(username, password, role)` — a **factory**: `switch` on the lower-cased role
  string → `new Tester / Developer / PM / Admin`. Accepts both `"pm"` and `"project manager"`.
  Unknown roles return `null` and the line is dropped.
- `getStoredRole(User)` — the reverse mapping via `instanceof`, so the file always stores the
  canonical short role names.

**Bugs side**

- `saveBugs(List<Bug>)` — writes the 12 fields joined by commas; a `null` date becomes an
  empty string.
- `loadBugs()` — `split(",", 12)`; skips lines with fewer than 12 parts; `Integer.parseInt`
  for the id (a non-numeric line aborts the whole load via the outer `catch (Exception)`,
  printing an error); date parse failures are individually ignored (`date = null`);
  screenshot path defaults to `""` when absent.
- `normalizeStatus(status)` — maps the legacy value `complete` → `Closed` on load, so old
  data files keep working with the current status vocabulary.

---

## Service Layer

### `UserService.java` — the user registry

Static `List<User> listOfUsers` filled in a **static initializer**:

```java
static {
    listOfUsers.addAll(FileHandler.loadUsers());
    if (listOfUsers.isEmpty()) { /* seed tester, developer, pm, admin with password "123" */ }
}
```

Public API (all `static`):

- `addUser(username, password, role)` — rejects duplicates (`findUserByUsername != null`),
  builds via its own private `createUser` factory (returns on invalid role with a printed
  hint), appends, then `FileHandler.saveUsers(listOfUsers)`.
- `updateUser(username, newPassword, newRole)` — **replaces the object**: creates a fresh
  user with the new password/role and `listOfUsers.set(index, updatedUser)`. (Side effect to
  know: a custom email is lost because the new object re-derives `username@gmail.com`.)
- `deleteUser(username)` — linear scan + `remove`, saves, returns `true/false` so callers can
  report success.
- `getAllUsers()` — exposes the live list (the GUI filters it for developers, the PM iterates
  it for reports).
- `findUserByUsername` / `findEmailByUsername` — **case-insensitive** lookups used by login,
  assignment validation and email routing.
- `saveUsersToFile()` — explicit flush used by the UIs after a batch of changes.

### `BugService.java` — the bug registry

Same pattern, smaller surface:

- Static list hydrated from `FileHandler.loadBugs()`; if empty, seeds the three demo bugs
  (*Login Crash*, *Stats Missing*, *Email Bug*) dated `new Date()`.
- `getAllBugs()` — the live list (GUI copies it into an `ObservableList`; roles iterate it).
- `addBug(bug)` — append **and immediately save**.
- `saveBugsToFile()` — the "commit" call used after in-place mutations
  (`setStatus`, `setAssignedTo`, `setScreenshotPath`).

### `AuthService.java` — login

Tiny and focused:

```java
public User login(String username, String password) {
    for (User user : UserService.getAllUsers())
        if (user.getUsername().equalsIgnoreCase(username)
                && user.getPassword().equals(password))
            return user;
    return null;
}
```

Username is case-insensitive, password is case-sensitive, `null` means "invalid credentials".
Because it returns the concrete subclass, the UIs can `instanceof`-dispatch on the result.

### `EmailService.java` — SMTP sender

A `final` utility class (private constructor, all-static):

- `sendEmail(to, subject, message)` builds a JavaMail `Session` for **SSL SMTP**
  (`mail.smtp.socketFactory.class = javax.net.ssl.SSLSocketFactory`, default
  `smtp.gmail.com:465`) and sends a `MimeMessage`. Failures are wrapped in a
  `RuntimeException` — callers catch it and degrade gracefully.
- Configuration is **environment-variable only**: `requireConfig` throws
  `IllegalStateException("Missing required environment variable: …")` for `SMTP_USER` /
  `SMTP_PASS`; `getConfig` supplies defaults for host/port/from. No secrets in the repo.

---

## Role Layer (the heart of RBAC)

All four classes `extend User`, override `getRole()`, and own a console `showMenu()` loop:
print options → read a line → `Integer.parseInt` in a `try/catch` (invalid input re-loops) →
dispatch → exit on the last option.

### `Tester.java`

- `reportBug(Bug)` — `BugService.addBug` + save + confirmation print.
- `assignBug(bug, developerName)` — **validates the target is a real `Developer`**
  (`UserService.findUserByUsername` + `instanceof`), sets `assignedTo`, saves.
- `viewOpenBugs()` / `viewClosedBugs()` — filter by status and `printBug()` each match
  (closed view accepts both `Closed` and legacy `Complete`).
- Private input helpers drive the console menu:
  - `reportBugFromInput` — asks for name, type, priority, level, project, optional date
    (parsed `yyyy-MM-dd`, falls back to today on `ParseException`), optional status
    (defaults to `Open`); computes the next id as `max(existing)+1`; stamps
    `reportedBy = username`.
  - `assignBugFromInput` — asks for bug id, finds it, calls `assignBug`, and **if the
    assignment stuck** sends the notification email.
  - `attachScreenshotFromInput` — delegates to `Screenshot`, then saves.
- `sendEmailToDeveloper(bug, name)` — resolves the address with
  `UserService.findEmailByUsername`, falling back to a derived
  `first.last@gmail.com`; subject *"New Bug Assigned to You"*; body summarizes id/name/
  priority/level/project/reporter; `RuntimeException` from `EmailService` is caught and
  printed.

### `Developer.java`

- `viewAssignedBugs()` — prints `ID / Name / Status` for every bug whose `assignedTo`
  matches this developer's `username` (case-insensitive), or "No bugs assigned to you."
- `updateBugStatus(Scanner)` — the workflow engine of the console app:
  1. read bug id (numeric validation),
  2. **ownership guard**: refuses with "Bug not assigned to you." unless
     `username.equalsIgnoreCase(bug.getAssignedTo())`,
  3. offer `1. In Progress` / `2. Closed`,
  4. set status + `BugService.saveBugsToFile()`,
  5. on *Closed*, call `sendEmailToTester(bug)`.
- `sendEmailToTester(bug)` — mirrors the Tester's mailer: resolves the reporter's email
  (with the same `username@gmail.com` fallback), subject *"Bug Closed Notification"*.

### `PM.java` (Project Manager)

Read-only analytics over the bug list:

- `monitorBugsByStatus()` — counts `open` vs `closed` (counting legacy `complete` as closed).
- `showDeveloperPerformance()` — builds two `HashMap<String,Integer>`s while iterating bugs:
  `assigned` (per `assignedTo`) and `resolved` (subset with status Closed/Complete); then for
  every user whose role is Developer prints `Assigned=… Resolved=… Completion=…%`
  (`done*100.0/total`, 0 when nothing assigned).
- `showTesterPerformance()` — same pattern keyed on `reportedBy`, printing
  `Reported=… Closed=… Closure=…%`.
- The identical algorithms are re-implemented GUI-side in
  `BugTrackerApp.viewDeveloperPerformance / viewTesterPerformance` (there they build a
  `StringBuilder` report shown in a dialog instead of printing).

### `Admin.java`

- `viewAllBugs()` — prints `bug.toString()` for every record (or "No bugs found.").
- `addUser / updateUser / deleteUser` — thin wrappers around `UserService` that add
  **user feedback**: e.g. `addUser` compares the list size before/after to print
  "User added successfully!"; `updateUser` first checks existence and afterwards verifies the
  password/role actually changed; `deleteUser` uses the boolean returned by the service.
  Each wrapper calls `UserService.saveUsersToFile()`.
- `showMenu()` — 5-option loop (View All Bugs / Add / Update / Delete / Exit); the add and
  update branches prompt for username, password and role inline.

---

## UI Layer

### `Main.java` — GUI entry point

```java
public static void main(String[] args) {
    javafx.application.Application.launch(BugTrackerApp.class, args);
}
```

Deliberately **not** a subclass of `javafx.application.Application` — launching from a plain
class avoids the JavaFX "missing modules" runtime error that hits `Application` subclasses on
a classpath-only JVM.

### `ConsoleMain.java` — console entry point *(added for this repository)*

One-liner: `new Application().start();` — `Application` never had a `main`, and this makes the
text UI runnable via `mvn exec:java`.

### `Application.java` — console login + dispatch

- Creates an `AuthService`, waits for *Press Enter to Start…*, then loops
  username/password until `login` returns non-null (printing
  "Username or Password are Incorrect. Please try again!!").
- Prints `Welcome <Role>!` and dispatches with an `instanceof` chain to the matching
  `showMenu()`. When the chosen role menu exits, the program ends.

### `BugTrackerApp.java` — the JavaFX GUI (the largest class)

Extends `javafx.application.Application`. Instance state: the primary `stage`, the logged-in
user `me`, an `ObservableList<Bug> bugs` mirrored from `BugService`, the `TableView<Bug>`,
a `FilteredList<Bug>` for search, plus the details `TextArea` and screenshot `ImageView`.

**`start(Stage)`** → title "Bug Tracker" → `showLogin()` → `stage.show()`.

**`showLogin()`** — a 400×260 dark-themed `VBox` (username `TextField`, `PasswordField`,
Sign-In button). On action: `auth.login(...)`; `null` → error `Alert`; success → store `me`,
`showDashboard()`. (The dashboard's Logout button simply calls `showLogin()` again.)

**`showDashboard()`** — builds the 1200×760 main scene:

- **Table:** six columns (ID, Name, Status, Priority, Assigned, Reporter) created by the
  generic helper `col(title, property, type)` using `PropertyValueFactory`; all columns
  constrained-resize together.
- **Data plumbing:** `bugs.setAll(BugService.getAllBugs())` → `FilteredList` (predicate set by
  the search box) → `SortedList` whose comparator is **bound to the table's** comparator, so
  header-sorting and filtering compose correctly.
- **Search box:** on every keystroke lower-cases the query and rebuilds the predicate; matches
  on id, name, status, assignedTo, reportedBy (null-safe `hit(...)` helper).
- **Selection listener:** `selectedItemProperty` → `showBug(b)` fills the details pane and the
  screenshot preview.
- **Action bar** (`btn(text, runnable)` helper): Refresh, Report, Assign, Status, Screenshot,
  Insights, Users, Logout.
- **Bottom:** `HBox` of the read-only details `TextArea` (grows) and the preview box
  (340×220 ratio-preserving `ImageView` + filename label).

**Actions & their permission guards** (each starts with an `instanceof` check and a warning
`alert` on refusal):

- `reportBug()` — Tester/Admin. A modal `Dialog<Bug>` with Name, Project, Description, Status
  combo (Open/In Progress/Closed, default Open) and `DatePicker` (default today). The result
  converter validates non-empty name/project, computes `id = max+1` with a stream, converts
  `LocalDate` → `Date` via the system zone, and hard-codes type/priority/level to
  `UI/Medium/Major`. On success: `BugService.addBug` + `refresh()`.
- `assignBug()` — Tester/Admin. Collects developer usernames from `UserService`, warns if
  none exist, shows a `ChoiceDialog`, then `setAssignedTo` + save + refresh.
- `updateStatus()` — Developer only, and only for a bug assigned to them
  (`equalsIgnoreCase`); `ChoiceDialog` of In Progress/Closed; save + refresh.
- `attachScreenshot()` — any role. JavaFX `FileChooser` filtered to
  png/jpg/jpeg/gif/bmp; stores the absolute path on the bug, saves, refreshes, re-renders the
  preview.
- `showInsights()` — PM/Admin. `ChoiceDialog` of *Bug Counts* / *Developer Performance* /
  *Tester Performance*, dispatching (Java 14 switch expression with `->`) to:
  - `viewBugCounts()` — tallies Open / In Progress / Closed (legacy Complete counted as
    Closed) and shows an information alert.
  - `viewDeveloperPerformance()` / `viewTesterPerformance()` — the PM's map-based algorithms
    rebuilt as `StringBuilder` reports shown in a scrollable `Dialog`.
- `manageUsers()` — Admin only. Choice of Add / Update / Delete → `TextInputDialog` for the
  username → (Add/Update) `TextInputDialog` for the password and `ChoiceDialog` for the role
  (`tester/developer/pm/admin`) → the matching `UserService` call → `saveUsersToFile()`.

**Display helpers:** `format(Bug)` renders the 12-field details text (date via the static
`SimpleDateFormat`, `null`-safe); `showBug(Bug)` additionally loads the screenshot with
`new Image(file.toURI().toString(), true)` (background loading) and handles missing/corrupt
files with friendly labels ("Screenshot file not found", "Unable to load image");
`alert(type, message)` is the shared dialog helper; `refresh()` re-syncs the observable list.

### `Screenshot.java` — console attachment helper

Used by the Tester console menu (the GUI has its own JavaFX `FileChooser` flow). Steps in
`attachScreenshot(Scanner)`:

1. `getBugFromUser` — read bug id with three explicit validations (non-empty, numeric,
   positive) and look it up (`findBugById`), printing ❌ messages on each failure.
2. `openFileChooser` — Swing `JFileChooser` with a `FileNameExtensionFilter` limited to
   png/jpg/jpeg/gif/bmp ("All Files" hidden); returns `null` on cancel.
3. `isValidImageFile` — existence check, **10 MB size cap**, extension whitelist re-check.
4. `attachScreenshotToBug` — store the absolute path on the bug and
   `BugService.saveBugsToFile()`; success prints a ✅ summary block.

---

## How a Typical Session Touches the Files

| Step | Classes involved | Disk effect |
|---|---|---|
| App starts | `Main` → `BugTrackerApp` → `AuthService`/`UserService`/`BugService` static init | reads `users.txt`, `Bug.txt` (seeds if missing) |
| Login | `AuthService.login` | none |
| Tester reports bug | `BugTrackerApp.reportBug` → `BugService.addBug` | rewrites `Bug.txt` |
| Tester assigns to developer | `BugTrackerApp.assignBug` (+ console: `Tester.sendEmailToDeveloper` → `EmailService`) | rewrites `Bug.txt` (console: sends SMTP mail) |
| Tester attaches screenshot | `FileChooser` / `Screenshot` → `bug.setScreenshotPath` → save | rewrites `Bug.txt` (image itself is *not* copied) |
| Developer closes bug | `updateStatus` / `Developer.updateBugStatus` (+ `sendEmailToTester`) | rewrites `Bug.txt` (console: sends SMTP mail) |
| PM opens insights | `showInsights` / `PM.*Performance` | none (read-only) |
| Admin manages users | `manageUsers` / `Admin.*` → `UserService` | rewrites `users.txt` |
