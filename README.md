<h1 align="center">🌐 HazardIQ+ – Smart Hazard Detection & Response App</h1>
<p align="center">
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/app/src/main/ic_launcher-playstore.png" alt="HazardIQ+ Logo" width="22%" style="border-radius: 50%;">
</p>

---

## 📌 Problem Statement
In emergency situations, timely information and quick response can save lives. However:
- People lack a **centralized platform** for hazard detection and alerts.
- Communication during emergencies is **fragmented**.
- No integrated solution combining **SOS, hazard detection, and medical advice** exists.

---

## 💡 Solution
HazardIQ+ bridges the gap by providing:
- Real-time hazard alerts and SOS communication.
- AI-powered medical recommender for quick health insights.
- Community chat and instant chatbot help.
- Role-based system for **Citizens** and **Responders** to collaborate effectively. 

---

## ✨ Features

### 1. **User Authentication & Role Management**
- **Email/Password Login & Signup** – Secure Firebase authentication for account creation and login.
- **Google Sign-In** – Seamless login using Google accounts with an account selector shown every time.
- **Role Selection (Citizen / Responder)** – Choose your role during signup; stored locally using SharedPreferences for instant retrieval.
- **Persistent Session** – Users stay logged in until they explicitly log out.

#### 🧑‍🤝‍🧑 Roles in the App
- **Citizen** – Receives hazard alerts, sends SOS requests, participates in community discussions.
- **Responder** – Responds to SOS requests, coordinates in hazard situations, and provides assistance in emergencies.

---

### 2. **Profile Management**
- View your **name, email, UID, and role** fetched securely from the backend via Retrofit.
- Logout button for secure sign-out.
- Floating Action Button (FAB) for quick profile access.

---

### 3. **Weather & Air Quality Dashboard**
- **Real-time Weather Data** – Shows temperature, humidity, and weather conditions.
- **Air Quality Index (AQI)** – Live AQI readings for your location.
- **Mapbox Integration** – Visualizes hazard and AQI zones.
- **India Map Boundaries** – Restricts interaction to Indian boundaries for relevance.

---

### 4. **Hazard Alerts**
- **Live Hazard Detection** – Displays hazards near the user in real-time.
- **Color-Coded Indicators** – Easy hazard differentiation by colors.
- **Interactive Map Markers** – Tap to view hazard details.

---

### 5. **Emergency SOS**
- One-tap SOS request sending.
- Real-time status tracking – shows if a responder has acknowledged.
- Background polling for updates.
- Displays responder details upon acceptance.

---

### 6. **Medical Recommender (AI-Powered)**
- **50 Symptom Chips** – Select symptoms you’re experiencing.
- **AI Prediction** – Hugging Face ML model predicts the most probable disease.
- **Detailed Report**:
  - Disease Name
  - Description
  - Recommended Diet
  - Medications
  - Precautions
  - Suggested Workouts
- User-friendly UI with chip-based symptom selection.

---

### 7. **Community & SOS Chat (Socket.io)**
- **Community Hazard Chat** – Discuss ongoing hazards with nearby citizens and responders.
- **SOS Private Chat** – Direct real-time communication between SOS sender and assigned responder.
- Powered by **Socket.io** for low-latency messaging.

---

### 8. **AI-Based Hazard Detection from Images**
- Click or upload a photo of a hazard.
- ML model processes the image and classifies the hazard type.
- Automatically tags the hazard on the map and notifies relevant users.

---

### 9. **Google Maps & Mapbox Visualizations**
- Interactive hazard, SOS, and AQI markers.
- Region-restricted map navigation.
- Tap markers for quick info popups.

---

### 10. **Modern & Responsive UI**
- Material Design 3 components for a clean, professional look.
- Gradient buttons for primary actions.
- Rounded cards and chips for better visual appeal.
- Fully responsive layouts supporting multiple screen sizes.

---

### 11. **Offline Handling & Error Management**
- Toast notifications for network issues.
- Fallback UI when data is unavailable.
- Retry options for failed requests.

---

### 12. **Security & Performance**
- Firebase Authentication for secure access.
- Role-based access for feature segregation.
- Retrofit with caching for faster loading.
- SharedPreferences for quick role/profile retrieval.

---

## 🛠 Tech Stack

| **Category** | **Technologies** |
|--------------|------------------|
| **Frontend** | Android (Kotlin), Material Design 3 |
| **Backend**  | Node.js, Express.js, PostgreSQL/Neon, FirebaseAuth, Firebase Cloud Notifications |
| **APIs**     | OpenWeather, AQI APIs, Mapbox, Hugging Face, Fast API |
| **AI**       | TfLite, Custom ML model hosted on Hugging Face Spaces |
| **Other**    | Retrofit, WebSocket, Work Manager, Geofencing, Geocoding & Reverse Geocoding, Shared Preferences |

---

## 📲 Screenshots

<p align="center">
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(11).png" width="22%" />
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(12).png" width="22%" />
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(10).png" width="22%" />
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(3).png" width="22%" />
</p>
<p align="center">
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(5).png" width="22%" />
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(6).png" width="22%" />
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(7).png" width="22%" />
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(8).png" width="22%" />
</p>
<p align="center">
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(4).png" width="22%" />
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(13).png" width="22%" />
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(1).png" width="22%" />
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(2).png" width="22%" />
</p>
<p align="center">
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(9).png" width="22%" />
  <img src="https://github.com/The-Breakroom-Carousal/HazardIQ/blob/android/screenshots/screenshot%20(14).png" width="22%" />
</p>


---

## 🚀 Getting Started

### Prerequisites
- Android Studio Giraffe+  
- JDK 17+  
- Node.js 18+  
- MongoDB  
- Firebase Project with Auth enabled  

### Setup
```bash
# Clone the repo
git clone https://github.com/yourusername/HazardIQ-Plus.git

# Open in Android Studio

# Add your keys in local.properties
MAPBOX_ACCESS_TOKEN=your_mapbox_token
FIREBASE_CONFIG=your_firebase_config

# Backend Setup
cd backend
npm install
node server.js
