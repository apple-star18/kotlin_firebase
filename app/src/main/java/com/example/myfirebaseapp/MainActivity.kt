package com.example.myfirebaseapp

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.PermissionStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

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
            MyFirebaseAppUI(
                auth = auth,
                googleSignInClient = googleSignInClient
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MyFirebaseAppUI(
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient
) {
    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Toast.makeText(context, "사진 찍기 성공", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "사진 찍기 실패", Toast.LENGTH_SHORT).show()
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(context, "Google 로그인 성공", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Firebase 인증 실패", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Google 로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Button(
                onClick = {
                    if (cameraPermissionState.status is PermissionStatus.Granted) {
                        val uri = createImageUri(context)
                        imageUri = uri
                        takePictureLauncher.launch(uri)
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Take a picture")
            }

            imageUri?.let { uri ->
                uploadImageToFirebaseStorage(uri)
            }

            Spacer(modifier = Modifier.height(32.dp))

            LoginScreen(
                onLogin = { email, password -> login(email, password, context, auth) },
                onRegister = { email, password -> register(email, password, context, auth) },
                onGoogleSignIn = {
                    val signInIntent = googleSignInClient.signInIntent
                    signInLauncher.launch(signInIntent)
                }
            )
        }
    }
}

fun uploadImageToFirebaseStorage(uri: Uri) {
    val storage = FirebaseStorage.getInstance()
    val storageRef: StorageReference = storage.reference
    val imagesRef = storageRef.child("user_faces/${System.currentTimeMillis()}.jpg")

    imagesRef.putFile(uri)
        .addOnSuccessListener { taskSnapshot ->
            // 업로드 성공 후 처리
            imagesRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                // 업로드된 이미지의 URL을 Firestore에 저장하거나 다른 작업 수행
                saveImageUrlToFirestore(downloadUrl.toString())
            }
        }
        .addOnFailureListener { exception ->
            // 업로드 실패 처리
            Log.e("Firebase", "Upload failed: ${exception.message}")
        }
}

fun saveImageUrlToFirestore(imageUrl: String) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val userRef = db.collection("users").document(userId)
    userRef.update("profileImageUrl", imageUrl)
        .addOnSuccessListener {
            // 성공적으로 URL을 Firestore에 저장한 후 처리
            Log.d("Firestore", "Image URL saved successfully")
        }
        .addOnFailureListener { exception ->
            // 실패 처리
            Log.e("Firestore", "Error saving image URL: ${exception.message}")
        }
}

private fun createImageUri(context: Context): Uri {
    val file = File(context.filesDir, "face_image.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

private fun login(email: String, password: String, context: Context, auth: FirebaseAuth) {
    if (email.isBlank() || password.isBlank()) {
        Toast.makeText(context, "이메일 또는 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
        return
    }

    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "로그인 성공", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "로그인 실패: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
}

private fun register(email: String, password: String, context: Context, auth: FirebaseAuth) {
    if (email.isBlank() || password.isBlank()) {
        Toast.makeText(context, "이메일 또는 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
        return
    }

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "회원가입 성공", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "회원가입 실패: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
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
        Button(onClick = { onLogin(email, password) }, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onRegister(email, password) }, modifier = Modifier.fillMaxWidth()) {
            Text("Sign up")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onGoogleSignIn, modifier = Modifier.fillMaxWidth()) {
            Text("Sign in with Google")
        }
    }
}
