# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class app.sukoon.**$$serializer { *; }
-keepclassmembers class app.sukoon.** { *** Companion; }
-keepclasseswithmembers class app.sukoon.** { kotlinx.serialization.KSerializer serializer(...); }

# Navigation type-safe routes are serialized by name
-keep @kotlinx.serialization.Serializable class app.sukoon.ui.navigation.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# WorkManager workers instantiated by HiltWorkerFactory
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# Glance receivers
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver
