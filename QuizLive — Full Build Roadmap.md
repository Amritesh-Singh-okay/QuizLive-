# QuizLive — Full Build Roadmap

**Java Web–Based Online Quiz Platform with Real-Time Leaderboard**

Built against the *Java Web Based Projects Marking Rubric* (Problem Understanding & Solution Design – 8, Core Java Concepts – 10, JDBC – 8, Servlets & Web Integration – 7) plus the full PDF feature spec for Admin / Quiz Creator / Participant.

---

## 0. Rubric → Where It's Covered

| Rubric Line | Marks | Where you build it |
| --- | --- | --- |
| Problem Understanding & Solution Design | 8 | §1 Architecture + §11 Feature map |
| OOP (Polymorphism, Inheritance, Exceptions, Interfaces) | part of Core Java | §3 Domain Model |
| Collections & Generics | part of Core Java | §3, §6 (leaderboard, in-memory session maps) |
| Multithreading & Synchronization | 4 | §7 |
| JDBC (incl. classes for DB ops) | 8 | §4 |
| Servlets & Web Integration | 7 | §8 |
| Security / Auth | — (graded under Solution Design) | §5 |

Every phase below explicitly tells you *which rubric box it fills*, so nothing you build is wasted effort.

---

## 1. Architecture (Day 1 — do this first, don't skip)

**3-tier, plain Java (no Spring):**

```
[Browser]
  HTML/CSS/JS (JSP for templated pages) + WebSocket client
        │  HTTP (fetch/forms)         │ WebSocket
        ▼                              ▼
[Servlet Layer]  ──filters (auth/role)──▶ [Service Layer]  ──▶ [DAO Layer: JDBC]  ──▶  [MySQL]
        ▲                                                                    │
        └────────────────── @ServerEndpoint (leaderboard push) ◀────────────┘
```

**Package structure** (Maven, deployed on Tomcat):

```
com.quizlive
├── model        → User, Quiz, Question, Attempt, AttemptAnswer, Message, enums
├── dao          → interfaces + JDBC implementations (UserDao, QuizDao, AttemptDao...)
├── service      → AuthService, QuizService, ScoringService, LeaderboardService
├── servlet      → LoginServlet, RegisterServlet, CreateQuizServlet, TakeQuizServlet,
│                  SubmitAnswerServlet, ApproveQuizServlet, MessageServlet...
├── filter       → AuthFilter, RoleFilter
├── websocket    → LeaderboardEndpoint
├── util         → PasswordUtil, DbConnectionUtil, JsonUtil
└── exception    → InvalidAttemptException, UnauthorizedException, QuizClosedException
```

- [ ] Create Maven project, add dependencies: `mysql-connector-j`, `javax.servlet-api`, `javax.websocket-api`, `gson` (JSON)
- [ ] Set up Tomcat run config
- [ ] Create the package skeleton above with empty classes

---

## 2. Database Design — *JDBC rubric, 8 marks* (Day 1)

```sql
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  salt VARCHAR(64) NOT NULL,
  role ENUM('ADMIN','CREATOR','PARTICIPANT') NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE quizzes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  description TEXT,
  creator_id INT NOT NULL,
  duration_seconds INT NOT NULL,
  status ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE questions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  quiz_id INT NOT NULL,
  question_text TEXT NOT NULL,
  option_a VARCHAR(255), option_b VARCHAR(255),
  option_c VARCHAR(255), option_d VARCHAR(255),
  correct_option CHAR(1) NOT NULL,
  points INT DEFAULT 1,
  FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

CREATE TABLE attempts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  quiz_id INT NOT NULL,
  user_id INT NOT NULL,
  started_at TIMESTAMP,
  submitted_at TIMESTAMP NULL,
  score INT DEFAULT 0,
  status ENUM('IN_PROGRESS','SUBMITTED','AUTO_SUBMITTED') DEFAULT 'IN_PROGRESS',
  UNIQUE KEY unique_attempt (quiz_id, user_id),
  FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE attempt_answers (
  id INT AUTO_INCREMENT PRIMARY KEY,
  attempt_id INT NOT NULL,
  question_id INT NOT NULL,
  selected_option CHAR(1),
  is_correct BOOLEAN,
  FOREIGN KEY (attempt_id) REFERENCES attempts(id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE messages (
  id INT AUTO_INCREMENT PRIMARY KEY,
  from_user_id INT NOT NULL,
  to_user_id INT NOT NULL,
  quiz_id INT,
  content TEXT NOT NULL,
  sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (from_user_id) REFERENCES users(id),
  FOREIGN KEY (to_user_id) REFERENCES users(id)
);

CREATE TABLE system_settings (
  setting_key VARCHAR(100) PRIMARY KEY,
  setting_value VARCHAR(255)
);
```

- [ ] Run this schema in MySQL
- [ ] `DbConnectionUtil` — single place that opens a `Connection` (use a simple connection pool like **HikariCP**, or plain `DriverManager` if you want zero extra deps — fine for this scale)
- [ ] **Integrity checks already baked in**: foreign keys, `ON DELETE CASCADE`, `UNIQUE` on email and on (quiz_id,user_id) so nobody attempts the same quiz twice
- [ ] Write 2–3 seed rows per table so you have test data from day one

---

## 3. Domain Model — *OOP rubric* (Day 1–2)

- [ ] `abstract class AppUser` with `id, name, email` → subclassed by `Admin`, `QuizCreator`, `Participant` (**inheritance**)
- [ ] `interface Gradeable { int computeScore(); }` implemented by `Attempt` (**interfaces/polymorphism**)
- [ ] Enums: `Role`, `QuizStatus`, `AttemptStatus`
- [ ] Custom checked exceptions: `InvalidAttemptException`, `QuizClosedException`, `UnauthorizedException` — thrown from service layer, caught in servlets, turned into clean JSON error responses (**exception handling**, explicitly graded)
- [ ] `Question`, `Quiz`, `Message` as plain model classes (POJOs)

---

## 4. DAO Layer — *JDBC, 8 marks* (Day 2)

Write **raw JDBC** here — no Hibernate/JPA, since that's what's graded.

- [ ] `UserDao` — `register()`, `findByEmail()`, `findById()`, `listAll()`, `deleteUser()`
- [ ] `QuizDao` — `create()`, `approve()/reject()`, `listApproved()`, `listByCreator()`
- [ ] `QuestionDao` — bulk insert questions for a quiz
- [ ] `AttemptDao` — `startAttempt()`, `submitAttempt()`, `getLeaderboard(quizId)`
- [ ] `MessageDao` — `send()`, `getThread(userA, userB)`
- [ ] Everything via `PreparedStatement` (never string-concatenated SQL — this is both a security requirement and a rubric point)
- [ ] Wrap multi-step writes (e.g. insert quiz + insert all its questions) in a single **transaction**: `conn.setAutoCommit(false)` → commit/rollback — this is what "database integrity" means in practice, and it's cheap to add

---

## 5. Authentication & Security — real auth, not fake (Day 2–3)

This satisfies "Solution Design" marks and is just good practice.

- [ ] **Password hashing**: use `javax.crypto` (built into the JDK, zero extra dependency) — `PBKDF2WithHmacSHA256` with a random salt per user and a few thousand iterations. This is Java's standard equivalent of bcrypt and needs no external library:

  ```java
  SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
  KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
  byte[] hash = f.generateSecret(spec).getEncoded();
  ```
- [ ] `RegisterServlet` → generate salt, hash password, store both, never store plaintext
- [ ] `LoginServlet` → re-hash input with stored salt, compare byte-for-byte, create `HttpSession` on success, store `userId` + `role` in session
- [ ] `AuthFilter` (a Servlet `Filter`) → blocks any request to protected URLs if no valid session exists
- [ ] `RoleFilter` → checks `session.getAttribute("role")` matches what the endpoint requires (Admin-only URLs reject Creators/Participants, etc.)
- [ ] **SQL injection**: already covered — PreparedStatements everywhere
- [ ] **XSS**: escape any user-submitted text (quiz titles, messages) before rendering in JSP — use a small helper (`StringEscapeUtils`-style) or JSTL `<c:out>`
- [ ] **Session fixation basics**: call `session.invalidate()` on logout, regenerate session ID on login

---

## 6. Service Layer (Day 3)

- [ ] `AuthService` — wraps hashing + DAO calls
- [ ] `QuizService` — create, approve/reject, fetch for taking (strip `correct_option` before sending to participant!)
- [ ] `ScoringService` — compares submitted answers to correct ones server-side, computes score, never trusts a client-supplied score
- [ ] `LeaderboardService` — builds sorted ranking: score desc, then `submitted_at` asc as tiebreaker (`List<Attempt>` + `Comparator.comparing(...).thenComparing(...)` — **Collections/Generics** rubric point)

---

## 7. Multithreading & Synchronization — *4 marks* (Day 3–4)

This is the part students usually fake or skip — don't. Build it for real, it's a natural fit here:

- [ ] When a participant starts a quiz, schedule an auto-submit using `ScheduledExecutorService`:

  ```java
  scheduler.schedule(() -> autoSubmit(attemptId), durationSeconds, TimeUnit.SECONDS);
  ```
- [ ] Keep active attempts in a `ConcurrentHashMap<Integer, AttemptSession>` (thread-safe by construction — demonstrates you understand *why* you need it, not just `HashMap`)
- [ ] When two participants submit at the same moment, the leaderboard update method must be `synchronized` (or use a `ReentrantLock`) to prevent a race condition corrupting the ranking
- [ ] Server-side time validation on manual submit too: reject (or clamp) if `now - startedAt > durationSeconds + small buffer` — this is your **anti-cheat check**, and it's the same logic as the auto-submit thread, so it's not extra work

---

## 8. Servlets & Web Integration — *7 marks* (Day 4)

| Servlet | Endpoint | Role |
| --- | --- | --- |
| `RegisterServlet` | `POST /register` | all |
| `LoginServlet` | `POST /login` | all |
| `CreateQuizServlet` | `POST /quizzes` | Creator |
| `ApproveQuizServlet` | `POST /admin/quizzes/{id}/approve` | Admin |
| `ListQuizzesServlet` | `GET /quizzes` | Participant |
| `StartAttemptServlet` | `POST /quizzes/{id}/start` | Participant |
| `SubmitAttemptServlet` | `POST /attempts/{id}/submit` | Participant |
| `LeaderboardServlet` | `GET /quizzes/{id}/leaderboard` | all (initial load; live updates via WebSocket) |
| `MessageServlet` | `POST /messages`, `GET /messages/{threadId}` | Creator ↔ Participant |
| `UserManagementServlet` | `GET/POST/DELETE /admin/users` | Admin |
| `SettingsServlet` | `POST /admin/settings` | Admin |

- [ ] Every Servlet returns JSON (use `gson` to serialize) so your JS frontend can be a thin client
- [ ] Register `AuthFilter`/`RoleFilter` in `web.xml` (or via `@WebFilter`) against the right URL patterns

---

## 9. Real-Time Leaderboard — WebSockets (Day 4–5)

This is your standout feature — satisfies "Leaderboard" in the PDF and is the thing that makes the demo memorable.

- [ ] `@ServerEndpoint("/ws/leaderboard/{quizId}")` class, using `javax.websocket` (built into Tomcat, no Spring needed)
- [ ] On `SubmitAttemptServlet` success → after scoring, call `LeaderboardEndpoint.broadcast(quizId, updatedLeaderboardJson)` to push to every connected session for that quiz
- [ ] Frontend: `new WebSocket("ws://.../ws/leaderboard/" + quizId)`, on message → re-render the leaderboard table without a page refresh
- [ ] **Demo move**: open two browser tabs, take the quiz in both, submit one — watch the other tab's leaderboard update live

---

## 10. Frontend (Day 5–6)

Keep it plain: JSP for page shells + vanilla JS/fetch for dynamic bits. No need for React here — it adds nothing the rubric is looking for and costs you time.

**Pages per role** (mapped straight from the PDF dashboards):

- **Admin**: user table (create/edit/delete), quiz approval queue, settings form, performance chart (simple `<canvas>` bar chart of avg scores — no library needed, or use Chart.js via CDN), alerts list
- **Creator**: create-quiz form (dynamic "add question" rows), quiz results table, message panel, quiz history list, performance overview
- **Participant**: quiz list → take-quiz page (countdown timer in JS, synced against server time), post-submit report (per-question right/wrong breakdown), message panel, "upcoming quizzes" reminder list, live leaderboard
- [ ] One shared CSS file, keep it clean — doesn't need to be fancy, just not broken/ugly
- [ ] Mobile-responsive basics: flexible widths, no fixed pixel layouts
- [ ] Countdown timer in JS for UX, but remember: **the real enforcement is server-side** (§7) — JS timer is just what the user sees

---

## 11. Full Feature Checklist (straight from the PDF — tick every box)

**Admin**: ✅ user management · ✅ quiz approval · ✅ system settings · ✅ performance graphs · ✅ system alerts **Creator**: ✅ create quizzes · ✅ review results · ✅ message participants · ✅ quiz history · ✅ performance overview **Participant**: ✅ take timed quizzes · ✅ performance report · ✅ message creators · ✅ quiz reminders · ✅ leaderboard

---

## 12. Differentiators (don't skip — this is what avoids "generic quiz app")

- [ ] **Live leaderboard via WebSocket** (already core to §9 — your #1 wow-moment)
- [ ] **Server-side anti-cheat**: reject late submissions even if client-side timer was tampered with; show this live in your demo
- [ ] **Per-question breakdown** after submit (right/wrong + correct answer shown) instead of just a score number
- [ ] **Tab-switch detection**: JS `visibilitychange` event flags "participant left the tab during the quiz" to the Creator — cheap to add (a few lines of JS + one DB column), looks like a real exam-proctoring feature
- [ ] **Auto-submit on timeout**, driven by the real server-side thread, not just the JS countdown hitting zero

---

## 13. Testing & Polish (Day 6)

- [ ] Manually test each role's full flow end to end
- [ ] Test the race condition: two attempts submitting within the same second — confirm leaderboard doesn't corrupt
- [ ] Test the cheat case: manually call the submit endpoint after time's up — confirm it's rejected
- [ ] Clean up: remove dead code, add comments, consistent naming (code quality is explicitly in the Review 1 guidelines)

---

## 14. GitHub & Submission (Day 6–7 — matches Review 1 requirements from the portal)

- [ ] Public repo, clear structure matching §1's package layout
- [ ] `README.md` with: project overview, tech stack, how to set up the DB, how to run (`mvn`/Tomcat steps), screenshots
- [ ] Presentation (PDF/PPT) with: problem statement, architecture diagram (reuse §1's diagram), screenshots of each dashboard, and a note on which rubric items map to which part of the code (makes the reviewer's job easy — this helps you)
- [ ] Double-check both are submitted **before 10 Oct 2026, 11:59 PM**

---

## Suggested 7-Day Timeline (today: 3 Oct)

| Day | Focus |
| --- | --- |
| 1 | §1 Architecture setup + §2 DB schema |
| 2 | §3 Domain model + §4 DAO/JDBC |
| 3 | §5 Auth + §6 Service layer |
| 4 | §7 Multithreading + §8 Servlets |
| 5 | §9 WebSockets + start §10 Frontend |
| 6 | Finish §10 Frontend + §12 Differentiators + §13 Testing |
| 7 | §14 README, PPT, final GitHub push — submit early, not at 11:58 PM |