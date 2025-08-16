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

# ✨ HAZARDIq+ – Features

> ⚡ **All real-time data, chat, and notifications are powered by our own secure Node.js backend**, with **Socket.io** for messaging and **custom ML integrations**.  
> 📢 **Notifications** are sent directly from our server — no reliance on third-party push providers for core alerts.

---

## **1. User Authentication & Role Management**
- 🔐 **Email/Password Login & Signup** – Secure Firebase authentication.
- 🌐 **Google Sign-In** – Seamless login with account selector every time.
- 👥 **🧑‍🤝‍🧑 Roles in the App** :
-  Citizen – Receives hazard alerts, sends SOS requests, participates in community discussions.
- Responder – Responds to SOS requests, coordinates in hazard situations, and provides assistance in emergencies.
- ♾ **Persistent Session** – Stay logged in until you sign out manually.

---

## **2. Profile Management**
- 👤 View your **name, email, UID, and role** fetched securely via Retrofit.
- 🚪 One-tap logout for secure sign-out.
- ⚡ Floating Action Button (FAB) for quick profile access.

---

## **3. Weather & Air Quality Dashboard**
- 🌦 **Real-Time Weather Data** – Temperature, humidity, and current conditions.
- 🌫 **Air Quality Index (AQI)** – Live AQI readings for your location.
- 🗺 **Mapbox Integration** – Hazard & AQI zones visualized interactively.
- 🇮🇳 **India Map Boundaries** – Keeps the focus relevant to Indian regions.

---

## **4. Hazard Alerts**
- 📡 **Live Hazard Detection** – Hazards near you in real-time.
- 🎨 **Color-Coded Indicators** – Quick visual differentiation of hazards.
- 📍 **Interactive Map Markers** – Tap for hazard details.
- 🚷 **Hazard Entry Warning** – If you step inside a hazard zone, you’ll get an **instant notification** from our backend advising you not to enter.

---

## **5. Emergency SOS**
- 🚨 **One-Tap SOS Sending** – Immediate request to nearby responders.
- 📊 **Status Tracking** – See when a responder accepts your request.
- 🔄 **Background Polling** – Keeps you updated even if you switch screens.
- 👨‍🚒 Displays **responder details** once accepted.
- 💬 **Live Chat with Responder** – After SOS acceptance, chat directly with your responder in real-time (**Socket.io**, hosted on our Node.js backend).

---

## **6. Medical Recommender (AI-Powered)**
- 💊 **50 Symptom Chips** – Select symptoms from a visually appealing chip list.
- 🤖 **ML Model (Hugging Face)** – Predicts the most probable disease.
- 📄 **Detailed Report**:
  - Disease Name
  - Description
  - Recommended Diet
  - Medications
  - Precautions
  - Suggested Workouts

---

## **7. Community & SOS Chat**
- 💬 **Community Hazard Chat** – Room-based discussions for each hazard so **citizens in that hazard area can interact** in real time.
- 🔒 **Private SOS Chat** – One-on-one messaging between SOS sender & responder.
- ⚡ Powered by **Socket.io** on our **Node.js backend** for low-latency communication.

---

## **8. AI-Based Hazard Detection from Images**
- 📷 **Capture or Upload** hazard photos.
- 🧠 **ML Model** – Classifies the hazard type from the image.
- 📍 Auto-tags hazard on the map and **notifies relevant users instantly via our server**.

---

## **9. AI Chatbot Assistant**
- 🤝 **Conversational AI** – Get instant answers to hazard safety questions, health concerns, and emergency steps.
- 🧠 Powered by **LLM-based chatbot** for context-aware responses.

---

## **10. AQI Prediction (Machine Learning)**
- 📈 **Custom ML Model** – Predicts future AQI levels for underserved areas using historical data.
- 📢 **Daily AQI Notifications** – Sent from our backend with safety tips based on your location.
- 🧠 **Smart City Mapping** – If your city doesn’t have a trained AQI model, our system automatically **maps you to the nearest trained city** for accurate predictions.

---

## **11. Google Maps & Mapbox Visualizations**
- 🗺 Hazard, SOS, and AQI markers with interactive popups.
- 🔍 Region-restricted navigation.
- 📌 Tap for quick info popups.

---

## **12. Modern & Responsive UI**
- 🎨 Material Design 3 components.
- 🌈 Gradient buttons for key actions.
- 🃏 Rounded cards & chips for visual appeal.
- 📱 Fully responsive layouts for all screen sizes.

---

## **13. Offline Handling & Error Management**
- 📡 Toast notifications for connectivity issues.
- 🛟 Fallback UI for unavailable data.
- 🔄 Retry options for failed network requests.

---

## **14. Security & Performance**
- 🔐 Firebase Authentication for secure access.
- 🛡 Role-based access control for feature segregation.
- ⚡ Retrofit with caching for speed.
- 📂 SharedPreferences for instant local data retrieval.

---

## 💡 **ML Models in HAZARDIq+:**
1. **AI Medical Recommender** – Symptom → Disease prediction (Hugging Face model).
2. **AI Hazard Detection from Images** – Computer vision model for hazard classification.
3. **AQI Prediction Model** – Custom ML model forecasting air quality levels with **nearest trained city fallback**.
4. **AI Chatbot Assistant** – LLM-powered conversational helper for hazard and health queries.


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
- PostgreSQL 
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
