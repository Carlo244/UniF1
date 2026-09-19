package com.example.unif1.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ColorSchemePreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Primary", color = MaterialTheme.colorScheme.onBackground)
        Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("Primary", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        
        Text("Secondary", color = MaterialTheme.colorScheme.onBackground)
        Surface(color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("Secondary", color = MaterialTheme.colorScheme.onSecondary)
            }
        }
        
        Text("Surface", color = MaterialTheme.colorScheme.onBackground)
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().height(40.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Box(contentAlignment = Alignment.Center) {
                Text("Surface", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Text("UniF1 Custom Colors", color = MaterialTheme.colorScheme.onBackground)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.weight(1f).height(40.dp).background(MaterialTheme.unif1Colors.run))
            Box(modifier = Modifier.weight(1f).height(40.dp).background(MaterialTheme.unif1Colors.bike))
            Box(modifier = Modifier.weight(1f).height(40.dp).background(MaterialTheme.unif1Colors.swim))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.weight(1f).height(40.dp).background(MaterialTheme.unif1Colors.ready))
            Box(modifier = Modifier.weight(1f).height(40.dp).background(MaterialTheme.unif1Colors.caution))
            Box(modifier = Modifier.weight(1f).height(40.dp).background(MaterialTheme.unif1Colors.recovery))
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun LightThemePreview() {
    UniF1Theme(appTheme = AppTheme.LIGHT) {
        ColorSchemePreview()
    }
}

@Preview(showBackground = true, name = "Dark Mode", backgroundColor = 0xFF121212)
@Composable
fun DarkThemePreview() {
    UniF1Theme(appTheme = AppTheme.DARK) {
        ColorSchemePreview()
    }
}
