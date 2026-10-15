# 🐞 Bug Tracker System

A **role-based desktop Bug Tracking System** built with **Java** and **JavaFX**.

The application lets a software team manage the full life-cycle of a bug — from the moment a
**Tester** reports it, through **assignment** to a **Developer**, to **closure** and
**performance reporting** for the **Project Manager** — with everything supervised and the user
accounts managed by an **Admin**. All data is persisted locally in simple flat files
(`users.txt` and `Bug.txt`), bugs can carry **screenshot attachments**, and the system sends
**email notifications** (SMTP) when bugs are assigned or closed.

---

## 📖 Table of Contents

1. [The Idea](#-the-idea)
2. [Features](#-features)
3. [The Four Roles](#-the-four-roles)
4. [Architecture](#-architecture)
5. [Project Structure](#-project-structure)
6. [Requirements](#-requirements)
7. [Getting Started (GUI)](#-getting-started-gui)
8. [Console Mode](#-console-mode)
9. [Default Accounts](#-default-accounts)
10. [Data Files](#-data-files)
11. [Email Notifications Setup](#-email-notifications-setup)
12. [Documentation Index](#-documentation-index)
13. [Known Limitations & Future Work](#-known-limitations--future-work)

---

## 💡 The Idea

Every software team needs a single source of truth for defects. Commercial tools (Jira, Bugzilla…)
are powerful but heavy. This project implements the **core concepts of a bug tracker from
scratch** as a learning-oriented desktop application:

- **A bug is a first-class record** with an ID, name, description, type (UI / Backend /
  Performance), priority (High / Medium / Low), severity level (Critical / Major / Minor),
  project name, date, status (Open / In Progress / Closed), the developer it is *assigned to*,
  the tester who *reported it*, and an optional *screenshot path*.
- **People are more than logins.** Each user has a **role**, and the role decides *what the user
  can do* and *which menu/dashboard they see*. This is the heart of the system:
  **role-based access control (RBAC)** implemented with plain Java inheritance and
  `instanceof` permission checks.
- **The workflow is a pipeline:**
  `Tester reports bug → Tester assigns it to a Developer (email sent) → Developer sets it
  In Progress → Developer closes it (email sent back to the reporter) → PM measures
  completion rates → Admin manages the accounts that make all of this possible.`
- **No database server required.** Persistence is two human-readable CSV-style text files
  handled by a single `FileHandler` class, which makes the system trivially portable and easy
  to inspect — ideal for a course project.
- **Two user interfaces over one core.** A modern **JavaFX GUI** (`BugTrackerApp`) and a classic
  **console text-menu UI** (`Application` + per-role `showMenu()` methods) both talk to the same
  service classes, demonstrating clean separation between UI and business logic.

---

## ✨ Features

| Area | Feature |
|---|---|
| 🔐 Authentication | Username/password login via `AuthService` (case-insensitive username matching) |
| 🧑‍🤝‍🧑 RBAC | 4 roles — Tester, Developer, PM, Admin — each with restricted capabilities in both UIs |
| 🐞 Bug management | Create, list, search, sort, view details, update status, assign to developers |
| 🔎 Search & sort | Live-filtered, column-sortable JavaFX `TableView` (filters on ID / name / status / assignee / reporter) |
| 🖼️ Screenshots | Attach an image (png/jpg/jpeg/gif/bmp, max 10 MB) to any bug; GUI shows a live preview |
| 📧 Email | SMTP notifications: developer is emailed on assignment, tester is emailed when the bug is closed |
| 📊 Insights / Reports | Open-vs-closed counts, per-developer completion rate, per-tester closure rate |
| 👤 User management | Admin can add / update / delete user accounts, choosing the role for each |
| 💾 Persistence | Auto-save to `users.txt` and `Bug.txt` after every mutation; seeded with defaults on first run |

---

## 🎭 The Four Roles

| Role | Class | Can do |
|---|---|---|
| **Tester** | `Tester.java` | Report bugs, assign bugs to developers (triggers email), attach screenshots, view open/closed bugs |
| **Developer** | `Developer.java` | View bugs assigned to them, move an assigned bug to *In Progress* or *Closed* (closing emails the reporter) |
| **Project Manager** | `PM.java` | Read-only analytics: developer performance, tester performance, open/closed bug counts |
| **Admin** | `Admin.java` | Everything oversight: view all bugs + full user management (add / update / delete accounts) |

In the JavaFX GUI these rules are enforced by `instanceof` guards at the top of each action
(e.g. only `Tester`/`Admin` may open the *Report* dialog; only a `Developer` who owns the
selected bug may change its status; only `PM`/`Admin` may open *Insights*; only `Admin` may open
*Users*). Full matrix and menu-by-menu details: **[docs/ROLES_AND_PERMISSIONS.md](docs/ROLES_AND_PERMISSIONS.md)**.

---

## 🏗️ Architecture

The codebase follows a simple **three-layer architecture** (all classes live in the default
package, as in the original course project):

```
┌───────────────────────────  UI LAYER  ────────────────────────────┐
│  Main.java ──launches──▶ BugTrackerApp.java   (JavaFX GUI)        │
│  ConsoleMain.java ─────▶ Application.java     (console menus)     │
│        role menus: Tester / Developer / PM / Admin .showMenu()    │
│        Screenshot.java (Swing JFileChooser for console mode)      │
└───────────────┬───────────────────────────────────────────────────┘
                │ calls
┌───────────────▼─────────────────  SERVICE LAYER  ─────────────────┐
│  AuthService   – login (validates credentials against users)      │
│  BugService    – in-memory bug list + add/save, seeds defaults    │
│  UserService   – in-memory user list + add/update/delete/find     │
│  EmailService  – SMTP send (javax.mail), env-var configuration    │
└───────────────┬───────────────────────────────────────────────────┘
                │ loads / saves through
┌───────────────▼─────────────  MODEL + PERSISTENCE  ───────────────┐
│  Model:  Bug.java,  User.java ◀─ Tester / Developer / PM / Admin  │
│  FileHandler.java  – CSV read/write for users.txt & Bug.txt       │
└───────────────────────────────────────────────────────────────────┘
```

Key design decisions:

- **Inheritance for roles:** `User` is the base class (username, password, email);
  `Tester`, `Developer`, `PM` and `Admin` extend it and override `getRole()`. Login returns a
  `User`, and the UI dispatches on the concrete subclass (`instanceof`).
- **Static service registries:** `BugService` and `UserService` hold the canonical
  `List<Bug>` / `List<User>` in static fields, hydrated from disk in a `static {}` block and
  seeded with demo records when the files are empty/missing. Every mutation calls
  `FileHandler.save…()` so the files never drift from memory.
- **Flat-file persistence:** `FileHandler` serializes each object to one comma-separated line
  (`split(",", limit)` on load, `BufferedWriter` on save) and normalizes legacy status values
  (`complete` → `Closed`).
- **GUI-agnostic core:** the same services power both the JavaFX and console UIs;
  `EmailService` reads all SMTP settings from environment variables so no secret is ever
  committed.

Detailed diagrams (class diagram + sequence diagrams for login, assign-with-email,
close-with-email): **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

---

## 📁 Project Structure

```
Bug_tracker_system/
├── README.md                     ← this file
├── pom.xml                       ← Maven build (Java 17, JavaFX 21, javax.mail)
├── .gitignore
├── users.txt                     ← runtime data: user accounts (username,password,role)
├── Bug.txt                       ← runtime data: bug records (12 CSV fields per line)
├── docs/
│   ├── ARCHITECTURE.md           ← layers, class diagram, sequence diagrams, design decisions
│   ├── CODE_WALKTHROUGH.md       ← file-by-file explanation of every class
│   ├── ROLES_AND_PERMISSIONS.md  ← role matrix + menus for GUI and console
│   ├── DATA_FILES.md             ← exact format of users.txt and Bug.txt
│   └── USER_GUIDE.md             ← step-by-step usage of the application
└── src/main/java/
    ├── Main.java                 ← JavaFX entry point (launches BugTrackerApp)
    ├── ConsoleMain.java          ← console entry point (launches Application)
    ├── BugTrackerApp.java        ← the complete JavaFX GUI (login + dashboard + dialogs)
    ├── Application.java          ← console login loop + role dispatch
    ├── AuthService.java          ← login validation
    ├── User.java                 ← base user model
    ├── Tester.java               ← tester role + console menu
    ├── Developer.java            ← developer role + console menu
    ├── PM.java                   ← project-manager role + console menu
    ├── Admin.java                ← admin role + console menu
    ├── UserService.java          ← user CRUD + lookup
    ├── Bug.java                  ← bug model
    ├── BugService.java           ← bug registry + persistence hooks
    ├── FileHandler.java          ← CSV load/save for both data files
    ├── EmailService.java         ← SMTP email sender (javax.mail)
    └── Screenshot.java           ← console-mode screenshot attachment (Swing file chooser)
```

---

## ✅ Requirements

| Tool | Version | Why |
|---|---|---|
| **JDK** | 17 or newer | code uses switch expressions (`case … ->`) and `Optional.isEmpty()` |
| **Maven** | 3.8+ | dependency management & run plugins |
| **JavaFX** | 21 (pulled automatically by Maven) | GUI toolkit; `CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS` needs JavaFX 20+ |
| Internet | first build only | Maven downloads JavaFX + JavaMail once, then caches them |

> Using an IDE (IntelliJ IDEA / Eclipse / VS Code)? Just **import the folder as a Maven
> project** — the `pom.xml` wires everything up, including the JavaFX module path.

---

## 🚀 Getting Started (GUI)

```bash
# 1. Clone
git clone https://github.com/Antonyos-Farag/Bug_tracker_system.git
cd Bug_tracker_system

# 2. Run the JavaFX application (compiles automatically)
mvn clean javafx:run
```

A dark login window appears. Sign in with one of the [default accounts](#-default-accounts),
e.g. `tester` / `123`, and you land on the main dashboard:

- **Top:** welcome label with your username and role.
- **Search box:** live filter across ID, name, status, assignee and reporter.
- **Table:** all bugs — click any column header to sort.
- **Buttons:** `Refresh`, `Report`, `Assign`, `Status`, `Screenshot`, `Insights`, `Users`,
  `Logout` — each is enabled/disabled by your role at click time (a warning dialog explains
  why an action is refused).
- **Bottom-left:** full details of the selected bug. **Bottom-right:** screenshot preview.

Full walkthrough with screenshots-worthy detail: **[docs/USER_GUIDE.md](docs/USER_GUIDE.md)**.

---

## 🖥️ Console Mode

The original text-menu interface is still fully functional on top of the same services:

```bash
mvn clean compile exec:java
```

Flow: *Press Enter to Start → username/password loop → "Welcome <Role>!" → that role's menu*
(Tester: report / assign / attach screenshot / view open / view closed; Developer: view
assigned / close bug; PM: developer & tester performance / status counts; Admin: view all bugs
/ add / update / delete users). Attachments use a Swing file-chooser dialog.

---

## 🔑 Default Accounts

Seeded by `UserService` on first run and shipped in `users.txt`:

| Username | Password | Role |
|---|---|---|
| `tester` | `123` | Tester |
| `developer` | `123` | Developer |
| `pm` | `123` | Project Manager |
| `admin` | `123` | Admin |
| `ahmad` | `1234` | Admin |
| `tony` | `123` | Developer |

> ⚠️ These are demo credentials for an educational project — see
> [Known Limitations](#-known-limitations--future-work).

---

## 🗂️ Data Files

Both files live in the **working directory** (the project root when you run via Maven) and are
created automatically with seed data if missing.

**`users.txt`** — one account per line:

```csv
username,password,role          ← format (no header line in the real file)
tester,123,tester
tony,123,developer
```

**`Bug.txt`** — one bug per line, 12 comma-separated fields:

```csv
id,name,description,type,priority,level,project,date,status,assignedTo,reportedBy,screenshotPath
1,Login Crash,App crashes on login,UI,High,Critical,BugTrack,2026-05-11,Open,developer,tester,
4,Play button,The play button doesnt work,UI,Medium,Major,GTA,2026-05-11,Open,,tony,D:\R.jpg
```

Exact field semantics, parsing rules and edge cases:
**[docs/DATA_FILES.md](docs/DATA_FILES.md)**.

---

## 📧 Email Notifications Setup

`EmailService` sends mail through SMTP (default: Gmail on port 465, SSL) using
**environment variables only** — no credentials in code:

| Variable | Required | Default | Meaning |
|---|---|---|---|
| `SMTP_USER` | ✅ | — | SMTP login (e.g. your Gmail address) |
| `SMTP_PASS` | ✅ | — | SMTP password (**Gmail App Password**, not your normal password) |
| `SMTP_HOST` | ❌ | `smtp.gmail.com` | SMTP server |
| `SMTP_PORT` | ❌ | `465` | SMTP port |
| `SMTP_FROM` | ❌ | value of `SMTP_USER` | From-address |

Example (Linux/macOS):

```bash
export SMTP_USER="youremail@gmail.com"
export SMTP_PASS="your-16-char-app-password"
mvn javafx:run
```

If the variables are not set, everything else still works — the assignment/closure flows simply
print `Failed to send email: Missing required environment variable: SMTP_USER` and continue.
Recipient addresses default to `<username>@gmail.com` (see `User`'s constructor) and are looked
up with `UserService.findEmailByUsername(...)`.

---

## 📚 Documentation Index

| Document | Contents |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layered design, Mermaid class diagram, sequence diagrams (login, assign+email, close+email), design rationale |
| [docs/CODE_WALKTHROUGH.md](docs/CODE_WALKTHROUGH.md) | Every class explained file-by-file with its key methods |
| [docs/ROLES_AND_PERMISSIONS.md](docs/ROLES_AND_PERMISSIONS.md) | Role definitions, GUI permission matrix, console menus, workflow responsibilities |
| [docs/DATA_FILES.md](docs/DATA_FILES.md) | `users.txt` / `Bug.txt` formats, field tables, parsing & normalization rules |
| [docs/USER_GUIDE.md](docs/USER_GUIDE.md) | How to run and use the app, scenario-by-scenario |

---

## ⚠️ Known Limitations & Future Work

Honest notes about the current (educational) implementation — good candidates for next steps:

1. **Passwords are stored in plain text** in `users.txt`. → hash them (e.g. BCrypt) before storing.
2. **CSV fragility:** a comma inside a bug name/description would shift the columns on load
   (`split(",", 12)` keeps the overflow in the last field only). → escape fields or move to JSON/SQLite.
3. **GUI reports hard-code** type/priority/level to `UI / Medium / Major` (the console flow asks
   for them). → add combo boxes to the Report dialog.
4. **No delete/edit for bugs** — only status changes and assignment.
5. **New bug IDs** are `max(id)+1`, so deleting records manually can reuse IDs.
6. **Screenshot paths are absolute** (`D:\R.jpg`) — attachments break if the file moves or on
   another machine. → copy images into a project `attachments/` folder.
7. **Single-machine only:** the files are local, so there is no multi-user concurrency control.
8. **Console `Scanner.close()`** calls close `System.in`, so a second menu in the same JVM run
   would fail — fine for the one-shot menu flow, worth refactoring if menus are chained.

---

## 🧭 Quick Command Reference

```bash
mvn clean javafx:run              # build + run the JavaFX GUI
mvn clean compile exec:java       # build + run the console version
mvn clean package                 # compile + package (target/*.jar)
```

---

*Educational project — a from-scratch implementation of the core ideas behind professional bug
trackers, using pure Java, JavaFX and flat-file storage.*
