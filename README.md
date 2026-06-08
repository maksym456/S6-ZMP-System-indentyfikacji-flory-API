<div align="center">

# 🌿 e-zielnik &middot; REST API

**Backend for a digital plant herbarium with photo-based species identification**

[![CI](https://github.com/maksym456/S6-ZMP-System-indentyfikacji-flory-API/actions/workflows/ci.yml/badge.svg)](https://github.com/maksym456/S6-ZMP-System-indentyfikacji-flory-API/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-database-4169E1.svg?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Coverage](https://img.shields.io/badge/coverage-%E2%89%A580%25-brightgreen.svg)](#-tests--quality)

</div>

---

## 📖 Table of contents

- [About](#-about)
- [Features](#-features)
- [Tech stack](#-tech-stack)
- [Quick start](#-quick-start)
- [Configuration](#-configuration)
- [API documentation](#-api-documentation)
- [Architecture](#-architecture)
- [Tests & quality](#-tests--quality)
- [Deployment](#-deployment)
- [Project structure](#-project-structure)
- [Requirements matrix (OPZ)](#-requirements-matrix-opz)
- [Team](#-team)

---

## 🌱 About

**e-zielnik** ("e-herbarium") is a flora identification system for keeping a digital herbarium of plants.
This repository contains the project's **central REST API**, consumed by three client applications
(mobile, web and desktop). The backend is written in **Java + Spring Boot** with a **PostgreSQL** database.

A user photographs a plant, the system recognizes its species through the external **PlantNet**
service, and then lets the user collect plants into named herbaria, describe them, share them
publicly or with friends, and receive notifications. An administrative section provides statistics
and moderation tools.

> 📄 **Full technical specification** (functional description, data model, complete endpoint
> catalog, security): [`docs/specyfikacja-api-ezielnik.md`](docs/specyfikacja-api-ezielnik.md)

| Metric | Value |
|---|---|
| Production Java code | ~6,600 lines across 87 files |
| Test code | ~3,900 lines across 22 files |
| Domain packages / entities | 10 modules / 9 entities |
| REST controllers | 8 (dozens of endpoints) |
| Test coverage threshold | ≥ 80% (JaCoCo, enforced in CI) |

---

## ✨ Features

- 🔐 **Authentication & accounts** - registration, login (by email or username), email
  verification, password reset, optional **email 2FA**.
- 🪪 **Secure sessions** - **JWT** access tokens (HS512) plus opaque **refresh tokens**
  with rotation and reuse detection.
- 📷 **Plant identification** - species recognition from a photo via **PlantNet**, with a
  two-step confirmation (attach to an existing plant or create a new one).
- 📚 **Herbaria & plants** - full CRUD for herbaria (private and public), management of plants,
  photos and taxonomic data.
- 🖼️ **Photo storage** - conversion to JPEG, cached serving, path-traversal protection.
- 👥 **Social features** - friends, sharing private herbaria with friends.
- 🔔 **Notifications** - in-app plus push via **Firebase Cloud Messaging**.
- 🛡️ **Admin panel** - statistics, account blocking, warnings, content moderation.
- 📊 **Operational security** - rate limiting (Bucket4j), account-status checks, input validation.
- 📜 **Documentation** - auto-generated OpenAPI / Swagger UI.

---

## 🧰 Tech stack

| Layer | Technologies |
|---|---|
| **Language / runtime** | Java 21 (LTS) |
| **Framework** | Spring Boot 4.0.6 (Web MVC, Data JPA, Security, Validation) |
| **Database** | PostgreSQL + Hibernate |
| **Security** | OAuth2 Resource Server, JJWT 0.13.0, BCrypt, Bucket4j 8.17.0 + Caffeine 3.2.4 |
| **Integrations** | PlantNet (identification), Brevo (email), Firebase Admin SDK 9.9.0 (push) |
| **Image / files** | TwelveMonkeys imageio-webp 3.12.0 |
| **Documentation** | springdoc-openapi 3.0.2 (Swagger UI) |
| **Testing** | JUnit 5, Mockito, Testcontainers 1.21.4, JaCoCo 0.8.14 |
| **Build / CI** | Maven (wrapper), Docker (multi-stage), GitHub Actions |

---

## 🚀 Quick start

The fastest path to a running app is **Docker Compose** (starts the API together with a PostgreSQL database).

```bash
# 1. Clone the repository
git clone https://github.com/maksym456/S6-ZMP-System-indentyfikacji-flory-API.git
cd S6-ZMP-System-indentyfikacji-flory-API

# 2. Create the .env.properties file (see the Configuration section)

# 3. Build and run everything
docker compose up --build
```

The API will be available at **http://localhost:8080**, and the interactive documentation at
**http://localhost:8080/swagger-ui.html**.

<details>
<summary><strong>🛠️ Local run (without Docker)</strong></summary>

**Requirements:** JDK 21 (the [Eclipse Temurin](https://adoptium.net/) distribution is recommended)
and a running PostgreSQL instance. Maven is not required globally - the repository ships with a wrapper (`./mvnw`).

```bash
# 1. Start PostgreSQL listening on localhost:5432
# 2. Fill in the .env.properties file (DB_URL, JWT_SECRET, PLANTNET_API_KEY, ...)
# 3. Run the application:
./mvnw spring-boot:run

# or build the artifact and run it manually:
./mvnw clean package
java -jar target/S6-ZMP-System-indentyfikacji-flory-API-1.0.jar
```

The database schema is created automatically by Hibernate (`ddl-auto=update`) - there are no manual migrations.

</details>

---

## ⚙️ Configuration

The application loads its configuration from a `.env.properties` file in the project root (optional import).

```properties
# Database
DB_URL=jdbc:postgresql://localhost:5432/
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Security (REQUIRED - set your own long, random secret)
JWT_SECRET=<long-random-base64-secret>
JWT_EXPIRATION_MS=604800000
JWT_REFRESH_EXPIRATION_DAYS=30

# Integrations
PLANTNET_API_KEY=<plantnet-key>
BREVO_API_KEY=<brevo-key-optional>
# FIREBASE_SERVICE_ACCOUNT_JSON=<service-account-json-optional>

# Application
APP_BASE_URL=http://localhost:8080
MAIL_FROM=ezielnik.app@gmail.com
PHOTO_STORAGE_PATH=./photos
PORT=8080
```

| Variable | Description | Default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection | - |
| `JWT_SECRET` | Secret used to sign JWTs **(required)** | - |
| `JWT_EXPIRATION_MS` | Access token lifetime | `7200000` (2 h) |
| `JWT_REFRESH_EXPIRATION_DAYS` | Refresh token lifetime | `30` days |
| `PLANTNET_API_KEY` | Plant identification service API key | - |
| `BREVO_API_KEY` | Transactional email API key (optional) | - |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase service account for push (optional) | - |
| `PHOTO_STORAGE_PATH` | Photo storage directory | `./photos` |
| `APP_BASE_URL` | Public application URL (used in email links) | `http://localhost:8080` |
| `PORT` | Server listening port | `8080` |

> ⚠️ In production, **always** override `JWT_SECRET` and the API keys with environment variables.
> Never commit real secrets to the repository.

---

## 📚 API documentation

Once the application is running, interactive documentation is available:

- **Swagger UI:** `<APP_BASE_URL>/swagger-ui.html`
- **OpenAPI (JSON):** `<APP_BASE_URL>/v3/api-docs`

Communication uses **JSON** over **HTTP**, with stateless authentication via a **JWT**
(`Authorization: Bearer <token>` header).

<details>
<summary><strong>Main endpoint groups</strong></summary>

| Prefix | Controller | Scope |
|---|---|---|
| `/users` | UserController | Registration, login, 2FA, profile, password reset, FCM tokens |
| `/herbaria` | HerbariumController | Herbarium CRUD, public/private visibility |
| `/herbaria/{id}/plants` | PlantController | Identification, plants and photos |
| `/photos` | PhotoController | Serving photo files |
| `/friends` | FriendshipController | Friend requests and friends |
| `/notifications` | NotificationController | In-app notifications |
| `/stats`, `/users` | Admin controllers | Statistics and moderation |

The full catalog of all endpoints with access levels is available in the
[technical specification](docs/specyfikacja-api-ezielnik.md).

</details>

---

## 🏗️ Architecture

A classic Spring Boot layered architecture, split into domain packages:

```
Controller (HTTP, JWT)  ->  Service (logic, transactions)  ->  Repository (JPA)  ->  Entity (table)
                                       |
                            DTO (*Request / *Response)
```

- **Central data model:** `User 1:N Herbarium 1:N Plant 1:N PlantPhoto`, plus helper entities
  (`Friendship`, `Notification`, `RefreshToken`, `TwoFactorCode`, `DeviceToken`).
- **Security:** BCrypt, JWT (HS512) with token-type separation (`purpose`), refresh token
  rotation, per-IP rate limiting, object-level resource protection.

Details (data model, security, full description): [technical specification](docs/specyfikacja-api-ezielnik.md).

---

## ✅ Tests & quality

A two-tier testing strategy:

- **Integration tests** (suffix `IT`, 14 files) - a full Spring Boot context against a real
  PostgreSQL instance via **Testcontainers**, with external services mocked (`@MockitoBean`).
- **Unit tests** (suffix `Test`, 4 files) - pure JUnit 5 + Mockito.

```bash
./mvnw verify          # compile, test and verify coverage
```

Coverage is measured by the **JaCoCo** plugin with an **80% instruction** threshold
(report: `target/site/jacoco/`). Every `push` and `pull request` to the `master` branch triggers
a **GitHub Actions** pipeline that builds the project, runs the tests and enforces the coverage threshold.

---

## ☁️ Deployment

The application is containerized (`Dockerfile`, multi-stage build) and deployed on **Railway**.

1. Railway builds the image from the `Dockerfile`.
2. Attach a PostgreSQL service (it provides `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`); the listening
   port is injected via the `PORT` variable.
3. In the Railway dashboard, set the production environment variables (`JWT_SECRET`,
   `PLANTNET_API_KEY`, optionally `BREVO_API_KEY`, `FIREBASE_SERVICE_ACCOUNT_JSON`, `APP_BASE_URL`).
4. **Important for photos:** set `PHOTO_STORAGE_PATH=/data/photos` and attach a **Railway Volume**
   mounted at `/data/photos`. The default `./photos` directory is ephemeral and loses files on
   every redeploy.

> 🌐 Live web frontend of the system (sister application):
> [s6-zmp-system-indentyfikacji-flory-snowy.vercel.app](https://s6-zmp-system-indentyfikacji-flory-snowy.vercel.app)

---

## 📂 Project structure

```
com.ezielnik.api
├── admin            admin panel
│   ├── content_management   content moderation (herbaria, plants, photos)
│   └── user_management      account moderation
├── auth             authentication
│   ├── refresh_token        refresh token rotation
│   └── two_factor_auth      two-factor authentication (2FA)
├── config           security, rate limiting, error handling
├── fcm              push notifications (Firebase Cloud Messaging)
├── friend           friends
├── herbarium        herbaria
├── notification     in-app notifications
├── photo            photo storage and serving
├── plant            plants and identification (PlantNet)
├── user             user accounts (and authentication endpoints)
└── ApiApplication   entry point
```

---

## 📋 Requirements matrix (OPZ)

The full functional scope of the entire system and its split across client modules.
Legend: **✓** - implemented in that module, **\*** - backed by the API (backend).

| OPZ | Functionality | API | web | mobile | desktop |
|---|---|:---:|:---:|:---:|:---:|
| FLY-01 | First administrator added to the system | ✓ | | | |
| FLY-02 | Administrator can log in | \* | | | ✓ |
| FLY-03 | Administrator can view all users' herbaria | \* | | | ✓ |
| FLY-04 | Administrator can delete users' herbaria | \* | | | ✓ |
| FLY-05 | Administrator can delete individual items/photos from herbaria | \* | | | ✓ |
| FLY-06 | Administrator can delete and block users | \* | | | ✓ |
| FLY-07 | Administrator can send warnings and push notifications | \* | | | ✓ |
| FLY-08 | Administrator sees system statistics | \* | | | ✓ |
| FLY-09 | Guest can browse public herbaria | \* | ✓ | | |
| FLY-10 | Guest can register | \* | ✓ | ✓ | |
| FLY-11 | Guest can log in | \* | ✓ | ✓ | |
| FLY-12 | Guest can reset their password | \* | ✓ | ✓ | |
| FLY-13 | User can add their own herbaria | \* | | ✓ | |
| FLY-14 | User can add photos and detect the plant | \* | | ✓ | |
| FLY-15 | User can add descriptions to their plants | \* | | ✓ | |
| FLY-16 | User sees a list of their herbaria and plants | \* | | ✓ | |
| FLY-17 | User can mark herbaria as public | \* | | ✓ | |
| FLY-18 | User can add others to their friends list | \* | ✓ | | |
| FLY-19 | User can see friends' and public herbaria | \* | ✓ | | |
| FLY-20 | User can view and compare friends' herbaria | \* | ✓ | | |
| FLY-21 | User can log out | \* | ✓ | ✓ | |
| FLY-22 | System supports Polish and English | \* | ✓ | ✓ | ✓ |

---

## 👥 Team

Team project carried out for the **Team Programming Methods (ZMP)** course, semester VI.

| Module | Technologies | Author | Repository |
|---|---|---|---|
| **REST API** | Java, Spring Boot, Maven, PostgreSQL | Maksym Wilk (43900) | [📦 API](https://github.com/maksym456/S6-ZMP-System-indentyfikacji-flory-API) |
| Mobile app | Dart, Flutter | Adam Rudziewicz (43882) | [📱 Mobile](https://github.com/adamrudziewicz/S6-ZMP-System-indentyfikacji-flory-mobile) |
| Web app | TypeScript, React, Vite | Szymon Rogula (43880) | [🌐 Web](https://github.com/SX2V/S6-ZMP-System-indentyfikacji-flory-Web) |
| Desktop app | C#, .NET | Sebastian Waga (43894) | [💻 Desktop](https://github.com/sebastianwaga/S6-ZMP-System-indentyfikacji-flory-Desktop) |

---

<div align="center">
<sub>Flora identification system (e-zielnik) &middot; REST API backend</sub>
</div>
