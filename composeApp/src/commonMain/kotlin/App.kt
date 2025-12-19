import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import core.Navigation

@Composable
fun App() {
    AppContent()
}

// APP START
@Composable
fun AppContent() {
    var showContent by remember { mutableStateOf(false) }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (showContent) Arrangement.Top else Arrangement.Center
        ) {
            // Animate the button OUT when showContent = true
            AnimatedVisibility(visible = !showContent) {
                Button(
                    onClick = { showContent = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Let’s go!", style = MaterialTheme.typography.titleLarge)
                }
            }

            // Animate the content IN when showContent = true
            AnimatedVisibility(visible = showContent) {
                Navigation()
            }
        }
    }
}
