# ProGuard & R8 Optimization Rules for Scanner Pro

# Keep Android & Jetpack Room Database components
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# Keep ML Kit on-device Text Recognition
-keep class com.google.mlkit.vision.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Keep Application Models & Core Engines
-keep class com.example.data.model.** { *; }
-keep class com.example.engine.** { *; }

# Keep Jetpack Compose State & Composables
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Optimize coroutines and remove logging overhead in shrunk build
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
