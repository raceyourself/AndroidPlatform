-dontobfuscate

# Jackson
-keep enum com.fasterxml.jackson.** {
    *;
}

# Jackson indirectly references classes in these namespaces that aren't in Android but are in J2SE.
# Because they're indirectly referenced, it's okay to simply ignore the fact they don't exist.
-dontwarn javax.xml.**,org.w3c.dom.**

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep public class com.raceyourself.platform.gpstracker.Helper
-keep public class com.raceyourself.platform.gpstracker.SyncHelper
-keep public class com.raceyourself.platform.gpstracker.FauxTargetTracker
-keep public class com.raceyourself.platform.auth.Helper
-keep public class com.raceyourself.platform.sensors.GestureHelper
-keep public class com.raceyourself.platform.points.PointsHelper
-keep public class com.raceyourself.platform.sensors.SensoriaSock
-keep class com.raceyourself.platform.models.** {*;}
-keep class com.roscopeco.ormdroid.** {*;}

-keepattributes Signature,InnerClasses

-dontwarn **
