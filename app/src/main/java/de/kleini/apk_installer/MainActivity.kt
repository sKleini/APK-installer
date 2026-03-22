package de.kleini.apk_installer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.*
import de.kleini.apk_installer.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.reflect.Type


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        const val PERMISSION_REQUEST_STORAGE = 0
    }

    private lateinit var binding: ActivityMainBinding
    lateinit var downloadController: DownloadController
    private var arrayAdapter: ArrayAdapter<String>? = null
    private var itemMap = mutableMapOf<String, String>()
    private var pendingDownloadUrl: String? = null
    private val startRepoUrl = "https://api.github.com/repos/sKleini/APK-releases/contents"

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadApkList()
    }

    private fun loadApkList() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    fetchItems(startRepoUrl)
                }

                binding.loadingSpinner.animate().alpha(0f).withEndAction {
                    binding.loadingSpinner.visibility = View.GONE
                }.start()

                arrayAdapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_list_item_1,
                    itemMap.keys.toList()
                )
                binding.dynamicApkList.adapter = arrayAdapter
                binding.dynamicApkList.choiceMode = android.widget.ListView.CHOICE_MODE_SINGLE
                binding.dynamicApkList.setOnItemClickListener { parent, _, position, _ ->
                    val itemName = parent.getItemAtPosition(position) as String
                    val url = itemMap[itemName]
                    checkStoragePermission(url)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load APK list", e)
                Toast.makeText(this@MainActivity, "Fehler beim Laden der Liste: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchItems(url: String, depth: Int = 0) {
        if (depth > 5) {
            Log.w(TAG, "Max recursion depth reached for URL: $url")
            return
        }

        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: $url")
            }

            val body = response.body ?: throw Exception("Leere Antwort von: $url")

            val listOfReposType: Type = Types.newParameterizedType(List::class.java, Repo::class.java)
            val reposJsonAdapter: JsonAdapter<List<Repo>> = moshi.adapter(listOfReposType)
            val repos = reposJsonAdapter.fromJson(body.source()) ?: return

            for (repo in repos) {
                when (repo.type) {
                    "dir" -> fetchItems(repo.url, depth + 1)
                    "file" -> {
                        val downloadUrl = repo.download_url
                        if (downloadUrl != null) {
                            itemMap[repo.path] = downloadUrl
                            Log.d(TAG, "APK gefunden: ${repo.path}")
                        }
                    }
                }
            }
        }
    }

    @JsonClass(generateAdapter = true)
    data class Repo(
        @Json(name = "name") val name: String,
        @Json(name = "path") val path: String,
        @Json(name = "type") val type: String,
        @Json(name = "url") val url: String,
        @Json(name = "download_url") val download_url: String? = null
    )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val url = pendingDownloadUrl
                pendingDownloadUrl = null
                if (url != null) {
                    startDownload(url)
                }
            } else {
                binding.root.showSnackbar(R.string.storage_permission_denied, Snackbar.LENGTH_SHORT)
            }
        }
    }

    private fun checkStoragePermission(apkUrl: String?) {
        if (apkUrl == null) return

        if (checkSelfPermissionCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startDownload(apkUrl)
        } else {
            pendingDownloadUrl = apkUrl
            requestStoragePermission()
        }
    }

    private fun startDownload(apkUrl: String) {
        Log.d(TAG, "Download gestartet: $apkUrl")
        downloadController = DownloadController(this, apkUrl)
        downloadController.enqueueDownload()
    }

    private fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationaleCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            binding.root.showSnackbar(
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
