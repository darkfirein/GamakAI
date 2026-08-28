# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve LineNumberTable and SourceFile for meaningful release crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlinx Serialization Rules
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <init>(...);
}

# Room Database Rules
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}

# Domain & Model Entities
-keep class com.example.model.** { *; }
-keep class com.example.memory.** { *; }
-keep class com.example.tools.** { *; }

# Google Generative AI / Gemini
-keep class com.google.ai.client.generativeai.** { *; }
