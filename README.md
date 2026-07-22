# 🍽️ SchoolBite — School Canteen Management System

A full-stack web application for managing a school canteen. Students, parents, and teachers can order food online and get it delivered to their classroom. Canteen admins manage the menu, stock, and order fulfillment.

---

## 📋 Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | React 18 + Vite + Nginx |
| **Backend** | Spring Boot 3 + Java 17 |
| **Database** | PostgreSQL |
| **Auth** | JWT (Access + Refresh tokens) |
| **Deployment** | Docker + Docker Compose |

---

## 🐳 Run with Docker (Easiest — Recommended)

> **Only requirement: [Docker Desktop](https://www.docker.com/products/docker-desktop) installed**

### Step 1 — Clone the project

```bash
git clone https://github.com/prat999tech/management_system_canteen_vendor.git
cd management_system_canteen_vendor
```

### Step 2 — Build and Start everything

```bash
docker-compose up --build
```

> ⏳ First run takes 3–5 minutes (downloads Java, Node, builds the app).  
> Subsequent runs are fast (cached layers).

### Step 3 — Open in Browser

```
http://localhost
```

That's it! 🎉

### Stop everything

```bash
docker-compose down
```

### Stop and delete all data (fresh start)

```bash
docker-compose down -v
```

---

## 👤 Default Admin Accounts (Auto-Created on First Start)

| Role | Email | Password |
|---|---|---|
| **Canteen Admin** | canteenadmin@school.local | Admin@12345 |
| **School Admin** | schooladmin@school.local | Admin@12345 |

> Or click the **"Login as Canteen Admin"** button on the login page directly.

---

## 🏗️ Docker Architecture

```
http://localhost (port 80)
        │
        ▼
┌─────────────────────┐
│  Nginx (Frontend)   │  ← Serves React app + proxies /api/* calls
└─────────┬───────────┘
          │  proxy /api/*
          ▼
┌─────────────────────┐
│  Spring Boot (8080) │  ← Backend API
└─────────┬───────────┘
          │  JDBC
          ▼
┌─────────────────────┐
│  PostgreSQL (5432)  │  ← Database (data persists in Docker volume)
└─────────────────────┘
```

---

## ⚙️ Run Without Docker (Manual Setup)

If you prefer running without Docker:

### Prerequisites

| Tool | Version | Download |
|---|---|---|
| **Java JDK** | 17+ | https://adoptium.net |
| **Node.js** | 18+ | https://nodejs.org |
| **PostgreSQL** | 15+ | https://www.postgresql.org/download |

### 1. Setup Database

Create a PostgreSQL database:
```sql
CREATE DATABASE canteen;
CREATE USER canteen WITH PASSWORD 'canteen';
GRANT ALL PRIVILEGES ON DATABASE canteen TO canteen;
```

### 2. Start Backend

```bash
cd backend
./mvnw spring-boot:run
# Windows: mvnw.cmd spring-boot:run
```

Wait for: `Started CanteenApplication in X seconds`  
Backend → **http://localhost:8080**

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend → **http://localhost:5173**

---

## 🗂️ Project Structure

```
school-canteen/
├── backend/                  ← Spring Boot API
│   ├── src/main/java/        ← Java source code
│   ├── src/main/resources/   ← Config + DB migrations (Flyway)
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                 ← React + Vite UI
│   ├── src/
│   │   ├── pages/            ← Login, Menu, Wallet, Orders, Admin pages
│   │   ├── components/       ← Layout, ProtectedRoute
│   │   ├── api/              ← Axios API calls
│   │   └── context/          ← Auth & Cart context
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
│
└── docker-compose.yml        ← Runs everything with one command
```

---

## ❓ Troubleshooting

| Problem | Fix |
|---|---|
| `Port 80 already in use` | Stop any local web server or change port in `docker-compose.yml` |
| `Port 8080 already in use` | Change the backend port in `docker-compose.yml` |
| Build fails on backend | Make sure Docker has enough memory (4GB+) in Docker Desktop settings |
| `./mvnw: Permission denied` | Run `chmod +x backend/mvnw` |
| Data not saving | Make sure the `canteen_pgdata` volume exists (`docker volume ls`) |

---

## 📱 Features

- ✅ Student / Parent / Teacher registration & login
- ✅ Browse daily menu with filters (Veg/Non-Veg, category, date)
- ✅ Add to cart & place orders with delivery slot selection
- ✅ Parent can link child accounts and order on their behalf
- ✅ Wallet top-up and balance management
- ✅ Real-time order tracking (Placed → Preparing → Packed → Delivered)
- ✅ Canteen admin: manage menu catalog, daily stock, order fulfillment board
- ✅ Full form validation with error messages
- ✅ Responsive design — works on mobile and desktop
