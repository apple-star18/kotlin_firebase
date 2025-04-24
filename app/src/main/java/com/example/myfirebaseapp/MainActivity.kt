package com.example.myfirebaseapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.activity.compose.rememberLauncherForActivityResult

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            var context = this
            val signInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    firebaseAuthWithGoogle(account.idToken!!, context)
                } catch (e: Exception) {
                    Toast.makeText(context, "Google 로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            LoginScreen(
                onLogin = { email, password -> login(email, password) },
                onRegister = { email, password -> register(email, password) },
                onGoogleSignIn = {
                    val signInIntent = googleSignInClient.signInIntent
                    signInLauncher.launch(signInIntent)
                }
            )
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String, context: ComponentActivity) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(context) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Google 로그인 성공", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Firebase 인증 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onLogin(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onRegister(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign up")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign in with Google")
        }
    }
}