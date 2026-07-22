# 🍽️ SchoolBite — School Canteen Management System

A full-stack web application for managing a school canteen. Students, parents, and teachers can order food online and get it delivered to their classroom. Canteen admins manage the menu, stock, and order fulfillment.

---

## 📋 Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | React 18 + Vite |
| **Backend** | Spring Boot 3 + Java 17 |
| **Database** | PostgreSQL |
| **Auth** | JWT (Access + Refresh tokens) |

---

## ⚙️ Prerequisites

Make sure your friend has these installed before running:

| Tool | Version | Download |
|---|---|---|
| **Java JDK** | 17 or higher | https://adoptium.net |
| **Node.js** | 18 or higher | https://nodejs.org |
| **Docker Desktop** | Latest | https://www.docker.com/products/docker-desktop *(easiest for PostgreSQL)* |
| **Git** | Any | https://git-scm.com |

> **No Docker?** Install PostgreSQL directly from https://www.postgresql.org/download/

---

## 🚀 Quick Start (3 Steps)

### Step 1 — Clone the project

```bash
git clone https://github.com/prat999tech/management_system_canteen_vendor.git
cd management_system_canteen_vendor
```

---

### Step 2 — Start the Database (PostgreSQL via Docker)

```bash
docker-compose up -d
```

This starts a PostgreSQL container on port **5433** with:
- Database: `canteen`
- Username: `canteen`
- Password: `canteen`

> **Check it's running:** `docker ps` — you should see a postgres container.

---

### Step 3 — Start Backend (Spring Boot)

Open a terminal in the `backend/` folder:

```bash
cd backend
./mvnw spring-boot:run
```

> **Windows users:** use `mvnw.cmd spring-boot:run`

Wait for this message:
```
Started CanteenApplication in X seconds
```

Backend runs at → **http://localhost:8080**

---

### Step 4 — Start Frontend (React)

Open a **new terminal** in the `frontend/` folder:

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at → **http://localhost:5173**

---

## 🌐 Open in Browser

Go to → **http://localhost:5173**

---

## 👤 Default Admin Accounts (Auto-Created)

| Role | Email | Password |
|---|---|---|
| **Canteen Admin** | canteenadmin@school.local | Admin@12345 |
| **School Admin** | schooladmin@school.local | Admin@12345 |

> Or click the **"Login as Canteen Admin"** button on the login page directly.

---

## 🗂️ Project Structure

```
school-canteen/
├── backend/              ← Spring Boot API (port 8080)
│   ├── src/
│   │   ├── main/java/    ← Java source code
│   │   └── resources/    ← Config files + DB migrations
│   └── pom.xml
│
├── frontend/             ← React + Vite UI (port 5173)
│   ├── src/
│   │   ├── pages/        ← All pages (Login, Menu, Wallet, etc.)
│   │   ├── components/   ← Layout, ProtectedRoute
│   │   ├── api/          ← Axios API calls
│   │   └── context/      ← Auth & Cart context
│   └── package.json
│
└── docker-compose.yml    ← PostgreSQL container config
```

---

## 🔄 How to Stop Everything

```bash
# Stop frontend: Ctrl+C in the frontend terminal
# Stop backend: Ctrl+C in the backend terminal
# Stop database:
docker-compose down
```

---

## ❓ Troubleshooting

| Problem | Fix |
|---|---|
| `Port 5433 already in use` | Change the port in `docker-compose.yml` and `backend/src/main/resources/application.yml` |
| `Port 8080 already in use` | Kill the process using port 8080 or change `server.port` in `application.yml` |
| `npm install` fails | Make sure Node.js 18+ is installed: `node --version` |
| Backend won't start | Make sure Docker/PostgreSQL is running first |
| `./mvnw: Permission denied` | Run `chmod +x backend/mvnw` then try again |

---

## 📱 Features

- ✅ Student / Parent / Teacher registration & login
- ✅ Browse daily menu with filters (Veg/Non-Veg, category)
- ✅ Add to cart & place orders with delivery slot selection
- ✅ Parent can link child accounts and order on their behalf
- ✅ Wallet top-up and balance management
- ✅ Real-time order tracking (Placed → Preparing → Delivered)
- ✅ Canteen admin: manage menu catalog, daily stock, order fulfillment
- ✅ Full form validation with error messages
