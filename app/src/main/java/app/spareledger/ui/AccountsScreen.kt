package app.spareledger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.spareledger.data.Account
import app.spareledger.data.AccountKind
import app.spareledger.data.LedgerSnapshot
import app.spareledger.data.Money
import app.spareledger.data.balances
import java.time.LocalDate

@Composable
internal fun AccountsScreen(
    data: LedgerSnapshot,
    today: LocalDate,
    money: Money,
    onAdd: () -> Unit,
    onEdit: (Account) -> Unit,
) {
    val balances = remember(data, today) { data.balances(today) }
    val active = data.activeAccounts
    val archived = data.accounts.filter(Account::archived)
    val cash = active.filter { it.kind != AccountKind.CARD }.sumOf { balances.getValue(it.id) }
    val cards = active.filter { it.kind == AccountKind.CARD }
    val owed = cards.sumOf { (-balances.getValue(it.id)).coerceAtLeast(0) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = screenWidth).fillMaxSize(),
            contentPadding = PaddingValues(start = screenGutter, end = screenGutter, top = 4.dp, bottom = bottomSpace),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (active.isNotEmpty()) {
                item {
                    Panel {
                        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Cash, bank & e-wallets", style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            AmountText(
                                money.format(cash), Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            if (cards.isNotEmpty()) {
                                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Credit cards owed", Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
                                    AmountText(
                                        money.format(owed), Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleSmall, alignment = TextAlign.End,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Column {
                    SectionTitle("Your accounts", "New account", onAdd)
                    if (active.isEmpty()) {
                        EmptyPanel(Icons.Outlined.AccountBalanceWallet, "No active accounts")
                    } else {
                        AccountGroup(active, balances, money, onEdit)
                    }
                }
            }
            if (archived.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("Archived")
                        AccountGroup(archived, balances, money, onEdit)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountGroup(
    accounts: List<Account>,
    balances: Map<Long, Long>,
    money: Money,
    onEdit: (Account) -> Unit,
) {
    Panel {
        Column {
            accounts.forEachIndexed { index, account ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val current = balances.getValue(account.id)
                val owed = account.kind == AccountKind.CARD && current < 0
                Surface(
                    onClick = { onEdit(account) }, color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 67.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(account.kind.icon)
                        Column(Modifier.weight(1.35f)) {
                            Text(account.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (owed) "Credit card · Owed" else account.kind.label,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AmountText(
                            money.format(if (owed) -current else current), Modifier.weight(1f), alignment = TextAlign.End,
                            color = if (owed || current < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AccountEditor(
    account: Account?,
    money: Money,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSave: (Account) -> Unit,
    onArchive: (Account) -> Unit,
    onActivity: (Long) -> Unit,
) {
    var draft by rememberSaveable {
        mutableStateOf(
            account?.let {
                val opening = if (it.kind == AccountKind.CARD) -it.openingMinor else it.openingMinor
                AccountDraft(it.name, it.kind, money.input(opening))
            } ?: AccountDraft(),
        )
    }
    val saved = draft.account(money, account)
    EditorFrame(
        title = if (account == null) "New account" else "Edit account",
        busy = busy, error = error, action = if (account == null) "Create account" else "Save changes",
        canSave = saved != null, onClose = onClose, onSave = { saved?.let(onSave) },
    ) {
        AccountFields(draft, money) { draft = it }
        if (account != null) {
            EditorOption("View transactions", Icons.AutoMirrored.Outlined.ReceiptLong, !busy) { onActivity(account.id) }
            EditorOption(
                if (account.archived) "Restore account" else "Archive account",
                if (account.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                !busy,
            ) { onArchive(account) }
        }
    }
}
