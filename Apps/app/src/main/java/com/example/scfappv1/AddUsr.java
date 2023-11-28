package com.example.scfappv1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class AddUsr extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth mAuth2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_usr);

        //mAuth = FirebaseAuth.getInstance();
        FirebaseOptions firebaseOptions = new FirebaseOptions.Builder()
                .setDatabaseUrl("https://console.firebase.google.com/project/scf-database/authentication/users")
                .setApiKey("AIzaSyCShvJKXz_GZq8haoONuYyaCOqURw_VrXQ")
                .setApplicationId("scf-database").build();

        try { FirebaseApp myApp = FirebaseApp.initializeApp(getApplicationContext(), firebaseOptions, "TempApp");
            mAuth2 = FirebaseAuth.getInstance(myApp);
        } catch (IllegalStateException e){
            mAuth2 = FirebaseAuth.getInstance(FirebaseApp.getInstance("TempApp"));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        /*
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            Intent intent = new Intent(AddUsr.this ,HomePage.class);
            startActivity(intent);
        }
         */
    }

    public void signup(String email, String password, String mName){
        mAuth2.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult>
                                                   task) {
                        if (task.isSuccessful()) {
                            Log.d("MainActivity",
                                    "createUserWithEmail:success");
                            FirebaseUser user = mAuth2.getCurrentUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(mName).build();
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(AddUsr.this,
                                                        "User created",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                            finish();
                            //user has been signed in, use an intent to move to the next activity
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("MainActivity",
                                    "createUserWithEmail:failure", task.getException());
                            if (password.length() < 6){
                                Toast.makeText(AddUsr.this,
                                        "Authentication failed. Password needs to be at least 6 characters.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(AddUsr.this,
                                        "Authentication failed. User may already exist.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    public void signupButtonClicked(View view){
        EditText email = findViewById(R.id.editTextTextEmailAddress);
        EditText password = findViewById(R.id.editTextTextPassword);
        EditText username = findViewById(R.id.editTextTextUsername);

        String sEmail = email.getText().toString();
        String sPassword = password.getText().toString();
        String mName = username.getText().toString();

        signup(sEmail, sPassword, mName);
    }
}