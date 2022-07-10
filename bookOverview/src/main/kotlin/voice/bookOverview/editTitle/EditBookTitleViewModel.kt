package voice.bookOverview.editTitle

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.data.Book
import voice.data.repo.BookRepository
import javax.inject.Inject

class EditBookTitleViewModel
@Inject
constructor(
  private val repo: BookRepository,
) {

  private val scope = MainScope()

  private val _state = mutableStateOf<EditBookTitleState?>(null)
  internal val state: State<EditBookTitleState?> get() = _state

  internal fun onEditBookTitleClick(id: Book.Id) {
    scope.launch {
      val book = repo.get(id) ?: return@launch
      _state.value = EditBookTitleState(
        title = book.content.name,
        bookId = id,
      )
    }
  }

  internal fun onDismissEditTitle() {
    _state.value = null
  }

  internal fun onUpdateEditTitle(title: String) {
    _state.value = _state.value?.copy(title = title)
  }

  internal fun onConfirmEditTitle() {
    val state = _state.value
    if (state != null) {
      check(state.confirmButtonEnabled)
      scope.launch {
        repo.updateBook(state.bookId) {
          it.copy(name = state.title.trim())
        }
      }
    }
    _state.value = null
  }
}
