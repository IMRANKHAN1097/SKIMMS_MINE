package com.example.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineImageStudioScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var promptText by remember { mutableStateOf("Intrinsically safe mining emergency evacuation signboard showing illuminated green exit corridor and methane warning badge, high contrast vector graphics") }
    var editPromptText by remember { mutableStateOf("Add a bold red gas hazard skull icon to the top right corner") }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // gemini-nano-banana-2.1 with responseModalities = listOf(ResponseModality.IMAGE)
    val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mine Safety Visual Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = SafetyAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("gemini-nano-banana-2.1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafetyAmber, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text("Text-to-Image Generation & Interactive Editing for Mine Safety Assets", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("image_studio_back_button")) {
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
                            text = "CREATE MINE SAFETY VISUAL ASSET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafetyAmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Synthesize underground evacuation signs, DGMS warning placards, and helmet hazard decals with Gemini Nano Banana:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            label = { Text("Visual Prompt Description") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("image_prompt_input"),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SafetyAmber,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Prompt Presets
                        Text("Safety Presets:", fontSize = 10.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = promptText.contains("Methane"),
                                onClick = { promptText = "Methane gas explosive limit danger sign with yellow chevron border and digital sensor meter" },
                                label = { Text("Methane Alert", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = promptText.contains("Evacuation"),
                                onClick = { promptText = "Underground shaft refuge chamber arrow sign with neon green lighting" },
                                label = { Text("Refuge Chamber", fontSize = 10.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isGenerating = true
                                    errorMessage = null
                                    try {
                                        val response = withTimeout(60_000L) {
                                            imageModel.generateContent(promptText)
                                        }
                                        val part = response.candidates.firstOrNull()
                                            ?.content?.parts?.filterIsInstance<ImagePart>()
                                            ?.firstOrNull()
                                        if (part?.image != null) {
                                            generatedBitmap = part.image
                                        } else {
                                            errorMessage = "Model did not return an image part."
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ImageGen", "Failed generating image", e)
                                        errorMessage = e.localizedMessage ?: "Image synthesis timed out or failed."
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_image_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isGenerating && !isEditing && promptText.isNotBlank()
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Synthesizing Asset (30-60s)...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Safety Asset", fontWeight = FontWeight.Bold)
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

            if (generatedBitmap != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MineDarkSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("SYNTHESIZED SAFETY GRAPHIC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SensorCyan)
                            Spacer(modifier = Modifier.height(10.dp))

                            Image(
                                bitmap = generatedBitmap!!.asImageBitmap(),
                                contentDescription = "Generated Safety Visual",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive Image Editing with Gemini Nano Banana
                            Text("EDIT THIS SAFETY ASSET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafetyAmber)
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = editPromptText,
                                onValueChange = { editPromptText = it },
                                label = { Text("Edit Instruction") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val currentBmp = generatedBitmap ?: return@Button
                                    scope.launch {
                                        isEditing = true
                                        errorMessage = null
                                        try {
                                            val response = withTimeout(60_000L) {
                                                imageModel.generateContent(
                                                    content {
                                                        image(currentBmp)
                                                        text(editPromptText)
                                                    }
                                                )
                                            }
                                            val part = response.candidates.firstOrNull()
                                                ?.content?.parts?.filterIsInstance<ImagePart>()
                                                ?.firstOrNull()
                                            if (part?.image != null) {
                                                generatedBitmap = part.image
                                            } else {
                                                errorMessage = "Edit operation did not return an image."
                                            }
                                        } catch (e: Exception) {
                                            Log.e("ImageEdit", "Failed editing image", e)
                                            errorMessage = e.localizedMessage ?: "Image editing timed out or failed."
                                        } finally {
                                            isEditing = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SensorCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isGenerating && !isEditing && editPromptText.isNotBlank()
                            ) {
                                if (isEditing) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Applying Edits (30-60s)...")
                                } else {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Apply Edit to Asset", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
