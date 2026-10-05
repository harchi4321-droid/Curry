# Myo Min Khant - Cooking Recipe App (ဟင်းချက်နည်း မှတ်စု)

Android Cooking Recipe Notebook app built with Kotlin, Jetpack Compose, Material 3, and Room Database.

## Features
- **Clean Architecture & Room DB**: Complete local offline persistence for recipes and app preferences.
- **English / Myanmar Bilingual Mode**: Seamless one-tap language switching between Burmese (မြန်မာ) and English.
- **Category System**: Categorize recipes by Main Dishes (ဟင်းလျာ), Soups (ဟင်းချို), Salads (အသုပ်), Fried (အကြော်), Desserts (အချိုပွဲ), Breakfast (မနက်စာ), Drinks (အဖျော်ယမကာ), Snacks (အဆာပြေ).
- **Gemini AI Chef Assistant**: AI-powered recipe formulation and culinary Q&A assistance.
- **Cooking Timer & Photo/Video Support**: Step-by-step timer dialog and video integration.

## How to Build APK using GitHub Actions (GitHub Actions ဖြင့် APK ထုတ်ယူနည်း)

1. **Push to GitHub**:
   - In Google AI Studio, click the menu in the top right corner and choose **"Push to GitHub"**.
2. **Go to GitHub Repository**:
   - Open your GitHub repository in your browser.
   - Click the **"Actions"** tab at the top.
3. **Run Workflow**:
   - Under Workflows, select **"Build Android APK"**.
   - Click the **"Run workflow"** button on the right.
4. **Download APK**:
   - Once the build succeeds (green checkmark), click on the completed run.
   - Scroll down to the **Artifacts** section at the bottom.
   - Click **`MyoMinKhant-Debug-APK`** to download the ready-to-install `.apk` file for your Android phone!

## Local Build
```bash
./gradlew assembleDebug
```
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
