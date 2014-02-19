# This proguard config should be run from the comman line as follows:
# java -jar /path/to/proguard.jar proguard-project.txt

-injars build/libs/GlassFitPlatform.jar
-outjars build/libs/GlassFitPlatform_proguard.jar

-libraryjars libs/
-libraryjars /Users/benlister/android-sdks/platforms/android-15/android.jar
-libraryjars /Users/benlister/android-sdks/extras/google/google_play_services/libproject/google-play-services_lib/libs/google-play-services.jar
-dontobfuscate
-dontwarn com.unity3d.**


# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Tell proguard not to strip out the following classes which are only accessed via JNI:
-keep public class com.glassfitgames.glassfitplatform.gpstracker.Helper
-keep public class com.glassfitgames.glassfitplatform.gpstracker.SyncHelper
-keep public class com.glassfitgames.glassfitplatform.gpstracker.FauxTargetTracker
-keep public class com.glassfitgames.glassfitplatform.auth.Helper
-keep public class com.glassfitgames.glassfitplatform.sensors.GestureHelper
-keep public class com.glassfitgames.glassfitplatform.points.PointsHelper
-keep public class com.glassfitgames.glassfitplatform.models.Track
-keep public class com.glassfitgames.glassfitplatform.models.Game
-keep public class com.glassfitgames.glassfitplatform.models.Transaction
-keep public class com.glassfitgames.glassfitplatform.sensors.SensoriaSock