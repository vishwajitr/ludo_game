# Default Android + Compose consumer rules are enough: no reflection, no serialization library.
# Keep line numbers in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager (pulled in by the Ads SDK) creates its Room database by reflection (WorkDatabase_Impl's
# no-arg constructor); R8 full mode strips it otherwise and the app crashes at startup.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
