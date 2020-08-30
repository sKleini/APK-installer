package de.kleini.apk_installer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.*
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.reflect.Type


class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    companion object {
        const val PERMISSION_REQUEST_STORAGE = 0
    }

    lateinit var downloadController: DownloadController
    private var listView: ListView? = null
    private var arrayAdapter: ArrayAdapter<String>? = null
    private var itemMap = mutableMapOf<String, String>()
    private var startRepoUrl = "https://api.github.com/repos/sKleini/APK-releases/contents"

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val thread = Thread(Runnable {
            try {
                synchronized(this) {
                    runGetRequest(startRepoUrl)

                    runOnUiThread {
                        listView = findViewById(R.id.dynamic_apk_list)
                        loadingSpinner.animate().alpha(0f).withEndAction {
                            loadingSpinner.visibility = View.INVISIBLE
                            (loadingSpinner.parent as ViewManager).removeView(loadingSpinner)
                        }.start()

                        arrayAdapter = ArrayAdapter(
                            applicationContext,
                            android.R.layout.simple_list_item_1,
                            itemMap.keys.toList()
                        )
                        listView?.adapter = arrayAdapter
                        listView?.choiceMode = ListView.CHOICE_MODE_SINGLE
                        listView?.onItemClickListener = this
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
        thread.start()
    }

    private fun runGetRequest(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful){
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Unexpected code $response",
                        Toast.LENGTH_LONG
                    ).show()
                }
                throw IOException("Unexpected code $response")
            }

            val listOfReposType: Type = Types.newParameterizedType(
                List::class.java,
                Repo::class.java
            )

            val reposJsonAdapter: JsonAdapter<List<Repo>> =
                moshi.adapter(listOfReposType)

            val repos = reposJsonAdapter.fromJson(response.body!!.source())

            if (repos != null) {
                for (repo in repos) {

                    if (repo.type.equals("dir")) {
                        runGetRequest(repo.url)
                    } else if (repo.type.equals("file")) {
                        if (repo.download_url != null) {
                            itemMap[repo.path] = repo.download_url
                            println(repo.download_url)
                        }
                    }
                }
            }
        }
    }

    @JsonClass(generateAdapter = true)
    data class Repo(
        @Json(name = "name") val name: String
        , @Json(name = "path") val path: String
        , @Json(name = "type") val type: String
        , @Json(name = "url") val url: String
        , @Json(name = "download_url") val download_url: String? = ""
    )

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        val items: String = p0?.getItemAtPosition(p2) as String
        Toast.makeText(applicationContext, itemMap[items], Toast.LENGTH_LONG).show()
        checkStoragePermission(itemMap[items])
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
                Toast.makeText(
                    applicationContext,
                    "PERMISSION_GRANTED ready to download. Please click again.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Permission request was denied.
                mainLayout.showSnackbar(R.string.storage_permission_denied, Snackbar.LENGTH_SHORT)
            }
        }
    }

    private fun checkStoragePermission(apkUrl: String?) {
        // Check if the storage permission has been granted
        if (checkSelfPermissionCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            // start downloading
            if (apkUrl != null) {
                downloadController = DownloadController(this, apkUrl)
                downloadController.enqueueDownload()
            }
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