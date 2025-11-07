package com.example.healthmeter.data.repository


import com.example.healthmeter.data.model.Measurement
import com.example.healthmeter.data.model.determineCondition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

val measurementDatabase = FirebaseDatabase.getInstance()
val usersReference = measurementDatabase.getReference("users")
val measurementAuth = FirebaseAuth.getInstance()

/**
 * Adds a new measurement for the current logged-in user.
 */
fun addMeasurement(
    measurement: Measurement,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val currentUser = measurementAuth.currentUser
    val username = currentUser?.displayName ?: return onFailure(Exception("User not logged in"))

    val userMeasurementsRef = usersReference.child(username).child("measurements")

    // Compute condition
    val condition = determineCondition(measurement.bpm, measurement.spo2)
    val finalMeasurement = measurement.copy(condition = condition)

    val newMeasurementRef = userMeasurementsRef.push()
    newMeasurementRef.setValue(finalMeasurement)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e) }
}


/**
 * Deletes a specific measurement by its ID (Firebase push key)
 */
fun deleteMeasurement(
    measurementId: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val currentUser = measurementAuth.currentUser
    val username = currentUser?.displayName ?: return onFailure(Exception("User not logged in"))

    val ref = usersReference.child(username).child("measurements").child(measurementId)
    ref.removeValue()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e) }
}

/**
 * Fetches the most recent measurement based on timestamp
 */
fun getLastMeasurement(
    onSuccess: (Measurement?) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val currentUser = measurementAuth.currentUser
    val username = currentUser?.displayName ?: return onFailure(Exception("User not logged in"))

    val ref = usersReference.child(username).child("measurements")

    ref.orderByChild("timestamp").limitToLast(1)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val measurement = snapshot.children.firstOrNull()
                        ?.getValue(Measurement::class.java)
                    onSuccess(measurement)
                } else {
                    onSuccess(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.toException())
            }
        })
}

/**
 * Fetches all measurements for the current user
 */
fun getAllMeasurements(
    onSuccess: (List<Pair<String, Measurement>>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val currentUser = measurementAuth.currentUser
    val username = currentUser?.displayName ?: return onFailure(Exception("User not logged in"))

    val ref = usersReference.child(username).child("measurements")

    ref.orderByChild("timestamp")
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Pair<String, Measurement>>()
                for (child in snapshot.children) {
                    val measurement = child.getValue(Measurement::class.java)
                    if (measurement != null) {
                        list.add(child.key!! to measurement)
                    }
                }
                onSuccess(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.toException())
            }
        })
}
