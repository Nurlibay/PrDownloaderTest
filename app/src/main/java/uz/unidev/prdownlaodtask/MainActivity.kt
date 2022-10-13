package uz.unidev.prdownlaodtask

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.rajat.pdfviewer.PdfViewerActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import uz.unidev.prdownlaodtask.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var bookData: BookData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            bookData = Repository.getBookData()
            if (bookData != null) {
                Glide
                    .with(this@MainActivity)
                    .load(bookData?.img)
                    .into(binding.ivBook)

                binding.tvBookName.text = bookData?.name
                binding.tvBookDescription.text = bookData?.description
            }
        }
        binding.btnDownload.setOnClickListener {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                        if (bookData != null) {
                            Repository.downloadFile(bookData!!).onEach {
                                when (it) {
                                    is Result.Start -> {
                                        binding.progressHorizontal.visibility = View.VISIBLE
                                    }
                                    is Result.Progress -> {
                                        binding.progressHorizontal.progress = ((it.current / it.total) * 100).toInt()
                                        binding.btnDownload.text = ((it.current / it.total) * 100).toInt().toString()
                                    }
                                    is Result.End -> {
                                        binding.btnDownload.text = getString(R.string.open)
                                        binding.btnDownload.setOnClickListener { _ ->
                                            val file = File(
                                                Environment.getExternalStorageDirectory().absolutePath.plus(
                                                    "/${Environment.DIRECTORY_DOCUMENTS}"
                                                ),
                                                it.fileName
                                            )
                                            if (file.exists()) {
                                                openPdfViewer(it.fileName, it.localPath)
                                            }
                                        }
                                        binding.progressHorizontal.visibility = View.INVISIBLE
                                    }
                                    is Result.Error -> {
                                        binding.progressHorizontal.visibility = View.INVISIBLE
                                        Toast.makeText(
                                            this@MainActivity,
                                            it.message,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }.launchIn(lifecycleScope)
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        // Not yet implemented
                    }
                }).check()
        }
    }

    private fun openPdfViewer(fileName: String, path: String) {
        startActivity(
            PdfViewerActivity.launchPdfFromPath(
                this,
                path,
                fileName,
                "pdf directory to save",
                enableDownload = true
            )
        )
    }
}