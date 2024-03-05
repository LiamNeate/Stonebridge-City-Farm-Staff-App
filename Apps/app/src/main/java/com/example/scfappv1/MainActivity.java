package com.example.scfappv1;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    public final static String emailStr = "email";
    public final static String teamStr = "team";
    public final static String adminStr = "admin";
    public final static String fNameStr = "firstName";
    public final static String lNameStr = "lastName";
    public final static String usrEmail = "usrEmail";
    private static final String TAG = "Testing";
    private FirebaseAuth mAuth;
    private DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            if (isBiometricAvailable()){
                showBiometricPrompt();
            } else {
                String email = mAuth.getCurrentUser().getEmail();
                setIntent(email);
            }
        }
    }

    private boolean isBiometricAvailable(){
        BiometricManager biometricManager = BiometricManager.from(this);
        return biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
    }


    private void showBiometricPrompt(){
        Executor executor = getMainExecutor();
        FragmentActivity activity = this;
        MainActivity inst = this;

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){
                        super.onAuthenticationSucceeded(result);
                        //Handle authentication success
                        Toast.makeText(inst,"Logged in.", Toast.LENGTH_SHORT).show();
                        String email = mAuth.getCurrentUser().getEmail();
                        setIntent(email);
                    }

                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Use your fingerprint to authenticate")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    public void login(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(MainActivity.this, "Logged in!",
                                    Toast.LENGTH_SHORT).show();
                            setIntent(email);
                            //user has been signed in, use an intent to move to the next activity
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("MainActivity", "signInWithEmail:failure",
                                    task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed. Email or password may be incorrect.",
                                    Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    public void loginButtonClicked(View view){
        EditText email = findViewById(R.id.editTextTextEmailAddress);
        EditText password = findViewById(R.id.editTextTextPassword);

        String sEmail = email.getText().toString();
        String sPassword = password.getText().toString();

        login(sEmail, sPassword);
    }

    public void setIntent(String email){
        Intent intent = new Intent(MainActivity.this ,BottomNav.class);
        reference = FirebaseDatabase.getInstance().getReference("username");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("username").document(email);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()){
                        String fName = document.getString("firstName");
                        String lName = document.getString("lastName");
                        String team = document.getString("team");
                        Boolean admin = document.getBoolean("admin");
                        intent.putExtra(emailStr, email);
                        intent.putExtra(teamStr, team);
                        intent.putExtra(adminStr, admin);
                        intent.putExtra(fNameStr, fName);
                        intent.putExtra(lNameStr, lName);
                        intent.putExtra(usrEmail, "");
                        startActivity(intent);
                    }
                    else{
                        Log.d(TAG, "No such document");
                    }
                }else{
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }
}