package com.example.gostreetball.ui.screens

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gostreetball.ui.AddReviewViewModel
import com.example.gostreetball.ui.theme.GoStreetBallTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: AddReviewViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
    itemId: String,
    isForCourt: Boolean
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect {
            navigateBack()
        }
    }

    LaunchedEffect(itemId, isForCourt) {
        viewModel.fetchForUpdate(itemId, isForCourt)
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${if (state.isUpdate) "Update" else "Add New"} ${if (isForCourt) "Court" else "Judge"} Review",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBackIosNew,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(horizontal = 30.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.error != null) {
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Rating:",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            StarRatingBar(
                rating = state.rating,
                onRatingChanged = { viewModel.updateRating(it) }
            )
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(
                value = state.reviewText,
                onValueChange = { viewModel.updateReviewText(it) },
                label = { Text("Write your review") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                singleLine = false,
                maxLines = 6,
                textStyle = LocalTextStyle.current.copy(lineHeight = 20.sp)
            )
            Spacer(modifier = Modifier.height(30.dp))

            Button(onClick = {
                viewModel.addReview(itemId, isForCourt)
            }) {
                Text("Submit Review")
            }
        }
    }
}

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { index ->
            IconButton(
                onClick = { onRatingChanged(index) }
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star $index",
                    tint = if (index <= rating) Color.Yellow else Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Preview")
@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "Dark Preview"
)
@Composable
fun AddReviewScreenPreview() {
    GoStreetBallTheme {
        AddReviewScreen(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            itemId = "",
            navigateBack = {},
            isForCourt = true
        )
    }
}