package com.example.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.net.URLEncoder

class ToolManager(private val context: Context) {

    private val commonAppMap = mapOf(
        "whatsapp" to "com.whatsapp",
        "হোয়াটসঅ্যাপ" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "ইউটিউব" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "ক্রোম" to "com.android.chrome",
        "telegram" to "org.telegram.messenger",
        "টেলিগ্রাম" to "org.telegram.messenger",
        "maps" to "com.google.android.apps.maps",
        "ম্যাপস" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "facebook" to "com.facebook.katana",
        "ফেসবুক" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "ইনস্টাগ্রাম" to "com.instagram.android",
        "spotify" to "com.spotify.music",
        "স্পটিফাই" to "com.spotify.music"
    )

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "jarvis_alerts",
                "JARVIS Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "JARVIS Assistant alerts and command confirmations"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun resolvePackage(appName: String, packageHint: String = ""): String? {
        val cleanName = appName.trim().lowercase()
        if (packageHint.isNotEmpty() && isPackageInstalled(packageHint)) {
            return packageHint
        }
        if (commonAppMap.containsKey(cleanName)) {
            return commonAppMap[cleanName]
        }
        for ((key, pkg) in commonAppMap) {
            if (cleanName.contains(key) || key.contains(cleanName)) {
                return pkg
            }
        }
        // Try searching installed applications
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in packages) {
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                if (label == cleanName || label.contains(cleanName)) {
                    return appInfo.packageName
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return if (packageHint.isNotEmpty()) packageHint else null
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openApp(appName: String, packageHint: String = ""): ActionResult {
        val targetPackage = resolvePackage(appName, packageHint)

        if (targetPackage == null || !isPackageInstalled(targetPackage)) {
            val appTitle = if (appName.isNotBlank()) appName else "App"
            return ActionResult(
                success = false,
                intent = JarvisIntent.OPEN_APP,
                spokenResponseBn = "আপনার ফোনে $appTitle ইনস্টল করা নেই। আমি কি Google Play Store থেকে এটি ডাউনলোড করার জন্য খুলে দেব?",
                spokenResponseEn = "$appTitle is not installed on your phone. Would you like me to open the Google Play Store to download it?",
                displayMessage = "$appTitle is not installed on this device.",
                requiresPlayStoreOffer = true,
                appName = appTitle,
                packageName = targetPackage ?: ""
            )
        }

        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ActionResult(
                    success = true,
                    intent = JarvisIntent.OPEN_APP,
                    spokenResponseBn = "ঠিক আছে, $appName চালু করছি।",
                    spokenResponseEn = "Opening $appName now, sir.",
                    displayMessage = "Launched $appName ($targetPackage) successfully.",
                    appName = appName,
                    packageName = targetPackage
                )
            } else {
                ActionResult(
                    success = false,
                    intent = JarvisIntent.OPEN_APP,
                    spokenResponseBn = "অ্যাপটি চালু করতে পারছি না।",
                    spokenResponseEn = "Unable to launch $appName.",
                    displayMessage = "No launchable activity found for $targetPackage.",
                    appName = appName,
                    packageName = targetPackage
                )
            }
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.OPEN_APP,
                spokenResponseBn = "ত্রুটি হয়েছে: ${e.localizedMessage}",
                spokenResponseEn = "Error launching $appName: ${e.localizedMessage}",
                displayMessage = "Launch error: ${e.message}",
                appName = appName,
                packageName = targetPackage
            )
        }
    }

    fun checkAppInstalled(appName: String, packageHint: String = ""): ActionResult {
        val targetPackage = resolvePackage(appName, packageHint)
        val installed = targetPackage != null && isPackageInstalled(targetPackage)
        val appTitle = if (appName.isNotBlank()) appName else "অ্যাপ"

        return if (installed) {
            ActionResult(
                success = true,
                intent = JarvisIntent.CHECK_APP_INSTALLED,
                spokenResponseBn = "হ্যাঁ, আপনার ফোনে $appTitle ইনস্টল করা আছে।",
                spokenResponseEn = "Yes, $appTitle is installed on your device.",
                displayMessage = "$appTitle is installed ($targetPackage).",
                appName = appTitle,
                packageName = targetPackage ?: ""
            )
        } else {
            ActionResult(
                success = false,
                intent = JarvisIntent.CHECK_APP_INSTALLED,
                spokenResponseBn = "না, আপনার ফোনে $appTitle ইনস্টল করা নেই। Google Play Store থেকে ডাউনলোড করতে চান?",
                spokenResponseEn = "No, $appTitle is not installed. Would you like to get it from Google Play?",
                displayMessage = "$appTitle is NOT installed.",
                requiresPlayStoreOffer = true,
                appName = appTitle,
                packageName = targetPackage ?: ""
            )
        }
    }

    fun openPlayStore(appName: String, packageName: String = ""): ActionResult {
        val resolvedPkg = if (packageName.isNotEmpty()) packageName else resolvePackage(appName) ?: ""
        val appTitle = if (appName.isNotBlank()) appName else "App"

        return try {
            val uri = if (resolvedPkg.isNotEmpty()) {
                Uri.parse("market://details?id=$resolvedPkg")
            } else {
                Uri.parse("market://search?q=" + URLEncoder.encode(appName, "UTF-8"))
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.android.vending")
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback to browser URL
                val webUri = if (resolvedPkg.isNotEmpty()) {
                    Uri.parse("https://play.google.com/store/apps/details?id=$resolvedPkg")
                } else {
                    Uri.parse("https://play.google.com/store/search?q=" + URLEncoder.encode(appName, "UTF-8"))
                }
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }

            ActionResult(
                success = true,
                intent = JarvisIntent.OPEN_PLAY_STORE,
                spokenResponseBn = "ঠিক আছে, $appTitle-এর official Play Store page খুলছি।",
                spokenResponseEn = "Opening the official Google Play Store page for $appTitle.",
                displayMessage = "Opened Google Play Store for $appTitle.",
                appName = appTitle,
                packageName = resolvedPkg
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.OPEN_PLAY_STORE,
                spokenResponseBn = "Play Store খুলতে পারছি না।",
                spokenResponseEn = "Failed to open Google Play Store: ${e.localizedMessage}",
                displayMessage = "Play Store error: ${e.message}"
            )
        }
    }

    fun closeApp(appName: String): ActionResult {
        // Android does not permit third-party apps to kill other applications directly without root/system privileges.
        // As instructed: navigate Home and explain Android limitation honestly.
        return try {
            goHome()
            val msgBn = if (appName.isNotBlank()) {
                "Android-এর নিয়ম অনুযায়ী আমি $appName সরাসরি force-stop করতে পারছি না। তবে আপনাকে Home screen-এ নিয়ে এসেছি।"
            } else {
                "Android-এর নিয়ম অনুযায়ী আমি অ্যাপটি সরাসরি force-stop করতে পারছি না। তবে আপনাকে Home screen-এ নিয়ে এসেছি।"
            }
            val msgEn = if (appName.isNotBlank()) {
                "Android policy does not allow direct force-stopping of $appName, but I have navigated you to the Home screen."
            } else {
                "Android security does not permit direct force-stopping of third-party apps, but I navigated to the Home screen."
            }
            ActionResult(
                success = true,
                intent = JarvisIntent.CLOSE_APP,
                spokenResponseBn = msgBn,
                spokenResponseEn = msgEn,
                displayMessage = "Navigated to Home screen (App force-stop is restricted by Android OS)."
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.CLOSE_APP,
                spokenResponseBn = "হোম স্ক্রিনে যেতে পারছি না।",
                spokenResponseEn = "Failed to navigate Home: ${e.message}",
                displayMessage = "Close app action failed: ${e.message}"
            )
        }
    }

    fun goHome(): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult(
                success = true,
                intent = JarvisIntent.GO_HOME,
                spokenResponseBn = "হোম স্ক্রিনে নিয়ে এসেছি।",
                spokenResponseEn = "Navigated to Home screen, sir.",
                displayMessage = "Navigated to Home screen."
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.GO_HOME,
                spokenResponseBn = "হোম স্ক্রিন খুলতে পারছি না।",
                spokenResponseEn = "Could not navigate to Home.",
                displayMessage = "Go Home error: ${e.message}"
            )
        }
    }

    fun webSearch(query: String, openBrowser: Boolean = true): ActionResult {
        if (query.isBlank()) {
            return ActionResult(
                success = false,
                intent = JarvisIntent.WEB_SEARCH,
                spokenResponseBn = "অনুসন্ধান করার জন্য কিছু বলুন।",
                spokenResponseEn = "Please specify what you would like me to search.",
                displayMessage = "Empty search query."
            )
        }

        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.google.com/search?q=$encoded"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                success = true,
                intent = JarvisIntent.WEB_SEARCH,
                spokenResponseBn = "Google-এ \"$query\" অনুসন্ধান করেছি।",
                spokenResponseEn = "Searched the web for \"$query\".",
                displayMessage = "Web search opened: $query"
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.WEB_SEARCH,
                spokenResponseBn = "অনুসন্ধান খুলতে ত্রুটি হয়েছে।",
                spokenResponseEn = "Failed to execute web search: ${e.message}",
                displayMessage = "Web search failed: ${e.message}"
            )
        }
    }

    fun searchYouTube(query: String): ActionResult {
        return try {
            val cleanQuery = query.trim()
            val uri = if (cleanQuery.isNotEmpty()) {
                val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
                Uri.parse("https://www.youtube.com/results?search_query=$encoded")
            } else {
                Uri.parse("https://www.youtube.com")
            }

            val appIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.google.android.youtube")
            }

            if (appIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appIntent)
            } else {
                // Browser fallback
                val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }

            val spokenBn = if (cleanQuery.isNotEmpty()) {
                "YouTube-এ \"$cleanQuery\" অনুসন্ধান করেছি।"
            } else {
                "YouTube চালু করেছি।"
            }
            val spokenEn = if (cleanQuery.isNotEmpty()) {
                "Searching YouTube for \"$cleanQuery\"."
            } else {
                "Opening YouTube now, sir."
            }

            ActionResult(
                success = true,
                intent = JarvisIntent.SEARCH_YOUTUBE,
                spokenResponseBn = spokenBn,
                spokenResponseEn = spokenEn,
                displayMessage = "YouTube query: $cleanQuery"
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.SEARCH_YOUTUBE,
                spokenResponseBn = "YouTube খুলতে পারছি না।",
                spokenResponseEn = "Failed to open YouTube: ${e.message}",
                displayMessage = "YouTube error: ${e.message}"
            )
        }
    }

    fun openCamera(): ActionResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                success = true,
                intent = JarvisIntent.OPEN_CAMERA,
                spokenResponseBn = "ক্যামেরা চালু করেছি।",
                spokenResponseEn = "Camera opened, sir.",
                displayMessage = "Opened camera application."
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.OPEN_CAMERA,
                spokenResponseBn = "ক্যামেরা খুলতে পারছি না।",
                spokenResponseEn = "Could not open camera: ${e.message}",
                displayMessage = "Camera error: ${e.message}"
            )
        }
    }

    fun openMaps(query: String = ""): ActionResult {
        return try {
            val uri = if (query.isNotBlank()) {
                val encoded = URLEncoder.encode(query, "UTF-8")
                Uri.parse("geo:0,0?q=$encoded")
            } else {
                Uri.parse("geo:0,0")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                success = true,
                intent = JarvisIntent.OPEN_MAPS,
                spokenResponseBn = if (query.isNotBlank()) "ম্যাপসে \"$query\" অনুসন্ধান করছি।" else "Google Maps খুলেছি।",
                spokenResponseEn = if (query.isNotBlank()) "Searching Maps for \"$query\"." else "Opening Google Maps, sir.",
                displayMessage = "Maps opened: $query"
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.OPEN_MAPS,
                spokenResponseBn = "ম্যাপস খুলতে পারছি না।",
                spokenResponseEn = "Failed to open Maps: ${e.message}",
                displayMessage = "Maps error: ${e.message}"
            )
        }
    }

    fun openSettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(
                success = true,
                intent = JarvisIntent.OPEN_SETTINGS,
                spokenResponseBn = "সিস্টেম সেটিংস খুলেছি।",
                spokenResponseEn = "System settings opened, sir.",
                displayMessage = "Opened Android System Settings."
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.OPEN_SETTINGS,
                spokenResponseBn = "সেটিংস খুলতে পারছি না।",
                spokenResponseEn = "Failed to open settings: ${e.message}",
                displayMessage = "Settings error: ${e.message}"
            )
        }
    }

    fun copyToClipboard(text: String): ActionResult {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("JARVIS Output", text)
            clipboard.setPrimaryClip(clip)
            ActionResult(
                success = true,
                intent = JarvisIntent.COPY_CLIPBOARD,
                spokenResponseBn = "ক্লিপবোর্ডে কপি করা হয়েছে।",
                spokenResponseEn = "Copied to clipboard, sir.",
                displayMessage = "Content copied to clipboard."
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.COPY_CLIPBOARD,
                spokenResponseBn = "কপি করতে ব্যর্থ হয়েছে।",
                spokenResponseEn = "Failed to copy to clipboard.",
                displayMessage = "Clipboard error: ${e.message}"
            )
        }
    }

    fun showNotification(title: String, message: String): ActionResult {
        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, "jarvis_alerts")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())

            ActionResult(
                success = true,
                intent = JarvisIntent.SHOW_NOTIFICATION,
                spokenResponseBn = "নোটিফিকেশন পাঠানো হয়েছে।",
                spokenResponseEn = "Notification dispatched, sir.",
                displayMessage = "Notification sent: $title"
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                intent = JarvisIntent.SHOW_NOTIFICATION,
                spokenResponseBn = "নোটিফিকেশন পাঠাতে সমস্যা হয়েছে।",
                spokenResponseEn = "Failed to post notification.",
                displayMessage = "Notification error: ${e.message}"
            )
        }
    }
}
