# Android Fullscreen Map App Setup Instructions

This app provides a **fullscreen map experience** with support for both **Google Maps** and **OpenStreetMap (OSMDroid)** to ensure compatibility with all Android devices, including those without Google Play Services.

## Features:

- ✅ **Fullscreen map experience**: The map takes up the entire screen when viewed
- ✅ **Automatic fallback**: Uses Google Maps when available, OSMDroid otherwise
- ✅ **Location services**: Shows current location with a marker
- ✅ **Immersive UI**: Hides status bar, navigation bar, and action bar when viewing the map
- ✅ **Smart navigation**: Easy access to other app sections via bottom navigation (shown on non-map screens)

## How it works:

### Navigation Behavior:
- **Home Tab (Map)**: 
  - Displays fullscreen map with no UI distractions
  - Hides status bar, navigation bar, action bar, and bottom navigation
  - Uses device's entire screen real estate
- **Other Tabs**: 
  - Shows normal UI with action bar and bottom navigation
  - Standard app behavior

### Map Technology:
- **Devices with Google Play Services**: Uses Google Maps for premium experience
- **Devices without Google Play Services**: Automatically falls back to OpenStreetMap
- **All devices**: Request location permissions and show current location with a marker

## For Google Maps (Optional but Recommended):

If you want to enable Google Maps for devices that support it, you need to set up a Google Maps API key.

### Steps to get a Google Maps API key:

1. **Go to the Google Cloud Console**
   - Visit https://console.cloud.google.com/

2. **Create a new project or select existing one**
   - Click on the project dropdown at the top
   - Create a new project or select an existing one

3. **Enable the Maps SDK for Android**
   - In the search bar, search for "Maps SDK for Android"
   - Click on it and press "Enable"

4. **Create an API key**
   - Go to "Credentials" in the left sidebar
   - Click "Create Credentials" > "API key"
   - Copy the generated API key

5. **Restrict the API key (recommended for security)**
   - Click on your newly created API key
   - Under "Application restrictions", select "Android apps"
   - Add your package name: `com.example.myapplication2`
   - Add your SHA-1 certificate fingerprint (you can get this from Android Studio)

6. **Add the API key to your app**
   - Open `app/src/main/AndroidManifest.xml`
   - Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` with your actual API key

### Getting SHA-1 fingerprint for development:

Run this command in your terminal from the project root:

```bash
./gradlew signingReport
```

Or alternatively:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

## Without Google Maps API Key:

If you don't set up a Google Maps API key, the app will still work perfectly! It will:
- Automatically detect that Google Maps is unavailable
- Fall back to OpenStreetMap (OSMDroid)
- Provide the same fullscreen experience
- Show your current location with a marker

## Permissions:

The app requests the following permissions:
- `ACCESS_FINE_LOCATION` - For precise location
- `ACCESS_COARSE_LOCATION` - For approximate location  
- `INTERNET` - For loading map tiles

## Usage:

1. **Install and open the app**
2. **Navigate to the "Home" tab** - This will show the fullscreen map
3. **Grant location permission when prompted** - This allows the app to show your current location
4. **Enjoy the fullscreen map experience!**
   - The map fills the entire screen
   - Your current location is marked with a pin
   - You can zoom, pan, and explore
5. **To access other app features** - Use the back button or navigate to other tabs

## Fullscreen Experience:

When viewing the map (Home tab):
- ✅ Status bar is hidden
- ✅ Navigation bar is hidden  
- ✅ Action bar is hidden
- ✅ Bottom navigation is hidden
- ✅ Map uses full screen real estate

When viewing other tabs:
- ✅ Normal UI elements are visible
- ✅ Bottom navigation is available
- ✅ Standard Android experience

## Technical Details:

- **Minimum SDK**: Android 8.1 (API 27)
- **Target SDK**: Android 14 (API 36)
- **Languages**: Kotlin
- **Architecture**: MVVM with Android Navigation Component
- **Map Libraries**: Google Maps SDK + OSMDroid
- **Location Services**: Google Play Services Location API + Android Location Manager