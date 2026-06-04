# AksharaNexus
### *Where Ancient Knowledge Finds Its Source*

AksharaNexus is a **Civilization Memory Tree Engine** — an industry-grade platform for preserving, verifying, and querying historical knowledge about ancient civilizations. Unlike open platforms such as Wikipedia where anyone can edit content, AksharaNexus enforces a strict university-backed editorial and peer-review pipeline to ensure every piece of historical data is credible, sourced, and authenticated before it reaches the public.

The platform consists of three portals serving distinct roles — university scholars who contribute content, appointed reviewers who verify and publish it, and a general public interface powered by an AI Oracle that answers natural language queries using only verified records.

---

## Live Portals

| Portal | URL | Audience |
|--------|-----|----------|
| University Portal | [contribute-aksharanexus.netlify.app](https://contribute-aksharanexus.netlify.app) | University Admins & Editors |
| Reviewer Portal | [reviewer-aksharanexus.netlify.app](https://reviewer-aksharanexus.netlify.app) | Appointed Reviewers |
| AI Oracle | [aksharaoracle.netlify.app](https://aksharaoracle.netlify.app) | General Public |

---

## How It Works

### 1. University Module
Registered university scholars (verified via institutional email domains) contribute historical data about civilizations. Content is structured as a **Civilization → Volume → Entry** hierarchy and stored using a custom persistent N-ary tree data structure with full version history, SHA-256 commit hashing, and rollback support — similar in concept to Git but purpose-built for historical knowledge trees.

- Only **Admins** can create civilizations, assign editors, and submit versions for review
- Only **Editors** can add and update volumes and entries within their assigned civilizations
- Every change produces a new immutable version — no data is ever overwritten

### 2. Reviewer Module
Reviewers are appointed by Super Admins via invite codes. They browse all university submissions, review individual entries, and publish approved content into the **Central Registry** — the single source of verified historical truth on the platform.

- Entries can be marked **Approved**, **Rejected**, or **Revision Requested**
- Only approved entries can be published to the Central Registry
- Reviewers can flag **divergences** when two universities provide conflicting accounts of the same historical topic

### 3. AI Oracle
The public-facing AI Oracle allows anyone to query the Central Registry using natural language. Queries are processed through a multi-stage pipeline:

1. **Intent Extraction** — civilization name resolved via 4-strategy cascade (n-gram DB lookup → proper noun detection → token matching → fuzzy Levenshtein)
2. **Query Classification** — CONVERSATIONAL / META / OFF_TOPIC / VAGUE_HISTORICAL / RESEARCH
3. **Central Registry Search** — fetches and parses verified entries by civilization and time range
4. **Block Ranking** — scores content blocks by keyword frequency, title relevance, and topic match
5. **LLM Response** — context injected into Groq LLaMA 3.3 70B for a cited, scholarly answer

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot (Java), Maven |
| Security | Spring Security, JWT (3 separate filter chains) |
| ORM | Hibernate / JPA |
| Database | PostgreSQL (Neon) |
| Data Structure | PersistentTree (custom Java library) |
| AI / LLM | Groq API — LLaMA 3.3 70B |
| University Seeding | ROR API (ror.org) |
| Frontend | HTML5, Vanilla CSS, REST APIs |
| Backend Hosting | Railway |
| Frontend Hosting | Netlify (3 deployments) |
| Hashing | SHA-256 (version commits) |
| Password Security | BCrypt |
| Serialization | Jackson |

---

## Environment Variables

The backend requires the following environment variables to be set (via Railway or a local `.env`):

```env
# Database (Neon PostgreSQL)
DATABASE_URL=your_neon_connection_string
DATABASE_USERNAME=your_db_username
DATABASE_PASSWORD=your_db_password

# JWT — University users
JWT_SECRET=your_jwt_secret
JWT_EXPIRATION=86400000

# JWT — Reviewers
REVIEWER_JWT_SECRET=your_reviewer_jwt_secret
REVIEWER_JWT_EXPIRY_MS=86400000

# Groq API
GROQ_API_KEY=your_groq_api_key

# Server (Railway injects this automatically)
PORT=8080
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                     Netlify                         │
│  University Portal  │  Reviewer Portal  │  Oracle   │
└────────────┬────────┴─────────┬─────────┴─────┬─────┘
             │                  │               │
             └──────────────────▼───────────────┘
                         Railway
                    Spring Boot Backend
                    ┌──────────────────┐
                    │ University Module│
                    │ Reviewer Module  │
                    │ AI Oracle Module │
                    └────────┬─────────┘
                             │
               ┌─────────────┼─────────────┐
               ▼             ▼             ▼
          Neon DB        Groq API       ROR API
        PostgreSQL     LLaMA 3.3 70B  (seed only)
```

---

## Key Design Decisions

**Persistent Tree for Version Control**
Civilization content is stored as a serialized persistent N-ary tree. Every mutation (add, update, rollback) produces a new immutable version via path-copying — only the modified path is recreated, all unmodified subtrees are shared by reference. This gives the platform Git-like version history with O(depth) overhead per commit.

**Three Separate JWT Filter Chains**
University users, Reviewers, and Public users each have entirely separate authentication contexts, JWT secrets, and security filter chains — ensuring no role can access another portal's endpoints.

**Central Registry as Ground Truth**
The Central Registry is not editable directly. It is populated exclusively from reviewer-approved entries sourced from university submissions, with full provenance tracking (source university, source version, source node ID).

**Divergence Flagging**
When two universities provide conflicting historical accounts of the same topic, reviewers can flag the divergence explicitly. Both entries remain in the registry with the conflict documented — preserving academic disagreement rather than arbitrarily resolving it.

---

## Modules

```
com.example.nexusa
├── AI/Oracle          — Oracle pipeline, LLM service, intent extraction
├── University         — Auth, civilization management, version control
├── Reviewer           — Review workflow, central registry management
└── Model              — JPA entities, enums, value objects
```

---

## Access Control Summary

| Role | Portal | Appointed By |
|------|--------|-------------|
| Super Admin | University | System |
| University Admin | University | Super Admin |
| University Editor | University | University Admin |
| Reviewer | Reviewer | Super Admin (invite code) |
| Public User | AI Oracle | Self-registered |

---

*AksharaNexus is a proprietary industry project. All rights reserved.*
