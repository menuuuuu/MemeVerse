# MemeVerse Production Deployment & Firebase Setup Guide

This guide details the steps to deploy the **MemeVerse** application into a production cloud infrastructure and integrate it with Firebase suite (Authentication, Firestore, and Storage).

---

## 1. Firebase Suite Setup

Follow these steps to migrate the native SQLite/Room model or hook your React web platform to a production Firebase instance:

### Step 1.1: Create a Firebase Project
1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add Project** and name it `MemeVerse`.
3. Configure Google Analytics (optional) and click **Create Project**.

### Step 1.2: Enable Authentication (Email & Google)
1. In the sidebar, navigate to **Build** -> **Authentication**.
2. Click **Get Started**.
3. Under the **Sign-in method** tab, enable:
   - **Email/Password**: Enable both email login and registration.
   - **Google**: Configure your project support email and enable. Save client tokens.
4. If building for Android, add your SHA-1 fingerprint (retrieved via `gradle signingReport` task) to your Firebase Project Settings.
5. Download and place `google-services.json` inside your `/app` directory.

### Step 1.3: Provision cloud Firestore Database
1. Navigate to **Build** -> **Firestore Database**.
2. Click **Create Database**.
3. Choose **Start in production mode** or **test mode** (for development).
4. Select a database storage region close to your target audience.
5. Define security rules:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read: if true;
         allow write: if request.auth != null && request.auth.uid == userId;
       }
       match /memes/{memeId} {
         allow read: if true;
         allow create: if request.auth != null;
         allow delete: if request.auth != null && (resource.data.creatorId == request.auth.uid || request.auth.token.admin == true);
       }
       match /comments/{commentId} {
         allow read, write: if request.auth != null;
       }
       match /reports/{reportId} {
         allow read: if request.auth != null && request.auth.token.admin == true;
         allow create: if request.auth != null;
         allow update: if request.auth != null && request.auth.token.admin == true;
       }
     }
   }
   ```

### Step 1.4: Set up Firebase Storage for Videos
1. Navigate to **Build** -> **Storage**.
2. Click **Get Started**.
3. Configure security rules allowing public video playback but authenticated video streams uploads:
   ```javascript
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /videos/{allPaths=**} {
         allow read: if true;
         allow write: if request.auth != null && request.resource.size < 100 * 1024 * 1024; // Limit 100MB
       }
     }
   }
   ```

---

## 2. Production Deployment Instructions

### Option A: Android APK Deployment
To generate a production-ready signed APK/AAB:
1. Open the AI Studio settings or terminal.
2. Run the release builder task in Gradle:
   ```bash
   gradle assembleRelease
   ```
3. Locate your build outputs inside `/app/build/outputs/apk/release/app-release-unsigned.apk`.
4. Sign the build bundle with your private keystore file (`my-upload-key.jks`) using `apksigner`.

### Option B: Web App React Host Deployment
To deploy a React-based variant of MemeVerse to production CDN (e.g. Firebase Hosting or Vercel):
1. Install Firebase CLI:
   ```bash
   npm install -g firebase-tools
   ```
2. Log into your Firebase console from the CLI:
   ```bash
   firebase login
   ```
3. Initialize hosting configurations:
   ```bash
   firebase init hosting
   ```
   Select your project `MemeVerse` and choose `build` as your public build directory (configured for Single Page redirection).
4. Run standard Web production compiler:
   ```bash
   npm run build
   ```
5. Deploy to production servers:
   ```bash
   firebase deploy --only hosting
   ```
   The CLI will provide your static production live URL (e.g., `https://memeverse.web.app`).

---

## 3. Technology Alignment Outline

| Component | Architecture Variant Used | Firebase Production Service Mapping |
| --- | --- | --- |
| **User Signin Engine** | Local SQLite user profile registers | `firebase.auth().signInWithEmailAndPassword` |
| **Reels Database** | SQLite `@Entity` table `memes` | Firestore collection `/memes` |
| **Comment Threads** | SQLite `@Entity` table `comments` | Firestore collection `/comments` |
| **Report Abuse Queue** | SQLite `@Entity` table `reports` | Firestore collection `/reports` |
| **Meme Media Assets** | Rendered custom vector gradient indicators | Firebase Cloud Storage buckets `/videos` |
| **Site Auditing** | `AdminDashboardScreen` stats logic | Firebase Cloud Admin Functions dashboards |
