package app.spareledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.spareledger.data.Account
import app.spareledger.data.Money
import app.spareledger.data.defaultCurrency
import java.util.Currency

@Composable
internal fun SetupScreen(
    busy: Boolean,
    error: String?,
    onSetup: (String, Account) -> Unit,
    onRestore: () -> Unit,
) {
    var currency by rememberSaveable { mutableStateOf(defaultCurrency()) }
    var draft by rememberSaveable { mutableStateOf(AccountDraft()) }
    var chooseCurrency by rememberSaveable { mutableStateOf(false) }
    val money = remember(currency) { Money(currency) }
    val account = draft.account(money)
    val details = Currency.getInstance(currency)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 460.dp).fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = screenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "Set up your first account", Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "Start with cash, a bank account, an e-wallet, or a card.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Panel {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    PickField("Ledger currency", currency + " · " + details.displayName, { chooseCurrency = true })
                    AccountFields(draft, money) { draft = it }
                }
            }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { account?.let { onSetup(currency, it) } },
                enabled = !busy && account != null, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Create account") }
            TextButton(
                onClick = onRestore, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Restore a backup") }
        }
    }
    if (chooseCurrency) {
        CurrencyDialog(currency, { chooseCurrency = false }, { currency = it; chooseCurrency = false })
    }
}

@Composable
private fun CurrencyDialog(selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val options = remember(selected) {
        val preferred = listOf(selected, "PHP", "USD", "EUR", "GBP", "JPY", "SGD", "AUD").distinct()
        Currency.getAvailableCurrencies().filter { it.defaultFractionDigits in 0..4 }
            .sortedWith(
                compareBy<Currency> { preferred.indexOf(it.currencyCode).takeIf { index -> index >= 0 } ?: preferred.size }
                    .thenBy { it.displayName },
            )
    }
    val matching = remember(search, options) {
        val text = search.trim()
        options.filter { it.currencyCode.contains(text, ignoreCase = true) || it.displayName.contains(text, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose currency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    search, { if (it.length <= 60) search = it },
                    Modifier.fillMaxWidth(), label = { Text("Search currencies") }, singleLine = true,
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 348.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(matching, key = { it.currencyCode }) {
                        ChoiceRow(it.currencyCode + " · " + it.displayName, selected == it.currencyCode, { onSelect(it.currencyCode) })
                    }
                }
                if (matching.isEmpty()) Text("No matching currencies")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
