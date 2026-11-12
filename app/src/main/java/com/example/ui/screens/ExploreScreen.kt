package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.theme.CloukeCobalt
import com.example.ui.theme.CloukeEmerald
import com.example.ui.theme.CloukePurple

data class ExploreTemplate(
    val id: String,
    val title: String,
    val category: String,
    val language: String,
    val description: String,
    val codeStarter: String
)

@Composable
fun ExploreScreen(
    onCloneToColtef: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val templates = remember {
        listOf(
            ExploreTemplate(
                id = "tmpl_1",
                title = "Android Clean M3 Architecture",
                category = "Mobile App",
                language = "Kotlin",
                description = "Complete MVVM architecture template with StateFlow, Repository layer, and Material 3 design system.",
                codeStarter = """// Android Clean M3 Template
package com.clouke.starter

class AppRepository {
    fun fetchCatalog(): List<String> = listOf("Item 1", "Item 2")
}
"""
            ),
            ExploreTemplate(
                id = "tmpl_2",
                title = "Gemini Streaming Agent Runner",
                category = "AI Agents",
                language = "Kotlin",
                description = "Streaming inference worker template with coroutine cancellation and real-time chunk consumption.",
                codeStarter = """// Gemini Agent Template
suspend fun executeStreamingAgent(prompt: String) {
    println("Streaming agent starting for: " + prompt)
}
"""
            ),
            ExploreTemplate(
                id = "tmpl_3",
                title = "FastAPI Microservice Boilerplate",
                category = "Backend",
                language = "Python",
                description = "Lightweight Python REST API with Pydantic validation, CORS middleware, and structured logging.",
                codeStarter = """# FastAPI Starter
from typing import Dict

def health_check() -> Dict[str, str]:
    return {"status": "healthy", "service": "clouke-api"}
"""
            ),
            ExploreTemplate(
                id = "tmpl_4",
                title = "Firestore Zero-Trust Rules Auditor",
                category = "Security",
                language = "TypeScript",
                description = "Automated rules tester verifying unauthenticated denial and cross-tenant access isolation.",
                codeStarter = """// Security Rules Auditor
export function auditRules() {
    console.log("Auditing Firestore security boundaries...");
}
"""
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Explore Hero Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CLOUKE Ecosystem Explore",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Production starter templates & agent extensions ready to launch into Coltef workspace.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates) { tmpl ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().testTag("explore_card_${tmpl.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = tmpl.category,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = tmpl.language,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { onCloneToColtef(tmpl.title, tmpl.language, tmpl.codeStarter) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("clone_template_${tmpl.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open in Coltef", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = tmpl.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = tmpl.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
