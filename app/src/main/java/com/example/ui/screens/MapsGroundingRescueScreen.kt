package com.example.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Tool
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsGroundingRescueScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var queryText by remember { mutableStateOf("Nearest emergency burn trauma center and mine rescue station to Raniganj Coalfield West Bengal with driving route") }
    var isLoading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // gemini-3.8-flash with Tool.googleMaps() per required metadata
    val mapsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleMaps())
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mine Emergency Rescue Dispatch", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = Color(0xFF4285F4).copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("Google Maps Grounded", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text("Live Hospital Routing • Rescue Stations • DGMS Regional Contact", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("maps_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MineDarkSurface)
            )
        },
        containerColor = MineDarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "REAL-TIME GEOSPATIAL EMERGENCY QUERY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafetyAmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Query grounded live Google Maps data for ambulance staging, hyperbaric oxygen therapy chambers, and mine rescue squads:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = queryText,
                            onValueChange = { queryText = it },
                            label = { Text("Emergency Routing Query") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("maps_query_input"),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4285F4),
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Query Presets
                        Text("Quick Mining Emergency Presets:", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = queryText.contains("Raniganj"),
                                onClick = { queryText = "Nearest emergency burn trauma center and mine rescue station to Raniganj Coalfield West Bengal" },
                                label = { Text("Raniganj Rescue", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = queryText.contains("Kolkata"),
                                onClick = { queryText = "Nearest super specialty hospital with burn ICU from University of Calcutta Rajabazar campus" },
                                label = { Text("CU Campus Route", fontSize = 10.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    resultText = null
                                    try {
                                        val response = withTimeout(45_000L) {
                                            mapsModel.generateContent(
                                                "You are the SKIMMS Mine Emergency Dispatch Officer. Provide real-world Google Maps grounded location names, addresses, emergency contacts, and evacuation routes for: $queryText"
                                            )
                                        }
                                        resultText = response.text ?: "No maps grounding data returned."
                                    } catch (e: Exception) {
                                        Log.e("MapsGrounding", "Query failed", e)
                                        errorMessage = e.localizedMessage ?: "Failed to query Google Maps Grounding."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("maps_search_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading && queryText.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grounding with Google Maps...")
                            } else {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Query Google Maps Grounding", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatusCritical.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = StatusCritical,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (resultText != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(BorderSubtle, Color(0xFF4285F4))))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grounded Google Maps Intelligence", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MineDarkBackground,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = resultText ?: "",
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    lineHeight = 19.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
