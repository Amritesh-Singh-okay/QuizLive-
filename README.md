# QuizLive — Enterprise Proctored Real-Time Assessment Platform

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-10%20(Servlet%206.0)-1A2B4C?style=for-the-badge&logo=eclipsevert.x&logoColor=white)](https://jakarta.ee/)
[![Apache Tomcat](https://img.shields.io/badge/Apache%20Tomcat-10.1%20Embedded-F8DC75?style=for-the-badge&logo=apachetomcat&logoColor=black)](https://tomcat.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%20InnoDB-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![HikariCP](https://img.shields.io/badge/HikariCP-5.1.0%20Pool-2C8EBB?style=for-the-badge)](https://github.com/brettwooldridge/HikariCP)
[![WebSocket](https://img.shields.io/badge/WebSocket-JSR%20356-009688?style=for-the-badge)](https://jakarta.ee/specifications/websocket/2.1/)
[![Tests](https://img.shields.io/badge/JUnit%205-111%20PASSED%20(100%25)-brightgreen?style=for-the-badge&logo=junit5&logoColor=white)]()
[![Docker](https://img.shields.io/badge/Docker-Multi--Stage%20Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

> **QuizLive** is a production-grade, high-concurrency, real-time proctored online assessment platform built in pure modern Java (**Jakarta EE 10 / Servlet 6.0**, **WebSocket 2.1**, **HikariCP**, and **Embedded Apache Tomcat 10.1**).  
> Designed with a **Zero-Trust Server-Authoritative Architecture**, QuizLive prevents cheating at the network, client, and protocol layers while delivering sub-second cold starts and live millisecond leaderboard synchronization.

---

## 🏗️ System Architecture & Solution Design

### 1. Problem Statement & Zero-Trust Architecture

Traditional online quiz apps suffer from critical security vulnerabilities:
1. **Answer Key Leakage:** Answers are often bundled in client HTML/JS payloads, easily viewed in F12 DevTools.  
   $\rightarrow$ *QuizLive Solution:* [`Question.sanitized()`](src/main/java/com/quizlive/model/Question.java#L43) strips answer keys before transmission.
2. **Client-Clock Manipulation:** JavaScript timers can be paused or bypassed by altering the OS clock.  
   $\rightarrow$ *QuizLive Solution:* Timers run strictly on background `ScheduledExecutorService` daemon threads on the server.
3. **Unmonitored Academic Dishonesty:** Students switch tabs to look up answers.  
   $\rightarrow$ *QuizLive Solution:* HTML5 Page Visibility API captures defocus events and logs tab switches server-side in real time.
4. **Polling Lag & Database Overload:** Client HTTP polling crashes databases during simultaneous classroom tests.  
   $\rightarrow$ *QuizLive Solution:* Native JSR-356 WebSockets push leaderboard changes to all observers in milliseconds.

---

### 2. High-Level 3-Tier Layered Architecture

QuizLive is engineered cleanly in pure Java on Embedded Tomcat 10.1 without Spring Boot overhead:

```mermaid
flowchart TD
    subgraph Client ["Presentation Layer (Browser)"]
        UI_P["Participant Portal (take-quiz.jsp)"]
        UI_C["Creator Studio (dashboard.jsp)"]
        UI_A["Admin Suite (dashboard.jsp)"]
        WS_C["WebSocket Client (/ws/*)"]
    end

    subgraph Filters ["Security and Routing Filter Layer"]
        F_Auth["AuthFilter (Session Gatekeeper)"]
        F_Role["RoleFilter (Role-Based RBAC)"]
    end

    subgraph Controllers ["Web and Servlet Layer (Jakarta EE 10 / Tomcat 10.1)"]
        S_Auth["Auth Servlets (Login / Register / Logout)"]
        S_Quiz["Quiz and Attempt Servlets (Create / Start / Submit)"]
        S_Admin["Admin Servlets (Approve / Users / Settings)"]
        S_WS["WebSocket Endpoints (Leaderboard and WaitingRoom)"]
    end

    subgraph Services ["Service and Concurrency Layer"]
        B_Auth["AuthService (PBKDF2 Hashing)"]
        B_Score["ScoringService and QuizService"]
        B_Rank["LeaderboardService (Comparator Sorter)"]
        B_Sched["QuizSchedulerService (Daemon Auto-Submit)"]
    end

    subgraph DAO ["Data Access Layer (DAO Pattern)"]
        D_User["UserDaoImpl"]
        D_Quiz["QuizDaoImpl"]
        D_Att["AttemptDaoImpl"]
        D_Other["QuestionDao / MessageDao / SettingsDao"]
    end

    subgraph Storage ["Persistence Layer"]
        Pool["HikariCP 5.1.0 Connection Pool"]
        DB[("MySQL 8.0 InnoDB Database")]
    end

    UI_P -->|HTTP REST / JSON| F_Auth
    UI_C -->|HTTP REST / JSON| F_Auth
    UI_A -->|HTTP REST / JSON| F_Auth
    F_Auth --> F_Role
    F_Role --> S_Auth
    F_Role --> S_Quiz
    F_Role --> S_Admin
    WS_C <-->|Full-Duplex WS| S_WS
    S_Auth --> Services
    S_Quiz --> Services
    S_Admin --> Services
    Services --> DAO
    DAO --> Pool
    Pool --> DB
    B_Score -.->|Live Event Push| S_WS
```

---

### 3. End-to-End Quiz Lifecycle Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Creator as Quiz Creator
    actor Admin as Administrator
    actor Student as Student Participant
    participant App as Servlets and Filters
    participant Svc as Quiz Engine
    participant DB as MySQL Database
    participant WS as WebSocket Endpoint

    Creator->>App: POST /api/quizzes/create
    App->>DB: Atomic Transaction: Insert Quiz + Questions
    Admin->>App: POST /api/admin/quizzes/approve
    App->>DB: UPDATE quizzes SET status = APPROVED
    Creator->>App: Host Quiz and Open Waiting Room
    Student->>WS: Connect to waiting room lobby
    WS-->>Creator: Update live participant counter
    Creator->>WS: Broadcast START_QUIZ signal
    WS-->>Student: Redirect to take-quiz.jsp
    Student->>App: POST /api/quizzes/start
    App->>Svc: Register AttemptSession and Schedule Auto-Submit
    App-->>Student: Deliver Sanitized Questions (Answers Stripped)
    opt Tab Switch or Window Defocus
        Student->>App: POST /api/attempts/tab-switch (Telemetry)
        App->>Svc: Increment AtomicInteger counter
        App->>DB: UPDATE attempts SET tab_switches = tab_switches + 1
    end
    alt Manual Submission
        Student->>App: POST /api/quizzes/submit
        App->>Svc: Cancel Scheduled Timer and Grade Server-Side
        App->>DB: Transaction: Save Attempt + Answers (COMMIT)
    else Timeout Exceeded
        Svc->>Svc: ScheduledExecutorService triggers handleAutoSubmit()
        Svc->>DB: UPDATE attempts SET status = AUTO_SUBMITTED (COMMIT)
    end
    Svc->>WS: Broadcast Leaderboard Update
    WS-->>Student: Live Leaderboard Push Update
```

---

### 4. Role-Based Access Control (RBAC) Workflows

```mermaid
flowchart LR
    subgraph Roles ["Platform Roles"]
        R_Admin["ADMIN"]
        R_Creator["CREATOR"]
        R_Participant["PARTICIPANT"]
    end

    subgraph AdminActions ["Administrator Suite"]
        A1["Manage Users"]
        A2["Approve Quizzes"]
        A3["System Settings"]
    end

    subgraph CreatorActions ["Creator Studio"]
        C1["Author Quizzes"]
        C2["Host Waiting Room"]
        C3["Monitor Tab Switches"]
    end

    subgraph ParticipantActions ["Participant Portal"]
        P1["Enter Access Code"]
        P2["Take Assessment"]
        P3["Live Leaderboard"]
    end

    R_Admin --> AdminActions
    R_Creator --> CreatorActions
    R_Participant --> ParticipantActions
```

---

## ☕ Core Java Concepts & OOP Implementation

### 1. OOP Inheritance, Polymorphism & Abstraction

```mermaid
classDiagram
    class AppUser {
        <<abstract>>
        -int id
        -String name
        -String email
        -Role role
        +canCreateQuiz()* boolean
        +canApproveQuiz()* boolean
        +canTakeQuiz()* boolean
        +getDashboardUrl()* String
        +create(role)$ AppUser
    }
    class Admin {
        +canCreateQuiz() boolean
        +canApproveQuiz() boolean
        +canTakeQuiz() boolean
        +getDashboardUrl() String
    }
    class QuizCreator {
        +canCreateQuiz() boolean
        +canApproveQuiz() boolean
        +canTakeQuiz() boolean
        +getDashboardUrl() String
    }
    class Participant {
        +canCreateQuiz() boolean
        +canApproveQuiz() boolean
        +canTakeQuiz() boolean
        +getDashboardUrl() String
    }

    AppUser <|-- Admin : extends
    AppUser <|-- QuizCreator : extends
    AppUser <|-- Participant : extends
```

- **Inheritance & Polymorphic Dispatch:** [`AppUser`](src/main/java/com/quizlive/model/AppUser.java#L12) declares abstract methods (`canCreateQuiz()`, `canApproveQuiz()`, `canTakeQuiz()`, `getDashboardUrl()`) implemented specifically by each subclass.
- **Factory Pattern:** `AppUser.create(...)` encapsulates polymorphic instantiation based on the `Role` enum.
- **Interface Abstraction:** The [`Gradeable`](src/main/java/com/quizlive/model/Gradeable.java#L3) interface contract (`computeScore()`, `getPercentage()`, `isPassed()`) is implemented by [`Attempt.java`](src/main/java/com/quizlive/model/Attempt.java#L10), decoupling evaluation logic from UI presentation.
- **Custom Exception Hierarchy:** Custom domain exceptions ([`InvalidAttemptException`](src/main/java/com/quizlive/exception/InvalidAttemptException.java), [`QuizClosedException`](src/main/java/com/quizlive/exception/QuizClosedException.java), [`UnauthorizedException`](src/main/java/com/quizlive/exception/UnauthorizedException.java)) enforce business integrity and translate to clean JSON error payloads.

---

### 2. Collections & Generics

1. **Type-Safe Generics:** Extensive use of `List<Question>`, `List<AttemptAnswer>`, and `Map<Integer, Set<Session>>`.
2. **Multi-Attribute Comparator Chaining:** [`LeaderboardService`](src/main/java/com/quizlive/service/LeaderboardService.java#L22) sorts attempts using a multi-factor comparator:
   ```java
   private static final Comparator<Attempt> RANKING_COMPARATOR = Comparator
           .comparing(Attempt::getScore, Comparator.reverseOrder())
           .thenComparing(Attempt::getDurationSeconds)
           .thenComparing(Attempt::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()));
   ```

---

### 3. Multithreading & Synchronization

1. **Daemon Scheduled Executor:** [`QuizSchedulerService`](src/main/java/com/quizlive/concurrency/QuizSchedulerService.java#L25) manages a pool of 4 named daemon threads scheduling timeout auto-submissions.
2. **Thread-Safe Registries:** Active sessions are stored in a `ConcurrentHashMap<Integer, AttemptSession>` inside [`ActiveAttemptRegistry`](src/main/java/com/quizlive/concurrency/ActiveAttemptRegistry.java#L10).
3. **Lock-Free Atomic Counters:** Tab switches are tracked via `AtomicInteger.incrementAndGet()` in [`AttemptSession`](src/main/java/com/quizlive/concurrency/AttemptSession.java#L16).
4. **Synchronization Locks:** Leaderboard recalculations and timeout handlers are guarded with `synchronized` methods to prevent double-grading race conditions.

---

## 💾 Database Integration & JDBC Operations

### 1. Relational Database Schema & ER Model

```mermaid
erDiagram
    USERS ||--o{ QUIZZES : "creates (1:N)"
    USERS ||--o{ ATTEMPTS : "undertakes (1:N)"
    USERS ||--o{ MESSAGES : "sends/receives (1:N)"
    QUIZZES ||--o{ QUESTIONS : "contains (1:N)"
    QUIZZES ||--o{ ATTEMPTS : "attempted_in (1:N)"
    ATTEMPTS ||--o{ ATTEMPT_ANSWERS : "records (1:N)"
    QUESTIONS ||--o{ ATTEMPT_ANSWERS : "evaluated_by (1:N)"

    USERS {
        int id PK
        string name
        string email UK
        string password_hash
        string salt
        enum role
        timestamp created_at
    }

    QUIZZES {
        int id PK
        string title
        text description
        int creator_id FK
        int duration_seconds
        enum status
        string access_code UK
        boolean is_public
    }

    QUESTIONS {
        int id PK
        int quiz_id FK
        text question_text
        string option_a
        string option_b
        string option_c
        string option_d
        char correct_option
        int points
    }

    ATTEMPTS {
        int id PK
        int quiz_id FK
        int user_id FK
        timestamp started_at
        timestamp submitted_at
        int score
        int tab_switches
        enum status
    }

    ATTEMPT_ANSWERS {
        int id PK
        int attempt_id FK
        int question_id FK
        char selected_option
        boolean is_correct
    }

    MESSAGES {
        int id PK
        int from_user_id FK
        int to_user_id FK
        text content
        timestamp sent_at
    }

    SYSTEM_SETTINGS {
        string setting_key PK
        string setting_value
    }
```

---

### 2. Classes for Database Operations (DAO Pattern)

All SQL operations are isolated behind 7 DAO interface contracts:
- [`UserDao`](src/main/java/com/quizlive/dao/UserDao.java) / [`UserDaoImpl`](src/main/java/com/quizlive/dao/impl/UserDaoImpl.java) — User CRUD & authentication
- [`QuizDao`](src/main/java/com/quizlive/dao/QuizDao.java) / [`QuizDaoImpl`](src/main/java/com/quizlive/dao/impl/QuizDaoImpl.java) — Quiz lifecycle & publishing
- [`QuestionDao`](src/main/java/com/quizlive/dao/QuestionDao.java) / [`QuestionDaoImpl`](src/main/java/com/quizlive/dao/impl/QuestionDaoImpl.java) — Batch question inserts & answer sanitization
- [`AttemptDao`](src/main/java/com/quizlive/dao/AttemptDao.java) / [`AttemptDaoImpl`](src/main/java/com/quizlive/dao/impl/AttemptDaoImpl.java) — Exam attempts & proctoring telemetry
- [`AttemptAnswerDao`](src/main/java/com/quizlive/dao/AttemptAnswerDao.java) / [`AttemptAnswerDaoImpl`](src/main/java/com/quizlive/dao/impl/AttemptAnswerDaoImpl.java) — Per-question evaluation logging
- [`MessageDao`](src/main/java/com/quizlive/dao/MessageDao.java) / [`MessageDaoImpl`](src/main/java/com/quizlive/dao/impl/MessageDaoImpl.java) — Instructor/student Q&A threads
- [`SettingsDao`](src/main/java/com/quizlive/dao/SettingsDao.java) / [`SettingsDaoImpl`](src/main/java/com/quizlive/dao/impl/SettingsDaoImpl.java) — System key-value configurations

---

### 3. JDBC Connectivity, HikariCP & ACID Transactions

- **HikariCP 5.1.0 Connection Pool:** Configured in [`DbConnectionUtil.java`](src/main/java/com/quizlive/util/DbConnectionUtil.java#L19) using double-checked locking singleton with prepared statement caching (`cachePrepStmts=true`).
- **100% Parameterized Queries:** Every database query strictly utilizes `PreparedStatement`, eliminating SQL injection risks.
- **ACID Transaction Boundaries:** Multi-row operations in [`QuizDaoImpl.createWithQuestions`](src/main/java/com/quizlive/dao/impl/QuizDaoImpl.java#L77) and [`AttemptDaoImpl.submitAttempt`](src/main/java/com/quizlive/dao/impl/AttemptDaoImpl.java#L144) use `conn.setAutoCommit(false)`, `conn.commit()`, and `conn.rollback()` in `catch (SQLException e)` blocks.

---

## 🌐 Servlets & Web Integration

### 1. Jakarta Servlets & Web Filters

- **16 Jakarta Servlets (`@WebServlet`):** Full REST API coverage for authentication, quiz authoring, host controls, attempt grading, and administration.
- **[`AuthFilter`](src/main/java/com/quizlive/filter/AuthFilter.java#L21):** Intercepts all traffic, validates active HTTP sessions, and whitelists public assets.
- **[`RoleFilter`](src/main/java/com/quizlive/filter/RoleFilter.java#L19):** Enforces strict Role-Based Access Control across `/admin/*`, `/creator/*`, and `/participant/*`.

---

### 2. Real-Time WebSocket Engine (JSR-356)

```mermaid
flowchart LR
    Browser1["Student Tab A (Observer)"] <-->|"WS: /ws/leaderboard/:quizId"| Server["LeaderboardEndpoint"]
    Browser2["Student Tab B (Observer)"] <-->|"WS: /ws/leaderboard/:quizId"| Server
    Browser3["Student Tab C (Submitting)"] -->|POST /api/quizzes/submit| Servlets["SubmitAttemptServlet"]
    Servlets -->|Trigger Broadcast| Server
    Server -->|Instant Push Notification| Browser1
    Server -->|Instant Push Notification| Browser2
```

- **[`LeaderboardEndpoint`](src/main/java/com/quizlive/websocket/LeaderboardEndpoint.java#L23):** Real-time push updates broadcast score and rank recalculations without client polling.
- **[`WaitingRoomEndpoint`](src/main/java/com/quizlive/websocket/WaitingRoomEndpoint.java#L27):** Live lobby synchronizes waiting student counters and initiates instant exam redirection.

---

## 🧪 Automated Test Suite (111 / 111 Tests Passing)

QuizLive includes **111 automated tests** verifying unit logic, JDBC DAOs, concurrency executors, and end-to-end browser flows:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.quizlive.ActiveAttemptRegistryTest          (3 tests  - PASSED)
[INFO] Running com.quizlive.AdminAndMessagingServletTest        (5 tests  - PASSED)
[INFO] Running com.quizlive.AppLauncherAndHealthTest           (2 tests  - PASSED)
[INFO] Running com.quizlive.AttemptDaoTest                     (3 tests  - PASSED)
[INFO] Running com.quizlive.AttemptModelTest                   (2 tests  - PASSED)
[INFO] Running com.quizlive.AuthServiceTest                    (6 tests  - PASSED)
[INFO] Running com.quizlive.AuthServletTest                    (11 tests - PASSED)
[INFO] Running com.quizlive.BrowserAndEndToEndVerificationTest (1 test   - PASSED)
[INFO] Running com.quizlive.CreatorRankAndQuizCodeTest         (15 tests - PASSED)
[INFO] Running com.quizlive.DbConnectionTest                   (2 tests  - PASSED)
[INFO] Running com.quizlive.EnumsAndExceptionsTest             (3 tests  - PASSED)
[INFO] Running com.quizlive.EnvConfigTest                      (5 tests  - PASSED)
[INFO] Running com.quizlive.JsonUtilAndFilterTest              (2 tests  - PASSED)
[INFO] Running com.quizlive.LeaderboardAndAttemptServletTest   (7 tests  - PASSED)
[INFO] Running com.quizlive.LeaderboardEndpointTest            (5 tests  - PASSED)
[INFO] Running com.quizlive.LeaderboardServiceTest             (2 tests  - PASSED)
[INFO] Running com.quizlive.MessageAndSettingsDaoTest          (2 tests  - PASSED)
[INFO] Running com.quizlive.PasswordUtilTest                   (3 tests  - PASSED)
[INFO] Running com.quizlive.QuizAndQuestionModelTest           (3 tests  - PASSED)
[INFO] Running com.quizlive.QuizAndScoringServiceTest          (3 tests  - PASSED)
[INFO] Running com.quizlive.QuizAttemptLifecycleServletTest    (2 tests  - PASSED)
[INFO] Running com.quizlive.QuizDaoTest                        (3 tests  - PASSED)
[INFO] Running com.quizlive.QuizManagementServletTest          (7 tests  - PASSED)
[INFO] Running com.quizlive.QuizSchedulerServiceTest           (2 tests  - PASSED)
[INFO] Running com.quizlive.UserDaoTest                        (4 tests  - PASSED)
[INFO] Running com.quizlive.UserHierarchyTest                  (2 tests  - PASSED)
[INFO] Running com.quizlive.WaitingRoomAndHostTest             (6 tests  - PASSED)
[INFO] 
[INFO] Results: Tests run: 111, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🚀 Quick Start & Installation Guide

### Prerequisites
- JDK 21 LTS installed (`java -version`)
- MySQL 8.0+ running locally OR Docker Desktop

### Local Execution (Maven Wrapper)
```bash
# 1. Clone & Setup Environment
git clone https://github.com/Amritesh-Singh-okay/QuizLive-.git
cd QuizLive-
cp .env.example .env

# 2. Run Test Suite (111 Tests)
.\mvnw.cmd test          # Windows
./mvnw test              # Linux / macOS

# 3. Launch Embedded Tomcat Server
.\mvnw.cmd compile exec:java   # Windows
./mvnw compile exec:java       # Linux / macOS
```
- Web Application: **`http://localhost:8080/quizlive`**
- Health Diagnostic: **`http://localhost:8080/quizlive/health`**

### Docker Deployment
```bash
docker compose up -d --build
```
Access at: **`http://localhost:8080`**

---

## 👥 Demo Credentials & Live Evaluator Script

All accounts share the default password: **`password123`**

| Role | Email Address | Password | Permissions & Dashboard |
|---|---|---|---|
| **System Administrator** | `admin@quizlive.com` | `password123` | Approves quizzes, manages users, system settings |
| **Quiz Creator** | `creator@quizlive.com` | `password123` | Authors quizzes, hosts live lobbies, tracks proctoring |
| **Student Participant 1** | `alice@quizlive.com` | `password123` | Joins waiting rooms, takes exams, views ranks |
| **Student Participant 2** | `bob@quizlive.com` | `password123` | Multi-user competition and live leaderboard tests |

### 5-Minute Evaluator Demonstration
1. **Admin Approval (OOP & RBAC):** Login as `admin@quizlive.com` $\rightarrow$ Approve pending quizzes in the approval queue.
2. **Creator Host & Waiting Room (WebSockets):** Login as `creator@quizlive.com` $\rightarrow$ Click **Host Live Quiz** on *"Core Java Fundamentals"* (Code: `JAVA101`).
3. **Student Joining & Proctoring (Anti-Cheat):** In another window, login as `alice@quizlive.com` $\rightarrow$ Enter code `JAVA101`. Watch the creator's student counter increment in real time. Creator clicks **Start Quiz** $\rightarrow$ Alice's screen transitions automatically. Switch tabs to test proctoring telemetry.
4. **Instant WebSocket Leaderboard (Concurrency):** Open `/leaderboard.jsp?quizId=1` in a separate tab. Submit Alice's exam and observe the live ranking update instantly without page reload.

---

## 📜 License

This project is licensed under the **MIT License**.
