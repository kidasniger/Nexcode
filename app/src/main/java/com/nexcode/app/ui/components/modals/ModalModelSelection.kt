package com.nexcode.app.ui.components.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.nexcode.app.ui.theme.JetBrainsMonoFontFamily
import com.nexcode.app.ui.theme.NexcodeTheme
import com.nexcode.app.ui.theme.PillShape

data class AiModelItem(
    val id: String,
    val name: String,
    val description: String
)

val DEFAULT_AI_MODELS = listOf(
    AiModelItem("claude-sonnet-4.5", "claude-sonnet-4.5", "Anthropic · équilibré, rapide"),
    AiModelItem("claude-opus-4.1", "claude-opus-4.1", "Anthropic · refactorisations lourdes"),
    AiModelItem("claude-haiku-4.5", "claude-haiku-4.5", "Anthropic · rapide, économique"),
    AiModelItem("gpt-4o", "gpt-4o", "OpenAI · raisonnement et code"),
    AiModelItem("gpt-4o-mini", "gpt-4o-mini", "OpenAI · rapide via clé personnelle"),
    AiModelItem("qwen2.5-coder:7b", "qwen2.5-coder:7b", "Serveur personnalisé · local Ollama")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalModelSelection(
    selectedModel: String = "claude-sonnet-4.5",
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val colors = NexcodeTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("modal_m_model"),
        containerColor = colors.surface,
        contentColor = colors.txt,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            // Poignée de tirage .grab (36x4 dp)
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(PillShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            // Titre h3
            Text(
                text = "Choisir un modèle",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.txt,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            // Sous-titre .ssub
            Text(
                text = "Les requêtes sont facturées sur votre propre clé API. Le modèle peut être changé à tout moment.",
                fontSize = 12.sp,
                color = colors.txt2,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 12.dp)
            )

            // 4 lignes de modèles
            DEFAULT_AI_MODELS.forEach { model ->
                val isSelected = model.name == selectedModel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable {
                            onModelSelected(model.name)
                            onDismiss()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .testTag("m_model_item_${model.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // li-ic
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_spark),
                            contentDescription = null,
                            tint = if (isSelected) colors.accent else colors.txt2,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = model.name,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.txt
                        )
                        Text(
                            text = model.description,
                            fontSize = 11.5.sp,
                            color = colors.txt2,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_c),
                        contentDescription = null,
                        tint = if (isSelected) colors.accent else colors.txt2.copy(alpha = 0.25f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
