# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep widget provider
-keep class com.example.balancewidget.BalanceWidget { *; }
-keep class com.example.balancewidget.AudioBalanceManager { *; }
