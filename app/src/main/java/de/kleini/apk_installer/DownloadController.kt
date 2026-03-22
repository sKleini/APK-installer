package de.kleini.apk_installer

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

class DownloadController(private val context: Context, private val url: String) {

    companion object {
        private const val TAG = "DownloadController"
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
    }

    fun enqueueDownload() {
        val fileName = Uri.parse(url).lastPathSegment ?: "download.apk"
        val destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + fileName
        val uri = Uri.parse("$FILE_BASE_PATH$destination")

        val file = File(destination)
        if (file.exists()) file.delete()

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
        request.setMimeType(MIME_TYPE)
        request.setTitle(context.getString(R.string.title_file_download))
        request.setDescription(context.getString(R.string.downloading))
        request.setDestinationUri(uri)

        Log.d(TAG, "Download eingereiht: $fileName")
        showInstallOption(destination)
        downloadManager.enqueue(request)
    }

    private fun showInstallOption(destination: String) {
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val file = File(destination)
                if (!file.exists()) {
                    Log.e(TAG, "Heruntergeladene Datei nicht gefunden: $destination")
                    context.unregisterReceiver(this)
                    return
                }

                val contentUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                    file
                )
                val install = Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    data = contentUri
                }
                context.startActivity(install)
                context.unregisterReceiver(this)
                Log.d(TAG, "Installation gestartet: $destination")
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}
