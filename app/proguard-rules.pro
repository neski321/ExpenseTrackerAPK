# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Added for Firebase Auth and session persistence issues ---
# Keep all Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep all model classes (if you use reflection or Gson)
-keepclassmembers class com.neski.pennypincher.data.models.** { *; }

# Keep Kotlin coroutines and flows
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep all classes with @Keep annotation
-keep @androidx.annotation.Keep class * { *; }

# Keep all public methods in SessionManager
-keep class com.neski.pennypincher.data.repository.SessionManager { *; }
# --- End custom rules ---