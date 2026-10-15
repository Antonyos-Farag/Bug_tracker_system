# 📗 User Guide

How to install, launch and use the Bug Tracker System — for each role, in both the JavaFX GUI
and the console.

---

## 1. Install & Launch

**Prerequisites:** JDK 17+ and Maven 3.8+ (`java -version`, `mvn -version`).

```bash
git clone https://github.com/Antonyos-Farag/Bug_tracker_system.git
cd Bug_tracker_system

# Graphical interface (recommended)
mvn clean javafx:run

# …or the classic text-menu interface
mvn clean compile exec:java
```

The first run downloads JavaFX and JavaMail automatically and creates `users.txt` / `Bug.txt`
with seed data in the project root.

**IDE users:** import as a Maven project, then run `Main` (GUI) or `ConsoleMain` (console).
If your IDE runs JavaFX from the classpath and complains about modules, use the Maven goals
above instead — the `javafx-maven-plugin` handles the module path.

---

## 2. Logging In

Use any account from `users.txt`. Out of the box:

| Username | Password | You will see |
|---|---|---|
| `tester` | `123` | Tester capabilities |
| `developer` | `123` | Developer capabilities |
| `tony` | `123` | Developer capabilities |
| `pm` | `123` | Project Manager insights |
| `admin` | `123` | Full oversight + user management |
| `ahmad` | `1234` | Full oversight + user management |

Usernames are case-insensitive; passwords are case-sensitive. Wrong credentials show
*“Invalid credentials.”* (GUI) or re-prompt (console).

---

## 3. The Dashboard (GUI)

After login the main window shows:

```
┌────────────────────────────────────────────────────────────────────┐
│ Welcome tester (Tester)                                            │
│ [ Search...                                                      ] │
│ ┌────┬──────────────┬─────────┬──────────┬──────────┬───────────┐  │
│ │ ID │ Name         │ Status  │ Priority │ Assigned │ Reporter  │  │
│ ├────┼──────────────┼─────────┼──────────┼──────────┼───────────┤  │
│ │ 1  │ Login Crash  │ Open    │ High     │ developer│ tester    │  │
│ │ …  │              │         │          │          │           │  │
│ └────┴──────────────┴─────────┴──────────┴──────────┴───────────┘  │
│ [Refresh][Report][Assign][Status][Screenshot][Insights][Users][Logout]│
│ ┌───────────────────────────────┐ ┌────────────────────────────┐   │
│ │ bug details of selected row   │ │ Screenshot Preview         │   │
│ └───────────────────────────────┘ └────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────┘
```

- **Search** filters live on ID, name, status, assignee and reporter; click **column headers**
  to sort.
- Selecting a row fills the **details pane** and the **screenshot preview** (or explains why an
  image could not be loaded).
- Buttons you are not allowed to use are not hidden — clicking one explains the rule
  (e.g. *“Only Tester/Admin can report.”*).
- **Logout** returns to the login screen; **Refresh** re-reads the service list.

---

## 4. Task Recipes

### 4.1 Report a bug (Tester / Admin)

1. Click **Report**.
2. Fill **Name** and **Project** (both required), optionally a **Description**, the initial
   **Status** (defaults to *Open*) and the **Date** (defaults to today).
3. **Save** — the bug gets the next free ID, your username as reporter, and default
   type/priority/level (`UI` / `Medium` / `Major`) in the GUI dialog. The table refreshes and
   `Bug.txt` is rewritten.

*Console:* Tester menu → `1. Report a Bug` — here you are also asked for type, priority,
level and an optional date/status.

### 4.2 Attach a screenshot (any role)

1. Select the bug, click **Screenshot**, choose a `.png/.jpg/.jpeg/.gif/.bmp` file.
2. The absolute path is stored on the bug and the preview pane shows the image immediately.

> The image file itself is **not copied** — if you move or delete it, the preview will say
> “Screenshot file not found”. Console mode additionally enforces a 10 MB size limit.

### 4.3 Assign a bug to a developer (Tester / Admin)

1. Select the bug, click **Assign**.
2. Pick a developer from the list (only `developer`-role accounts appear) and confirm.
3. The **Assigned** column updates and `Bug.txt` is saved.

*Console:* Tester menu → `2. Assign Bug to Developer`. The console flow **also emails the
developer** (“New Bug Assigned to You”) if SMTP is configured — see §6.

### 4.4 Work a bug (Developer)

1. Log in as the developer the bug is assigned to.
2. Select the bug, click **Status**, choose **In Progress** while working, then **Closed**
   when the fix is done.
3. You can only change bugs assigned to *you* — otherwise you get
   *“Bug is not assigned to you.”*

*Console:* Developer menu → `1. View Assigned Bugs` to see your queue, `2. Close Bug` to move
a bug to In Progress/Closed. Closing **emails the original reporter** (“Bug Closed
Notification”) if SMTP is configured.

### 4.5 Read the reports (PM / Admin)

Click **Insights** and choose:

- **Bug Counts** — totals of Open / In Progress / Closed.
- **Developer Performance** — per developer: assigned, resolved, completion %.
- **Tester Performance** — per tester: reported, closed, closure %.

*Console:* PM menu options 1–3 produce the same numbers as text output.

### 4.6 Manage user accounts (Admin)

1. Click **Users** → choose **Add**, **Update** or **Delete**.
2. Enter the username; for Add/Update also the password and the role
   (`tester` / `developer` / `pm` / `admin`).
3. `users.txt` is saved after the operation. Updates replace the account object, so the new
   role/password take effect at the next login.

*Console:* Admin menu → options 2–4 with inline prompts; option 1 lists every bug.

---

## 5. A Full Round-Trip Scenario (try it!)

1. Log in as **tester/123** → *Report* a bug “Checkout button frozen” in project “ShopApp”.
2. Still as tester → *Assign* it to **tony**.
3. Log out, log in as **tony/123** → the bug shows in the table; select it → *Status* →
   **In Progress**; later → **Closed**.
4. Log out, log in as **pm/123** → *Insights* → **Bug Counts** shows one more Closed bug;
   **Developer Performance** shows tony’s completion rate rise.
5. Log out, log in as **admin/123** → *Users* → **Add** a new tester `sara` / `123` /
   `tester` → log out and sign in as `sara` to prove it works.
6. Open `Bug.txt` and `users.txt` in a text editor and see every change you made, one CSV line
   per record.

---

## 6. Enabling Email Notifications (optional)

Emails are sent only when these environment variables exist (Gmail defaults shown):

| Variable | Example | Notes |
|---|---|---|
| `SMTP_USER` | `youremail@gmail.com` | required |
| `SMTP_PASS` | `abcd efgh ijkl mnop` | required — a Gmail **App Password** (2-Step Verification must be on) |
| `SMTP_HOST` | `smtp.gmail.com` | optional |
| `SMTP_PORT` | `465` | optional (SSL) |
| `SMTP_FROM` | `youremail@gmail.com` | optional, defaults to `SMTP_USER` |

Linux / macOS:

```bash
export SMTP_USER="youremail@gmail.com"
export SMTP_PASS="your-app-password"
mvn javafx:run
```

Windows (PowerShell):

```powershell
$env:SMTP_USER = "youremail@gmail.com"
$env:SMTP_PASS = "your-app-password"
mvn javafx:run
```

Without configuration the app still works normally; email steps print
`Failed to send email: Missing required environment variable: SMTP_USER` and continue.
Recipients are looked up per username as `username@gmail.com` unless the `User` object carries
a custom email.

---

## 7. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `mvn javafx:run` fails with “JavaFX runtime components missing” | You ran the `Application` subclass directly from the IDE classpath. Run `Main` via the Maven goal, or add the JavaFX SDK to the module path in your run configuration. |
| Login always fails | Check `users.txt` in the *working directory* you launched from; passwords are case-sensitive. |
| Table is empty / shows only 3 demo bugs | `Bug.txt` was missing, empty or malformed (non-numeric ID, <12 fields) — the console prints `Error loading bugs: …`. Fix the line or delete the file to reseed. |
| “Screenshot file not found” | The stored path (e.g. `D:\R.jpg`) doesn’t exist on this machine — paths are absolute and not portable. Re-attach the image. |
| “No developers found” when assigning | No account with role `developer` exists — create one as Admin. |
| Email errors | See §6 — usually missing env vars or using a normal Gmail password instead of an App Password. |
| Compile error about switch `->` | JDK too old — install JDK 17+ and make sure `JAVA_HOME` points to it. |

---

## 8. Resetting Demo Data

Delete `users.txt` and/or `Bug.txt` from the project root and restart: both services reseed
their defaults (4 users with password `123`, 3 demo bugs) and write the files back on the next
save.
