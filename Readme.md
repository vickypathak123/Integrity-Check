# Safetynet

Android Safetynet

Android Ads code that is required in every app of Vasundhara
Infotech [Vasundhara Infotech LLP](https://vasundharainfotechllp.com)

[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=23)
[![](https://jitpack.io/v/sagarpatel1137/Integrity-Check.svg)](https://jitpack.io/#sagarpatel1137/IntegrityCheck/1.0.0)

## Using `build.gradle`

###### Step 1. Add the JitPack repository to your project's `build.gradle`

```groovy
    allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

###### Step 2. Add the dependency to your module's `build.gradle`

```groovy
    android {
    packagingOptions {
        exclude 'META-INF/DEPENDENCIES.txt'
        exclude 'META-INF/DEPENDENCIES'
        exclude 'META-INF/dependencies.txt'
    }
}

dependencies {
    implementation project(":IntegrityCheck")
}
```

1. Your Splash Screen Called Given Code is mandatory
2. Set `credentials.json` in Your Project App->src-main->resources
3. You Must Set PackageName `packageName(getPackageName())`
4. call `checkIntegrity` method in that you get `onSuccess` callback

```kotlin
    AppProtector
    .with(this)
    .appName(getString(R.string.app_name)) // Your Application Name
    .packageName(packageName) //Pass packageName it need to check which you want check Play Integrity
    .checkIntegrity(object : CheckPlayIntegrityStatus {
        override fun onSuccess() {

        }
    })//using this method check application is safe or not if application is it will callback onSuccess if not it display dialog
```

#### Need to Initial all Safetynet Dialog Color Attribute in your App Theme

```xml

<style name="YOUR_APP_THEME" parent="Theme.AppCompat.DayNight.NoActionBar">
    <item name="dialog_title_text_color">@android:color/holo_red_dark</item>
    <item name="dialog_msg_text_color">@android:color/holo_red_dark</item>
    <item name="dialog_link_text_color">@android:color/holo_purple</item>
    <item name="dialog_background_color">@android:color/holo_orange_light</item>
    <item name="dialog_button_background_color">@android:color/holo_blue_light</item>
    <item name="dialog_button_text_color">@android:color/holo_orange_dark</item>
</style>
```