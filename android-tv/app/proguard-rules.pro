# Coil and OkHttp are shipped with consumer rules. These keeps cover the
# optional TLS providers OkHttp references reflectively on some platforms.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep the bundled catalogue parser simple and untouched.
-keep class com.stemstream.tv.data.** { *; }
