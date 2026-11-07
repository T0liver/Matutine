package hu.toliver.matutine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.matutine.edition.EditionScreen
import hu.toliver.matutine.edition.EditionViewModel
import hu.toliver.matutine.edition.LocalEditionRepository
import hu.toliver.matutine.store.FileEditionStore
import hu.toliver.matutine.theme.MatutineTheme

@Composable
fun MatutineApp(dataRoot: String) {
    val viewModel = remember(dataRoot) {
        val store = FileEditionStore(dataRoot)
        val repository = LocalEditionRepository(store)
        EditionViewModel(repository)
    }
    MatutineTheme {
        EditionScreen(
            state = viewModel.state,
            onRetrySample = viewModel::retrySample,
        )
    }
}

@Composable
@Preview
fun MatutineAppPreview() {
    MatutineApp(dataRoot = ".")
}
