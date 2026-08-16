# kotlinx.serialization (streaming JSON DTOs)
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.**

-keepclassmembers class com.mckai.app.** {
    static *** Companion;
}
-keepclasseswithmembers class com.mckai.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mckai.app.**$$serializer { *; }

# Room entities accessed via generated implementation
-keep class com.mckai.app.data.db.entity.** { *; }