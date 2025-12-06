package com.example.testme.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.testme.ui.theme.BrandCyan50
import com.example.testme.ui.theme.BrandGreen50
import com.example.testme.ui.theme.UIBorder
import com.example.testme.ui.theme.UIPrimary
import com.example.testme.ui.theme.UISecondary

@Composable
fun TestMeBackground(
    content: @Composable BoxScope.() -> Unit
) {
    // 새로운 스타일 배경 (그라데이션)
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(BrandGreen50, BrandCyan50),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        content()
    }
}

// 롤백: 기존의 SoftBlobBackground 구현 복구
@Composable
fun SoftBlobBackground() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Blob 1 (Green)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFB3F6A5).copy(alpha = 0.55f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.3f, size.height * 0.35f),
                radius = size.minDimension * 0.6f
            ),
            center = Offset(size.width * 0.3f, size.height * 0.35f),
            radius = size.minDimension * 0.6f
        )

        // Blob 2 (Mint Blue)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFA5F6E8).copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.8f, size.height * 0.25f),
                radius = size.minDimension * 0.5f
            ),
            center = Offset(size.width * 0.8f, size.height * 0.25f),
            radius = size.minDimension * 0.5f
        )

        // Blob 3 (Light Teal)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFAEEBFF).copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.15f, size.height * 0.75f),
                radius = size.minDimension * 0.55f
            ),
            center = Offset(size.width * 0.15f, size.height * 0.75f),
            radius = size.minDimension * 0.55f
        )

        // Center light glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                radius = size.minDimension * 0.7f
            ),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            radius = size.minDimension * 0.7f
        )
    }
}

@Composable
fun TestMeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = Color.White,
    hasBorder: Boolean = true,
    content: @Composable () -> Unit
) {
    val border = if (hasBorder) BorderStroke(1.dp, UIBorder) else null
    // clickable modifier moved inside Card to ensure ripple effect works correctly with shape
    // But Card composable handles onClick directly if provided in newer APIs or via overload.
    // Standard Card with onClick:
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(20.dp), // Increased for trendy look
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Soft elevation
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            content()
        }
    }
}

@Composable
fun TestMeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    enabled: Boolean = true
) {
    val containerColor = if (isSecondary) UISecondary else UIPrimary
    val contentColor = if (isSecondary) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary

    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // More rounded
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = if (isSecondary) UISecondary.copy(alpha = 0.5f) else UIPrimary.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        enabled = enabled
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun TestMeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, color = Color.Gray) } },
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UIPrimary,
            unfocusedBorderColor = UIBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
fun TestMeTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = color
    )
}

@Composable
fun <T> TestMeDropdown(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T?) -> Unit,
    itemLabel: (T) -> String,
    placeholder: String = "Select",
    modifier: Modifier = Modifier,
    showClearOption: Boolean = false,
    clearOptionLabel: String = "전체"
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedItem?.let(itemLabel) ?: placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (selectedItem != null) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown",
                    tint = Color.Gray
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            if (showClearOption) {
                DropdownMenuItem(
                    text = { Text(clearOptionLabel) },
                    onClick = {
                        onItemSelected(null)
                        expanded = false
                    }
                )
            }
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
