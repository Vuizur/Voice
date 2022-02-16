package voice.app.scanner

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import voice.data.Book
import voice.data.toUri
import voice.ffmpeg.ffmpeg
import java.io.IOException
import javax.inject.Inject

class CoverScanner
@Inject constructor(
  private val context: Context,
  private val coverSaver: CoverSaver,
) {

  suspend fun scan(books: List<Book>) {
    books.forEach { findCoverForBook(it) }
  }

  private suspend fun findCoverForBook(book: Book) {
    val coverFile = book.content.cover
    if (coverFile != null && coverFile.exists())
      return

    val foundOnDisc = findAndSaveCoverFromDisc(book)
    if (foundOnDisc)
      return

    scanForEmbeddedCover(book)
  }

  private suspend fun findAndSaveCoverFromDisc(book: Book): Boolean {
    withContext(Dispatchers.IO) {
      val documentFile = DocumentFile.fromTreeUri(context, book.id.toUri()) ?: return@withContext false
      if (!documentFile.isDirectory) return@withContext false
      documentFile.listFiles().forEach { child ->
        if (child.isFile && child.canRead() && child.type?.startsWith("image/") == true) {
          val coverFile = coverSaver.newBookCoverFile()
          try {
            context.contentResolver.openInputStream(child.uri)?.use { input ->
              coverFile.outputStream().use { output ->
                input.copyTo(output)
              }
            }
            coverSaver.setBookCover(coverFile, book.id)
          } catch (e: IOException) {
            Timber.e(e, "Error while copying the cover from ${child.uri}")
          }
        }
      }
    }
    return false
  }

  private suspend fun scanForEmbeddedCover(book: Book) {
    val coverFile = coverSaver.newBookCoverFile()
    book.chapters
      .take(5).forEach { chapter ->
        ffmpeg(
          input = chapter.id.toUri(),
          context = context,
          command = listOf("-an", coverFile.absolutePath)
        )
        if (coverFile.exists() && coverFile.length() > 0) {
          coverSaver.setBookCover(coverFile, bookId = book.id)
          return
        }
      }
  }
}