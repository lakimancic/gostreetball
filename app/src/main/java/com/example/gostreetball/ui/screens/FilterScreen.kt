package com.example.gostreetball.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gostreetball.Screens
import com.example.gostreetball.data.model.BoardType
import com.example.gostreetball.data.model.CourtType
import com.example.gostreetball.ui.CourtsViewModel
import com.example.gostreetball.ui.components.SortSegmentedControl

@Composable
fun FilterScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    val mainBackStackEntry = navController
        .currentBackStackEntryAsState()
        .value
        ?.let { navController.getBackStackEntry(Screens.MainScreen.name) }

    val viewModel: CourtsViewModel? = mainBackStackEntry?.let { hiltViewModel(it) }
    if (viewModel == null) return;

    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(16.dp),
    ) {
        Text(
            text = "Filter Courts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Court Type",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    color = MaterialTheme.colorScheme.onSurface
                )
                CourtType.entries.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.toggleTypeFilter(type) }
                            .padding(4.dp)
                    ) {
                        Checkbox(
                            checked = state.selectedTypes.contains(type),
                            onCheckedChange = { viewModel.toggleTypeFilter(type) }
                        )
                        Text(text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Board Type",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                BoardType.entries.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.toggleBoardTypeFilter(type) }
                            .padding(4.dp)
                    ) {
                        Checkbox(
                            checked = state.selectedBoardTypes.contains(type),
                            onCheckedChange = { viewModel.toggleBoardTypeFilter(type) }
                        )
                        Text(text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Sort By",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        SortSegmentedControl(
            selectedSort = state.sortKey,
            onSortSelected = { viewModel.setSortKey(it) }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Radius: ${state.radius}m",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.radius != null,
                    onCheckedChange = { viewModel.setRadius(if (it) 10 else null) }
                )
            }
            if (state.radius != null) {
                Slider(
                    value = state.radius!!.toFloat(),
                    onValueChange = { viewModel.setRadius(it.toInt()) },
                    valueRange = 10f..1000f
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    navController.popBackStack()
                    viewModel.fetchCourts()
                }
            ) { Text("Apply", style = MaterialTheme.typography.titleLarge) }
            OutlinedButton(
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                onClick = { viewModel.resetFilters() }
            ) { Text("Clear", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

//@Preview(showBackground = true, name = "Light Preview")
//@Preview(
//    showBackground = true,
//    uiMode = UI_MODE_NIGHT_YES,
//    name = "Dark Preview"
//)
//@Composable
//fun FilterScreenPreview() {
//    GoStreetBallTheme {
//        FilterScreen(
//            modifier = Modifier
//                .background(MaterialTheme.colorScheme.background),
//            navController = Nav
//        )
//    }
//}