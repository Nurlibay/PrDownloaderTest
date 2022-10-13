package uz.unidev.prdownlaodtask

import android.os.Environment
import android.util.Log
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object Repository {

    private val dirPath = Environment.getExternalStorageDirectory().absolutePath

    private val fireStore = FirebaseFirestore.getInstance()

    suspend fun getBookData(): BookData? {
        var bookData: BookData? = null
        val doc = fireStore.collection("books").document("7X1JLJ5Nxk4Ldp21eM20")
        doc.get()
            .await().apply {
                val documentSpanShot = this
                bookData = BookData(
                    documentSpanShot.getString("id")!!,
                    documentSpanShot.getString("name")!!,
                    documentSpanShot.getString("description")!!,
                    documentSpanShot.getString("img")!!,
                    documentSpanShot.getString("file")!!
                )
            }
        return bookData
    }

    fun downloadFile(bookData: BookData): Flow<Result> = callbackFlow {
        Log.d("kitap", bookData.file)
        Log.d("joli", dirPath)
        PRDownloader.download(
            bookData.file,
            dirPath.plus("/${Environment.DIRECTORY_DOCUMENTS}"),
            bookData.name
        ).setTag(bookData.id)
            .build()
            .setOnStartOrResumeListener {
                trySend(Result.Start)
            }
            .setOnProgressListener {
                trySend(Result.Progress(it.currentBytes, it.totalBytes))
            }.start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    trySend(Result.End(bookData.name))
                }
                override fun onError(error: Error?) {
                    Log.d("xatolik", error?.connectionException.toString())
                    Log.d("xatolik", error?.serverErrorMessage.toString())
                    trySend(Result.Error(error.toString()))
                }
            })
        awaitClose {
            PRDownloader.cancel(bookData.id)
        }
    }
}

sealed interface Result {
    object Start : Result
    class End(val fileName: String) : Result
    class Progress(val current: Long, val total: Long) : Result
    class Error(val message: String) : Result
}
