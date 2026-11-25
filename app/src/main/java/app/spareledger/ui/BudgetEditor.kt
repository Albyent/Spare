package app.spareledger.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import app.spareledger.data.Money
import kotlinx.coroutines.delay
import java.time.YearMonth

@Composable
internal fun BudgetEditor(
    month: YearMonth,
    budget: Long?,
    money: Money,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSave: (Long?) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(budget?.let(money::input).orEmpty()) }
    var focusedOnce by rememberSaveable { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (budget == null && !focusedOnce) {
            delay(280)
            amountFocus.requestFocus()
            keyboard?.show()
            focusedOnce = true
        }
    }
    val amount = money.parse(value)

    EditorFrame(
        title = if (budget == null) "Set monthly budget" else "Edit monthly budget",
        busy = busy, error = error, action = "Save budget", canSave = amount != null && amount >= 0,
        onClose = onClose, onSave = { if (amount != null && amount >= 0) onSave(amount) },
    ) {
        Text(
            monthLabel(month), style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AmountField(
            "Budget amount", value, money, { value = it }, focusRequester = amountFocus, large = true,
            hint = "Enter zero or more.".takeIf { amount != null && amount < 0 },
        )
        if (budget != null) {
            EditorOption("Remove monthly budget", Icons.Outlined.RemoveCircleOutline, !busy) { onSave(null) }
        }
    }
}
