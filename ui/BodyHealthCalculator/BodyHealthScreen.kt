package com.example.healthmeter.ui.BodyHealthCalculator

import GetCurrentUser
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.twotone.Cake
import androidx.compose.material.icons.twotone.FitnessCenter
import androidx.compose.material.icons.twotone.Height
import androidx.compose.material.icons.twotone.Wc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthmeter.model.Gender
import com.example.healthmeter.model.User
import kotlin.math.round

@Composable
fun BodyHealthScreen() {
    var user by remember { mutableStateOf<User?>(null) }
    var manualInput by remember { mutableStateOf(false) }

    // Fields for manual input
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.Male) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    var bmiResult by remember { mutableStateOf<Double?>(null) }
    var idealWeightResult by remember { mutableStateOf<Double?>(null) }
    var caloriesResult by remember { mutableStateOf<Double?>(null) }

    // Function to calculate results
    fun calculateResults(u: User) {
        bmiResult = BodyHealthCalculator.calculateBMI(u.weight, u.height.toDouble())
        idealWeightResult = BodyHealthCalculator.calculateIdealWeight(u.height.toDouble(), u.gender.name)
        val userData = UserHealthData(
            weightKg = u.weight,
            heightCm = u.height.toDouble(),
            age = u.age,
            gender = u.gender.name,
            activityLevel = 1.2
        )
        caloriesResult = BodyHealthCalculator.calculateDailyCalories(userData)
    }

    // Fetch current user from Firebase
    LaunchedEffect(Unit) {
        if (!manualInput) {
            GetCurrentUser { u ->
                user = u
                u?.let { calculateResults(it) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Body Health Calculator",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Switch mode button with icon
        Button(onClick = {
            manualInput = !manualInput
            if (!manualInput) {
                user?.let { calculateResults(it) }
            } else {
                bmiResult = null
                idealWeightResult = null
                caloriesResult = null
            }
        }) {
            Icon(
                imageVector = if (manualInput) Icons.Default.Person else Icons.Default.Calculate,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (manualInput) "Use My Profile" else "Enter Another Person's Data")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (manualInput) {
            // Manual input form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF6F8FA))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val roundedShape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it.take(3) },
                    label = { Text("Age") },
                    placeholder = { Text("Enter age") },
                    singleLine = true,
                    shape = roundedShape,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Cake, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it.take(4) },
                    label = { Text("Height (cm)") },
                    placeholder = { Text("Enter height") },
                    singleLine = true,
                    shape = roundedShape,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Height, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.take(6) },
                    label = { Text("Weight (kg)") },
                    placeholder = { Text("Enter weight") },
                    singleLine = true,
                    shape = roundedShape,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Gender dropdown like EditProfile
                var genderExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { genderExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                            .border(BorderStroke(1.dp, Color.Black), shape = roundedShape), // Black border
                        shape = roundedShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF6F8FA)
                         )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(imageVector = Icons.Default.Wc, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(20.dp))
                            Text("Gender: ${gender.name}", color = Color.Black)
                        }
                    }
                    DropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false
                        },
                        modifier = Modifier.width(330.dp)
                    ) {
                        Gender.values().forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g.name) },
                                onClick = {
                                    gender = g
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Calculate button like EditProfile
                Button(
                    onClick = {
                        val a = age.toIntOrNull()
                        val h = height.toDoubleOrNull()
                        val w = weight.toDoubleOrNull()
                        if (a != null && h != null && w != null) {
                            val manualUser = User(
                                username = "Manual",
                                email = "",
                                age = a,
                                gender = gender,
                                height = h.toInt(),
                                weight = w
                            )
                            calculateResults(manualUser)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = roundedShape
                ) {
                    Icon(imageVector = Icons.Filled.Calculate, contentDescription = "Calculate")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calculate")
                }
            }
        } else {
            if (user == null) {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display results using ResultCard
        bmiResult?.let { bmi -> ResultCard("BMI", bmi, getBMIEmoji(bmi)) }
        idealWeightResult?.let { w -> ResultCard("Ideal Weight (kg)", w, "⚖️") }
        caloriesResult?.let { c -> ResultCard("Daily Calories", c, "🔥") }
    }
}

// Card composable for displaying a result
@Composable
fun ResultCard(label: String, value: Double, emoji: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = getCardColor(label, value).copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            Text("$emoji ${round(value * 10) / 10}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}

// Color-coded BMI emoji
fun getBMIEmoji(bmi: Double): String {
    return when {
        bmi < 18.5 -> "⚠️ Underweight"
        bmi < 25 -> "💚 Healthy"
        bmi < 30 -> "🧡 Overweight"
        else -> "❤️‍🔥 Obese"
    }
}

// Card background color based on type/value
fun getCardColor(label: String, value: Double): androidx.compose.ui.graphics.Color {
    return when (label) {
        "BMI" -> when {
            value < 18.5 -> Color.Yellow
            value < 25 -> Color.Green
            value < 30 -> Color(0xFFFFA500) // Orange
            else -> Color.Red
        }
        "Ideal Weight (kg)" -> Color.Blue
        "Daily Calories" -> Color.Magenta
        else -> Color.Gray
    }
}

// Data class for calculation
data class UserHealthData(
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val gender: String,
    val activityLevel: Double = 1.2
)
