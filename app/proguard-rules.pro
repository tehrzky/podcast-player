# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn com.google.api.client.**
-dontwarn com.google.common.**
