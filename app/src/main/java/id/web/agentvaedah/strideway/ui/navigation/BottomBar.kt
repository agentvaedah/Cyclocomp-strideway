package id.web.agentvaedah.strideway.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import id.web.agentvaedah.strideway.ui.theme.Palette

private data class Tab(
    val route: String,
    val label: String,
    val outlined: ImageVector,
    val filled: ImageVector,
)

private val leftTabs = listOf(
    Tab(Routes.HOME, "Home", NavIcons.HomeOutlined, NavIcons.HomeFilled),
    Tab(Routes.MAPS, "Peta", NavIcons.MapOutlined, NavIcons.MapFilled),
)
private val rightTabs = listOf(
    Tab(Routes.TRAINING, "Latihan", NavIcons.TrainingOutlined, NavIcons.TrainingFilled),
    Tab(Routes.YOU, "You", NavIcons.YouOutlined, NavIcons.YouFilled),
)

/**
 * Four athletic navigation tabs flanking a prominent Record action. Active tabs swap to their
 * filled icon inside a subtle Signal pill so selection reads immediately without heavy bars.
 */
@Composable
fun BottomBar(currentRoute: String?, recording: Boolean, onSelect: (String) -> Unit) {
    Column(Modifier.background(Palette.Surface).navigationBarsPadding()) {
        HorizontalDivider(color = Palette.Outline.copy(alpha = 0.6f), thickness = 1.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leftTabs.forEach { TabItem(it, currentRoute == it.route, onSelect, Modifier.weight(1f)) }
            RecordTab(currentRoute == Routes.RECORD, recording, { onSelect(Routes.RECORD) }, Modifier.weight(1f))
            rightTabs.forEach { TabItem(it, currentRoute == it.route, onSelect, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, onSelect: (String) -> Unit, modifier: Modifier) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) Palette.Accent else Palette.TextMuted,
        animationSpec = tween(160),
        label = "tab-icon",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) Palette.Text else Palette.TextMuted,
        animationSpec = tween(160),
        label = "tab-label",
    )
    val pillBg by animateColorAsState(
        targetValue = if (selected) Palette.Accent.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(160),
        label = "tab-pill",
    )
    Column(
        modifier
            .height(68.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab.route) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .background(pillBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) tab.filled else tab.outlined,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = labelColor)
    }
}

@Composable
private fun RecordTab(selected: Boolean, recording: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .height(68.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(50.dp)
                .background(Palette.Accent.copy(alpha = if (selected || recording) 0.22f else 0.12f), CircleShape)
                .padding(4.dp)
                .background(Palette.Accent, CircleShape)
                .then(if (selected) Modifier.border(1.5.dp, Palette.Text, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (recording) {
                Box(Modifier.size(13.dp).background(Palette.OnAccent, RoundedCornerShape(3.dp)))
            } else {
                Box(
                    Modifier
                        .size(16.dp)
                        .border(2.5.dp, Palette.OnAccent, CircleShape)
                        .padding(4.dp)
                        .background(Palette.OnAccent, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            if (recording) "Merekam" else "Rekam",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected || recording) Palette.Accent else Palette.TextMuted,
        )
    }
}
