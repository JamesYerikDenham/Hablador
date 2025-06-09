@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.hablador

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        TextToSpeechScreen()
                    }
                }
            }
        }
    }
}

// 🔧 Función auxiliar para mostrar la voz seleccionada como texto legible
fun getVoiceLabel(voice: Voice?): String {
    return when {
        voice == null -> ""
        voice.locale.language == "es" && voice.locale.country == "ES" -> "España"
        voice.locale.language == "es" && voice.locale.country == "US" -> "Latinoamérica"
        voice.locale.language == "en" && voice.locale.country == "US" -> "Inglés (EE.UU.)"
        voice.locale.language == "en" && voice.locale.country == "GB" -> "Inglés (Reino Unido)"
        voice.locale.language == "fr" && voice.locale.country == "FR" -> "Francia"
        voice.locale.language == "fr" && voice.locale.country == "CA" -> "Francés canadiense"
        else -> "${voice.locale.displayLanguage} (${voice.locale.displayCountry})"
    }
}

@Composable
fun TextToSpeechScreen() {
    val context = LocalContext.current
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    var inputText by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(Locale("es", "ES")) }
    var selectedVoice by remember { mutableStateOf<Voice?>(null) }
    var availableVoices by remember { mutableStateOf<List<Voice>>(emptyList()) }

    // Inicialización de TTS
    DisposableEffect(selectedLanguage) {
        var ttsEngine: TextToSpeech? = null

        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine?.language = selectedLanguage

                val voices = ttsEngine?.voices?.filter {
                    when (selectedLanguage.language) {
                        "es" -> it.locale.language == "es" &&
                                (it.locale.country == "ES" || it.locale.country == "US" || it.locale.country == "MX")
                        "en" -> it.locale.language == "en" &&
                                (it.locale.country == "US" || it.locale.country == "GB")
                        "fr" -> it.locale.language == "fr" &&
                                (it.locale.country == "FR" || it.locale.country == "CA")
                        else -> it.locale == selectedLanguage
                    }
                } ?: emptyList()

                availableVoices = voices
                selectedVoice = voices.firstOrNull()
            }
        }

        ttsEngine = TextToSpeech(context, listener)
        tts.value = ttsEngine

        onDispose {
            ttsEngine?.stop()
            ttsEngine?.shutdown()
        }
    }

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize()) {

        // TextArea reducido
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Texto a leer") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 160.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Idiomas disponibles
        val languages = listOf(
            "Español" to Locale("es", "ES"),
            "Inglés" to Locale("en", "US"),
            "Francés" to Locale("fr", "FR")
        )
        var expandedLang by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expandedLang,
            onExpandedChange = { expandedLang = !expandedLang }
        ) {
            OutlinedTextField(
                value = languages.find { it.second.language == selectedLanguage.language }?.first ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Idioma") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLang) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedLang,
                onDismissRequest = { expandedLang = false }
            ) {
                languages.forEach { (label, locale) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            selectedLanguage = locale
                            tts.value?.language = locale
                            availableVoices = tts.value?.voices?.filter { it.locale == locale } ?: emptyList()
                            selectedVoice = availableVoices.firstOrNull()
                            expandedLang = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selector de voz (acento)
        var expandedVoice by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expandedVoice,
            onExpandedChange = { expandedVoice = !expandedVoice }
        ) {
            OutlinedTextField(
                value = getVoiceLabel(selectedVoice),
                onValueChange = {},
                readOnly = true,
                label = { Text("Voz (acento)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVoice) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedVoice,
                onDismissRequest = { expandedVoice = false }
            ) {
                availableVoices
                    .map { voice ->
                        val label = getVoiceLabel(voice)
                        label to voice
                    }
                    .distinctBy { it.first } // Eliminar duplicados por nombre
                    .forEach { (label, voice) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedVoice = voice
                                tts.value?.voice = voice
                                expandedVoice = false
                            }
                        )
                    }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Reproducir
        Button(
            onClick = {
                tts.value?.voice = selectedVoice
                tts.value?.speak(inputText, TextToSpeech.QUEUE_FLUSH, null, null)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reproducir")
        }
    }
}
