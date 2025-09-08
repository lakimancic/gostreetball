package com.example.gostreetball.ui.screens.auth

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gostreetball.R
import com.example.gostreetball.ui.theme.GoStreetBallTheme

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    navigateToLogin: () -> Unit,
    navigateToRegister: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "logo",
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = navigateToRegister,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "SIGN UP",
                fontSize = 20.sp
            )
        }
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = navigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp)
                .height(60.dp)
        ) {
            Text(
                text = "SIGN IN",
                fontSize = 20.sp
            )
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
fun WelcomeScreenPreview() {
    GoStreetBallTheme {
        WelcomeScreen(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            navigateToLogin = {},
            navigateToRegister = {}
        )
    }
}