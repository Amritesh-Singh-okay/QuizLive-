# QuizLive — Proctored Real-Time Online Assessment Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-10-blue.svg)](https://jakarta.ee/)
[![Tomcat](https://img.shields.io/badge/Apache%20Tomcat-10.1-yellow.svg)](https://tomcat.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/JUnit%205-89%20Passing-brightgreen.svg)]()
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)

**QuizLive** is a high-concurrency, real-time proctored online assessment platform built with pure modern Java (**Jakarta EE 10 / Servlet 6.0**, **WebSocket 2.1**, **HikariCP**, and **Embedded Apache Tomcat 10.1**).

Unlike traditional online quiz tools that rely on client-side JavaScript timers and expose answers in browser network traffic, QuizLive enforces **Zero-Trust Server-Authoritative Integrity**: countdown timers, grading, and anti-cheat telemetry are strictly governed on the backend.

---

## 📖 Complete Technical Specification

For an exhaustive technical breakdown of the architecture, cryptography, anti-cheat engineering, and benchmarks comparing QuizLive against traditional platforms, read the official whitepaper:

👉 **[Read the Full System Architecture & Security Specification](QUIZLIVE_PLATFORM_SPECIFICATION.md)**

---

## 🚀 Key Technical Highlights

- **Zero-Trust Server-Authoritative Timers:** Timers are enforced by background `ScheduledExecutorService` daemon threads. Late submissions are mathematically impossible, even if a user manipulates their client clock.
- **Client Answer Key Stripping (`Question.sanitized()`):** Answer keys are completely zeroed out before questions are sent to participants, preventing F12 DevTools network inspection cheating.
- **Page Visibility Anti-Cheat Proctoring:** Detects and logs browser tab-switches and window defocus events in real time. Flagged attempts are visible on educator dashboards.
- **WebSocket Real-Time Leaderboards:** Native `@ServerEndpoint` WebSocket broadcasts update all connected observers live without database polling.
- **Enterprise Password Security:** PBKDF2WithHmacSHA256 key derivation with 65,536 rounds, 128-bit random salts, and constant-time `MessageDigest.isEqual` comparison to eliminate timing attacks.
- **Strict RBAC & Privilege Escalation Immunity:** Polymorphic `AppUser` hierarchy with servlet filter interceptors. Self-registering as an `ADMIN` is strictly rejected at both HTTP and Service perimeters.
- **Ultra-Lightweight Micro-Footprint:** Built without Spring Boot bloat. Sub-second cold start and under 90MB RAM footprint.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language & Runtime** | Java 21 (Temurin LTS) |
| **Web & Servlet Engine** | Jakarta EE 10 (Servlet 6.0, WebSocket 2.1, JSP 3.0) on Embedded Tomcat 10.1 |
| **Connection Pooling** | HikariCP 5.1.0 |
| **Database** | MySQL 8.0 / TiDB Cloud Serverless (InnoDB) |
| **JSON Serialization** | Google Gson 2.10.1 |
| **Testing** | JUnit 5 (89 Unit & Integration Tests) |
| **Containerization** | Multi-Stage Docker (Temurin 21) |

---

## ⚡ Quick Start (Local Development)

### Prerequisites
- JDK 21 installed (`java -version`)
- MySQL 8.0 running locally

### 1. Clone & Configure
```bash
git clone https://github.com/Amritesh-Singh-okay/QuizLive-.git
cd QuizLive-
cp .env.example .env
```
Open `.env` and configure your local MySQL password.

### 2. Run Tests
```bash
# On Windows
.\mvnw.cmd test

# On Linux / macOS
./mvnw test
```

### 3. Launch the Application
```bash
# On Windows
.\mvnw.cmd compile exec:java

# On Linux / macOS
./mvnw compile exec:java
```
Access the application at: **`http://localhost:8080/quizlive`**  
Diagnostic health check: **`http://localhost:8080/quizlive/health`**

---

## 🐳 Docker Deployment

Run the complete full-stack environment (QuizLive + MySQL 8.0) with Docker Compose:

```bash
docker compose up -d --build
```
Access at: **`http://localhost:8080`**

---

## 👥 Demo Credentials (Local & Sandbox)

| Role | Email | Password |
|---|---|---|
| **Administrator** | `admin@quizlive.com` | `password123` |
| **Quiz Creator** | `creator@quizlive.com` | `password123` |
| **Participant** | `alice@quizlive.com` | `password123` |

---

## 📄 License
This project is licensed under the MIT License.
