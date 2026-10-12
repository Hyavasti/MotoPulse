## 📌 Overview

**MotoPulse** is built for everyday riders and riding clubs to take the guesswork out of motorcycle upkeep while staying connected on the road. Rather than relying on paper service manuals or memory, MotoPulse tracks maintenance intervals, records fuel stops, and allows groups to coordinate rides via direct and group chat channels.

---

## ✨ Key Features

### 🛠️ Maintenance & Service Tracking
* **Interval Reminders:** Mileage-based countdowns and alerts for engine oil, gear oil, coolant, CVT belt/chain, brake pads, and tires.
* **Service Logs:** Comprehensive maintenance record history to track replacement dates, parts costs, and odometer readings.

### ⛽ Fuel & Expense Ledger
* **Fuel Logging:** Track liters filled, total cost, and mileage per fill-up.
* **Consumption Analytics:** Calculate running cost per kilometer and monitor vehicle fuel efficiency over time.

### 🧭 Social Rides & Convoys
* **Group Ride Planning:** Create group rides, set destination waypoints, and invite companions.
* **Live Coordination:** Keep the pack aligned with location-aware route markers and meet points.

### 💬 In-App Messaging
* **1-on-1 Direct Chats:** Coordinate privately with riding partners.
* **Group Chat Channels:** Dedicated chat spaces for riding clubs and active convoy groups powered by real-time sync.

---

## 🛠️ Tech Stack & Architecture

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
* **Asynchronous Flow:** Kotlin Coroutines & Flow
* **Backend & Auth:** Supabase (Auth, PostgreSQL, Realtime)
* **Services & Cloud:** Firebase (Cloud Messaging / Realtime services)
* **Location & Maps:** Google Play Services Location & Map SDK
