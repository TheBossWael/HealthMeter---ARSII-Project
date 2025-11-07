package com.example.healthmeter.ui.InstructionsForCamScan

import android.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.healthmeter.Assets.Screen

@Composable
fun InstructionsMeasurementScreen(
    navController: NavController,
    onCancel: () -> Unit = {},
    onNext: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Instruction text
        Text(
            text = "Simply position yourself\nwithin the frame",
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            color = Color.Black
        )

        // Placeholder for illustration / frame
        Box(
            modifier = Modifier
                .size(340.dp)
                .border(
                    width = 1.5.dp,
                    color = Color(0xFF90A4AE),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.example.healthmeter.R.drawable.visage),
                contentDescription = "Illustration of a person with curly hair",
                modifier = Modifier
                    .fillMaxSize().padding(28.dp)
            )        }

        // Subtext
        Text(
            text = "We’ll measure your heart and respiratory rate by detecting small changes on your chest",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            TextButton(onClick = {
                onCancel()
                navController.navigate(Screen.HomeScreen.route) {
                    popUpTo(Screen.HomeScreen.route) { inclusive = true }
                }
            }) {
                Text("Cancel" , fontSize = 20.sp, color = Color(0xFF133a94) )
            }

            Button(
                modifier = Modifier.height(45.dp).width(110.dp),
                onClick = {
                    onNext()
                    navController.navigate(Screen.RppgScreen.route)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF133a94))
            ) {
                Text("Next", color = Color.White,fontSize = 18.sp)
            }
        }
    }
}
