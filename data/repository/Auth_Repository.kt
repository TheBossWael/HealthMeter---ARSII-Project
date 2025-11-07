import android.content.Context
import androidx.navigation.NavHostController
import com.example.healthmeter.Assets.Screen
import com.example.healthmeter.Assets.showToast
import com.example.healthmeter.model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase


//firebase backend file

// Initialize Firebase needed variables
val database = FirebaseDatabase.getInstance()
val usersRef = database.getReference("users")
val auth = FirebaseAuth.getInstance()





fun checkUserLoggedIn(): Boolean {
    //if user logged in return true, else false
    return auth.currentUser != null
}


fun getUserByUsername(username: String): User? {
    //fetch user by username from database
    return null //placeholder, replace with actual database call
}


//fetches the user snapshot asynchronously and returns the result via a callback.
fun checkUserExists(username: String, onResult: (Boolean) -> Unit) {

    usersRef.child(username).get()
        .addOnSuccessListener { snapshot ->
            onResult(snapshot.exists()) //return true
        }
        .addOnFailureListener {
            onResult(false) // or return false
        }
}


fun GetCurrentUserName(): String? {
    //get current user name from firebase auth
    return auth.currentUser?.displayName

}


fun registerUser(user: User, context: Context, navController: NavHostController) {
    auth.createUserWithEmailAndPassword(user.email, user.password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseUser = auth.currentUser

                // Update FirebaseAuth user profile with displayName = username
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(user.username)
                    .build()

                firebaseUser?.updateProfile(profileUpdates)
                    ?.addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            // Remove password before saving user data
                            val userToSave = user.copy(password = "")

                            // Save user in your database
                            usersRef.child(user.username).setValue(userToSave)
                                .addOnSuccessListener {
                                    // Send verification email
                                    firebaseUser.sendEmailVerification()
                                    // Show message, logout and navigate
                                    showToast(context, "Registration successful, Please log in.")
                                    logoutUser(navController)
                                    navController.navigate(Screen.Login.route)
                                }
                                .addOnFailureListener { e ->
                                    showToast(context, "Failed to save user: ${e.message}")
                                }
                        } else {
                            showToast(context, "Failed to update user profile: ${updateTask.exception?.message}")
                        }
                    }
            } else {
                showToast(context, "Registration failed: ${task.exception?.message}")
            }
        }
}


fun loginUser(
    email: String,
    password: String,
    context: Context,
    navController: NavHostController,
    onSuccess: (() -> Unit)? = null,
    onFailure: ((Exception?) -> Unit)? = null
) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val currentUser = auth.currentUser
                if (currentUser?.isEmailVerified == true) {
                    showToast(context, "Login successful!")
                    //callback to decide navigation-graph switch
                    if (onSuccess != null) {
                        onSuccess()
                    } else {
                        // Default: navigate to HomeScreen
                        navController.navigate(Screen.HomeScreen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                    }
                } else {
                    showToast(context, "Please verify your email before logging in.")
                    auth.signOut()
                    onFailure?.invoke(null)
                }
            } else {
                showToast(context, "Login failed: ${task.exception?.message}")
                onFailure?.invoke(task.exception)
            }
        }
}


// logoutUser with caller can decide navigation-graph switch
fun logoutUser(
    navController: NavHostController,
    onComplete: (() -> Unit)? = null
) {
    auth.signOut()
    if (onComplete != null) {
        // caller will handle Navigation switching
        onComplete()
    } else {
        // Default : navigate to Welcome
        navController.navigate(Screen.Welcome.route) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
        }
    }
}


fun forgetPassword(email: String, context: Context) {

    auth.sendPasswordResetEmail(email)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast(context, "Password reset email sent successfully.")
            } else {
                showToast(context, "Failed to send reset email: ${task.exception?.message}")
            }
        }
}


fun deleteUser(user: User){}


fun updateUser(user: User, context: Context) {
    val ref = usersRef.child(user.username)

    ref.setValue(user)
        .addOnSuccessListener {
            showToast(context, "User updated successfully.")
        }
        .addOnFailureListener { e ->
            showToast(context, "Failed to update user: ${e.message}")
        }
}




// Fetches the current user's data from Firebase Realtime Database (defensive callback)
fun GetCurrentUser(callback: (User?) -> Unit) {
    val username = GetCurrentUserName()
    if (username.isNullOrBlank()) {
        callback(null)
        return
    }

    usersRef.child(username).get()
        .addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                callback(null)
            } else {
                callback(snapshot.getValue(User::class.java))
            }
        }
        .addOnFailureListener {
            callback(null)
        }
}



//get a password and check if it's correct for the current user
fun checkIfPasswordCorrect(password: String, onResult: (Boolean) -> Unit) {
    val user = auth.currentUser ?: return onResult(false)
    val email = user.email ?: return onResult(false)

    val credential = EmailAuthProvider.getCredential(email, password)

    user.reauthenticate(credential)
        .addOnSuccessListener { onResult(true) }
        .addOnFailureListener { onResult(false) }
}


fun changePass(newPassword: String,context: Context) {
    auth.currentUser?.updatePassword(newPassword)
        ?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast(context, "Password updated successfully.")
                logoutUser(navController = NavHostController(context))
            } else {
                showToast(context, "Failed to update password: ${task.exception?.message}")
            }
        }
}

fun verifyEmailChange(newEmail: String, user: User, context: Context) {
    auth.currentUser?.updateEmail(newEmail.trim())
        ?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Send verification email
                auth.currentUser?.sendEmailVerification()
                showToast(context, "Email updated. Please verify your new email.")

                // Update database only after sending verification
                val updatedUser = user.copy(email = newEmail.trim())
                updateUser(updatedUser, context)
            } else {
                showToast(context, "Failed to update email: ${task.exception?.message}")
            }
        }
}
















