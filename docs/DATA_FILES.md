# 🗂️ Data Files: `users.txt` & `Bug.txt`

The system persists everything in **two flat text files** located in the *working directory*
of the JVM (the project root when launched with `mvn javafx:run` / `mvn exec:java`).
All reading and writing goes through `FileHandler.java` — no other class touches the disk.

```
FileHandler
   ├── USERS_FILE = "users.txt"   → saveUsers() / loadUsers()
   └── BUGS_FILE  = "Bug.txt"     → saveBugs()  / loadBugs()
```

Both files are **created automatically** if missing: the services seed demo records
(4 default users, 3 demo bugs) the first time anything is saved.

---

## 1. `users.txt` — the *Users Accounts* file

**One account per line, 3 comma-separated fields, no header:**

```
username,password,role
```

### Example (shipped with the repository)

```csv
tester,123,tester
developer,123,developer
pm,123,pm
admin,123,admin
ahmad,1234,admin
tony,123,developer
```

### Field reference

| # | Field | Rules |
|---|---|---|
| 1 | `username` | Unique (case-insensitive — `AuthService` and `UserService.findUserByUsername` use `equalsIgnoreCase`). Login is case-insensitive for usernames. |
| 2 | `password` | Plain text, case-sensitive at login. *(Educational project — see README limitations.)* |
| 3 | `role` | One of `tester`, `developer`, `pm`, `admin` (loading also accepts `project manager` as an alias for `pm`). Matching is case-insensitive and trimmed. |

### Parsing behaviour (`FileHandler.loadUsers`)

- Blank/whitespace-only lines are skipped.
- Each line is split with `split(",", 3)` — at most 3 parts, so a stray comma in the role
  column would remain inside field 3 (which would then fail role matching and drop the line).
- Lines with fewer than 3 parts are skipped.
- An **unknown role ⇒ the whole line is ignored** (`createUser` returns `null`).

### Writing behaviour (`FileHandler.saveUsers`)

- Role is written via `getStoredRole(User)`, an `instanceof` reverse-mapping that always emits
  the canonical short names (`tester` / `developer` / `pm` / `admin`).
- Emails are **not stored** — `User`'s constructor derives `username@gmail.com` on load.
- The entire file is rewritten on every user add/update/delete.

---

## 2. `Bug.txt` — the *Bugs* file

**One bug per line, 12 comma-separated fields, no header:**

```
id,bugName,description,bugType,priority,bugLevel,projectName,bugDate,status,assignedTo,reportedBy,screenshotPath
```

### Example (shipped with the repository)

```csv
1,Login Crash,App crashes on login,UI,High,Critical,BugTrack,2026-05-11,Open,developer,tester,
2,Stats Missing,Dashboard stats not loading,Backend,Medium,Major,BugTrack,2026-05-11,Closed,developer,tester,
3,Email Bug,Email notification not sent,Backend,Medium,Minor,BugTrack,2026-05-11,Closed,developer,tester,
4,Play button,The play button doesnt work,UI,Medium,Major,GTA,2026-05-11,Open,,tony,D:\R.jpg
5,123,12345,UI,Medium,Major,12341,2026-05-11,Closed,tony,tester,D:\R.jpg
```

Note line 4: `assignedTo` is **empty** (bug not yet assigned) — empty fields are legal and
load as `""`.

### Field reference

| # | Field | Type / format | Notes |
|---|---|---|---|
| 1 | `id` | integer | Unique key; new ids are computed as `max(existing)+1` |
| 2 | `bugName` | text | Short title |
| 3 | `description` | text | Details; GUI reports may store any text, console reports may leave it empty |
| 4 | `bugType` | text | Convention: `UI` / `Backend` / `Performance` (free-form on load) |
| 5 | `priority` | text | Convention: `High` / `Medium` / `Low` |
| 6 | `bugLevel` | text | Convention (severity): `Critical` / `Major` / `Minor` |
| 7 | `projectName` | text | The product/project the bug belongs to |
| 8 | `bugDate` | `yyyy-MM-dd` | Parsed with `SimpleDateFormat`; unparseable/empty ⇒ `null` (shown blank in GUI) |
| 9 | `status` | text | `Open` / `In Progress` / `Closed`; legacy `Complete` is **normalized to `Closed` on load** |
| 10 | `assignedTo` | username | Must match a `Developer` account to be assignable; empty until assigned |
| 11 | `reportedBy` | username | Stamped automatically with the reporter's username |
| 12 | `screenshotPath` | absolute file path | e.g. `D:\R.jpg`; empty when no attachment; image files are **not** copied into the project |

### Parsing behaviour (`FileHandler.loadBugs`)

- Blank lines skipped.
- `split(",", 12)`: **the limit matters** — commas inside the *last* field (screenshot path)
  are preserved, but commas inside earlier fields (e.g. a bug name like “Crash, sometimes”)
  will shift columns. Lines ending up with fewer than 12 parts are skipped.
- `Integer.parseInt` on field 1 — a malformed id throws, and the **whole load aborts** with a
  printed error (the outer `catch (Exception)`), so keep ids numeric.
- Date parse errors are swallowed per-line (`catch (ParseException ignored)`), yielding
  `date = null`.
- `normalizeStatus("complete") → "Closed"` keeps old data files compatible.

### Writing behaviour (`FileHandler.saveBugs`)

- Dates are formatted `yyyy-MM-dd` (`null` date ⇒ empty field).
- The entire file is rewritten after **every** mutation: report, assign, status change,
  screenshot attach — so `Bug.txt` always mirrors the in-memory `BugService` list.

---

## 3. Consistency Rules Between the Two Files

The files are independent, but the app assumes these conventions:

| Reference | Convention | What happens if violated |
|---|---|---|
| `Bug.assignedTo` → `users.txt` | Should be a `developer` account | Console assign rejects non-developers; GUI dropdown only lists developers, but a manually edited file could reference anyone |
| `Bug.reportedBy` → `users.txt` | Should be a `tester`/`admin` account | Email falls back to `reportedBy.toLowerCase()@gmail.com` if the lookup fails |
| `screenshotPath` → filesystem | Absolute path to an existing image | GUI preview shows “Screenshot file not found”; console validation rejects >10 MB or non-image files at attach time |

---

## 4. Inspecting / Editing the Files Safely

Because they are plain text you can inspect them with any editor or:

```bash
cat users.txt
column -s, -t Bug.txt | less -S      # pretty-print the bugs table
```

Rules of thumb when hand-editing:

1. Keep **exactly 12 fields** per bug line (trailing empty fields need their commas:
   `...,tester,` is valid, `...,tester` is not).
2. **No commas inside fields** other than the last one.
3. Dates strictly `yyyy-MM-dd`.
4. Roles strictly `tester` / `developer` / `pm` / `admin`.
5. Ids numeric and unique.
6. Close the app first — it rewrites the whole file on the next save and would overwrite
   concurrent manual edits.
