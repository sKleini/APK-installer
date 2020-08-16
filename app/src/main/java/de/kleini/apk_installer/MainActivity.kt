package de.kleini.apk_installer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST_STORAGE = 0
    }

    lateinit var downloadController: DownloadController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonAPK_lock_master.setOnClickListener {
            val apkUrl  = "https://github.com/sKleini/APK-releases/raw/master/Lock/Lock.apk"
            checkStoragePermission(apkUrl)
        }

        buttonAPK_bxLock_master.setOnClickListener {
            val apkUrl  = "https://github.com/sKleini/APK-releases/raw/master/bxLock/bxLock.apk"
            checkStoragePermission(apkUrl)
        }

        buttonAPK_lock_develop.setOnClickListener {
            val apkUrl  = "https://github.com/sKleini/APK-releases/raw/master/Lock/develop/Lock.apk"
            checkStoragePermission(apkUrl)
        }

        buttonAPK_bxLock_develop.setOnClickListener {
            val apkUrl  = "https://github.com/sKleini/APK-releases/raw/master/bxLock/develop/bxLock.apk"
            checkStoragePermission(apkUrl)
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // start downloading
                Toast.makeText(applicationContext,"PERMISSION_GRANTED ready to download. Please click again.",Toast.LENGTH_SHORT).show()
            } else {
                // Permission request was denied.
                mainLayout.showSnackbar(R.string.storage_permission_denied, Snackbar.LENGTH_SHORT)
            }
        }
    }


    private fun checkStoragePermission(apkUrl: String) {
        // Check if the storage permission has been granted
        if (checkSelfPermissionCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            // start downloading
            downloadController = DownloadController(this, apkUrl)
            downloadController.enqueueDownload()
        } else {
            // Permission is missing and must be requested.
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {

        if (shouldShowRequestPermissionRationaleCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            mainLayout.showSnackbar(
                R.string.storage_access_required,
                Snackbar.LENGTH_INDEFINITE, R.string.ok
            ) {
                requestPermissionsCompat(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_STORAGE
                )
            }

        } else {
            requestPermissionsCompat(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_STORAGE
            )
        }
    }
}