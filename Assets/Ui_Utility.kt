package com.example.healthmeter.Assets

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthmeter.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Show a Toast message quickly. */
fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, message, duration).show()
}

/**
 * Layout with two stacked boxes:
 * - TopBox: bottom-rounded corners with gradient.
 * - BottomBox: top-rounded corners with gradient and padding.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BoxBackgroundLayout(
    modifier: Modifier = Modifier,
    topBoxHeightFraction: Float = 0.3f,
    bottomBoxPaddingFraction: Float = 0.24f,
    topBoxColors: List<Color> = listOf(Color(0xFF133a94), Color(0xFF7A78E4)),
    bottomBoxColors: List<Color> = listOf(Color(0xFFF6F8FA), Color(0xFFF6F8FA)),
    topContent: @Composable BoxScope.() -> Unit = {},
    bottomContent: @Composable BoxScope.() -> Unit = {},
) {

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val topBoxHeight = maxHeight * topBoxHeightFraction
        val bottomBoxHeight = maxHeight
        val bottomBoxPadding = maxHeight * bottomBoxPaddingFraction

        Box {
            // Top box with bottom rounded corners
            RoundedGradientBox(
                modifier = Modifier.height(topBoxHeight),
                roundTop = false,
                roundBottom = true,
                colors = topBoxColors
            ) {
                topContent()
            }

            // Bottom box with top rounded corners and top padding
            RoundedGradientBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomBoxHeight)
                    .padding(top = bottomBoxPadding),
                roundTop = true,
                roundBottom = false,
                colors = bottomBoxColors
            ) {
                bottomContent()
            }
        }
    }
}

/**
 * Box with vertical gradient and optional rounded corners.
 *
 * @param roundTop rounds top corners if true.
 * @param roundBottom rounds bottom corners if true.
 */
@Composable
fun RoundedGradientBox(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFF133a94), Color(0xFF7A78E4)),
    cornerRadius: Int = 32,
    roundTop: Boolean = false,
    roundBottom: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(
        topStart = if (roundTop) cornerRadius.dp else 0.dp,
        topEnd = if (roundTop) cornerRadius.dp else 0.dp,
        bottomStart = if (roundBottom) cornerRadius.dp else 0.dp,
        bottomEnd = if (roundBottom) cornerRadius.dp else 0.dp,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors), shape),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Composable function for a button that shows a spinner while loading after pressed.
 * Disables button during loading and delays onSubmit by default 1 second.
 */
@Composable
fun LoadingButtonWithSpinner(
    buttonText: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    spinnerColor: Color = Color(0xFF133a94),
    strokeWidth: Dp = 2.dp,
    isFormValid: Boolean = true,
    delayBeforeSubmit: Long = 3000L
) {
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = spinnerColor, strokeWidth = strokeWidth)
        }
    } else {
        Spacer(modifier = Modifier.height(10.dp))
        ContinueButton(
            text = buttonText,
            onClick = {
                isLoading = true
                coroutineScope.launch {
                    onSubmit()
                    delay(delayBeforeSubmit)
                    isLoading = false
                }
            },
            enabled = isFormValid,
            modifier = modifier
        )
    }
}

/**
 * Simple full-width button.
 * Background color changes with enabled state.
 */
@Composable
fun ContinueButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Color(0xFF133a94) else Color.LightGray,
            contentColor = Color.White
        )
    ) {
        Text(text)
    }
}
