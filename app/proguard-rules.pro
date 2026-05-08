# Keep Room-generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp

# Kotlin metadata
-keep class kotlin.Metadata { *; }
