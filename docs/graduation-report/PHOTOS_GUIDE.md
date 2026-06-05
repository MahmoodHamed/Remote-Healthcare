# Photo & Figure Guide for Graduation Report

Place all images in the `figures/` folder using the **exact filenames** listed below.
Recommended format: **PNG** or **JPG**, minimum width **1200 px** for screenshots.

---

## Title Page (2 images)

| Filename | What to capture |
|----------|-----------------|
| `university-logo.png` | University of Baghdad official logo |
| `college-logo.png` | Al-Khwarizmi College of Engineering logo |

---

## Chapter 1 — Introduction (1 image)

| Filename | What to capture |
|----------|-----------------|
| `project-overview.png` | A wide photo of your full setup: Samsung Galaxy Watch on wrist, phone showing the app, and laptop with the web dashboard — all running at the same time |

---

## Chapter 4 — System Design (3 images)

| Filename | What to capture |
|----------|-----------------|
| `system-architecture.png` | Block diagram showing: Watch → MQTT → Backend → Database → Web/Mobile clients. You can draw this in Draw.io, PowerPoint, or Lucidchart |
| `data-flow.png` | Flowchart: Sensor reading → Watch app → MQTT publish → Backend ingest → PostgreSQL → SignalR broadcast → Dashboard update |
| `use-case-diagram.png` | UML use-case diagram with actors: Patient, Doctor, Relative, Admin |

---

## Chapter 5 — Implementation (6 images)

| Filename | What to capture |
|----------|-----------------|
| `watch-heart-rate.png` | Screenshot of the Wear OS watch app while measuring heart rate (HR tab selected, bpm value visible, Start/Stop button) |
| `watch-spo2-temp.png` | Screenshot of watch app on SpO₂ or Temperature tab with a live reading |
| `android-login.png` | Screenshot of the Android mobile app login or registration screen |
| `android-vitals.png` | Screenshot of the Android app showing a patient's live vitals or patient list |
| `web-dashboard.png` | Screenshot of the web dashboard with multiple vital tiles (HR, SpO₂, Temp, Fall Detection) |
| `web-patient-monitor.png` | Screenshot of the web Patient Monitor page with real-time SignalR updates |

---

## Chapter 6 — Testing & Results (4 images)

| Filename | What to capture |
|----------|-----------------|
| `alert-notification.png` | Screenshot of a push notification or in-app alert (e.g. "Low SpO₂" or "Fall detected") on phone or dashboard |
| `swagger-api.png` | Screenshot of Swagger UI (`/swagger`) showing the RPM API endpoints |
| `docker-services.png` | Screenshot of `docker compose ps` or Docker Desktop showing running containers (postgres, redis, mosquitto, api) |
| `testing-setup.png` | Photo of your testing environment: watch on wrist, MQTT broker running, dashboard showing live data |

---

## Optional but Recommended (3 images)

| Filename | What to capture |
|----------|-----------------|
| `hardware-watch.png` | Clear product photo of the Samsung Galaxy Watch 8 |
| `database-er.png` | ER diagram of main tables: User, PatientProfile, VitalRecord, Alert, Device |
| `team-photo.png` | Group photo with supervisors and students |

---

## Quick checklist

- [ ] `university-logo.png`
- [ ] `college-logo.png`
- [ ] `project-overview.png`
- [ ] `system-architecture.png`
- [ ] `data-flow.png`
- [ ] `use-case-diagram.png`
- [ ] `watch-heart-rate.png`
- [ ] `watch-spo2-temp.png`
- [ ] `android-login.png`
- [ ] `android-vitals.png`
- [ ] `web-dashboard.png`
- [ ] `web-patient-monitor.png`
- [ ] `alert-notification.png`
- [ ] `swagger-api.png`
- [ ] `docker-services.png`
- [ ] `testing-setup.png`

---

## How to compile the report

```bash
cd docs/graduation-report
pdflatex main.tex
pdflatex main.tex   # run twice for references/TOC
```

Or use **Overleaf**: upload `main.tex` and the `figures/` folder.

Missing images show a gray placeholder box — replace them one by one as you capture screenshots.
