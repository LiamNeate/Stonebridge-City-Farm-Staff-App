package com.example.scfappv1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class HomePage extends AppCompatActivity {

    private FirebaseAuth mAuth;

    public FirebaseUser currentUser;

    public TextView todayTime;
    public TextView test;

    public String username;

    FirebaseDatabase rootNode;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        test = findViewById(R.id.textView2);
        Intent intent = getIntent();
        test.setText(intent.getStringExtra("username"));
        test.setText("TEST");
    }
/*
    public void logoutClicked(View view){
        mAuth.signOut();
        finish();
    }

    public void deleteClicked(View view){
        currentUser.delete();
        Toast.makeText(HomePage.this,"Account deleted", Toast.LENGTH_SHORT).show();
        mAuth.signOut();
        finish();
    }

    public void addClicked(View view){
        Intent intent = new Intent(HomePage.this ,AddUsr.class);
        startActivity(intent);
        //refreshPage();
    }

    public void refreshPage(){
        finish();
        startActivity(getIntent());
    }

 */
}