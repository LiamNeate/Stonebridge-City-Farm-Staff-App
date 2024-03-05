package com.example.scfappv1.ui.addUser;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.scfappv1.AddUsr;
import com.example.scfappv1.R;
import com.example.scfappv1.databinding.FragmentAddUserBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddUserFragment extends Fragment {

    private FragmentAddUserBinding binding;
    public FirebaseAuth mAuth;
    private static final String TAG = "TestingMessage";
    public TextInputEditText firstName;
    public TextInputEditText secondName;
    public TextInputEditText emailField;
    public TextInputEditText passwordField;
    public TextInputEditText confPasswordField;
    public AutoCompleteTextView rolesView;
    public AutoCompleteTextView teamsView;
    public DatePicker dobPicker;
    public boolean isVisiting;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AddUserViewModel AddUserViewModel =
                new ViewModelProvider(this).get(AddUserViewModel.class);

        binding = FragmentAddUserBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //Getting all items from the page
        firstName = getView().findViewById(R.id.addFirstName);
        secondName = getView().findViewById(R.id.addSurnameName);
        emailField = getView().findViewById(R.id.addEmail);
        passwordField = getView().findViewById(R.id.addPassword);
        confPasswordField = getView().findViewById(R.id.addConfPassword);
        teamsView = getView().findViewById(R.id.selectTeam);
        rolesView = getView().findViewById(R.id.selectRole);
        dobPicker = getView().findViewById(R.id.datePicker);

        //Setting max date as today
        dobPicker.setMaxDate(System.currentTimeMillis() - 1000);
        //Creating string arrays for options of staff and role
        //These are hard coded but would like to make these database defined in later iterations.
        String[] teamsArr = new String[] {
                "Barn", "Cafe", "Shop"
        };
        String[] rolesArr = new String[] {
                "Manager", "Staff", "Volunteer"
        };

        //Setting array adapters of the string arrays and then setting them to the drop down menus
        ArrayAdapter<String> rolesAdapter = new ArrayAdapter<>(getActivity(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, rolesArr);
        rolesView.setAdapter(rolesAdapter);
        rolesView.setThreshold(100);
        ArrayAdapter<String> teamsAdapter = new ArrayAdapter<>(getActivity(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, teamsArr);
        teamsView.setAdapter(teamsAdapter);
        teamsView.setThreshold(100);

        //Getting the create user button
        Button createUsr = getView().findViewById(R.id.createUsrBtnConf);

        //Set onClick for create user button
        createUsr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //checkFields()
                submitFields();
            }
        });

        //Checking if the user wants to edit an existing user
        Intent intent = getActivity().getIntent();
        String userEmail = intent.getStringExtra("usrEmail");
        if(!userEmail.equals("")){
            // Create a storage reference from our app
            intent.putExtra("usrEmail", "");
            isVisiting = true;
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference users = db.collection("username").document(userEmail);
            users.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        //Setting the text fields with the existing information
                        firstName.setText(document.getString("firstName"));
                        secondName.setText(document.getString("lastName"));
                        TextInputLayout emailLayout = getActivity().findViewById(R.id.addEmailLayout);
                        emailLayout.setHint(document.getId());
                        teamsView.setText(document.getString("team"));
                        rolesView.setText(document.getString("role"));

                        //Set password field as disabled
                        emailField.setEnabled(false);
                        passwordField.setEnabled(false);
                        confPasswordField.setEnabled(false);
                        emailField.setBackgroundColor(getResources().getColor(R.color.button_greyed_out));
                        passwordField.setBackgroundColor(getResources().getColor(R.color.button_greyed_out));
                        confPasswordField.setBackgroundColor(getResources().getColor(R.color.button_greyed_out));

                        Button update = getActivity().findViewById(R.id.createUsrBtnConf);
                        update.setText("Update account");

                        Timestamp dateTS = document.getTimestamp("DOB");
                        Date date = new Date(dateTS.toDate().getTime());
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String dateNoChanged = sdf.format(date);
                        int day = Integer.valueOf(dateNoChanged.substring(0,2));
                        int month = Integer.valueOf(dateNoChanged.substring(3,5));
                        int year = Integer.valueOf(dateNoChanged.substring(6,10));
                        dobPicker.updateDate(year, month-1, day);
                    }
                }
            });
        }
    }

    public void submitFields(){

        //Connecting to the authentication database
        FirebaseOptions firebaseOptions = new FirebaseOptions.Builder()
                .setDatabaseUrl("https://console.firebase.google.com/project/scf-database/authentication/users")
                .setApiKey("AIzaSyCShvJKXz_GZq8haoONuYyaCOqURw_VrXQ")
                .setApplicationId("scf-database").build();

        try { FirebaseApp myApp = FirebaseApp.initializeApp(getContext().getApplicationContext(), firebaseOptions, "TempApp");
            mAuth = FirebaseAuth.getInstance(myApp);
        } catch (IllegalStateException e){
            mAuth = FirebaseAuth.getInstance(FirebaseApp.getInstance("TempApp"));
        }

        //Converting to string
        String fName = firstName.getEditableText().toString();
        String sName = secondName.getEditableText().toString();
        String email = emailField.getEditableText().toString();
        String password = passwordField.getEditableText().toString();
        String confPassword = confPasswordField.getEditableText().toString();
        String role = rolesView.getEditableText().toString();
        String team = teamsView.getEditableText().toString();

        //Checking they are not empty and the email is valid
        if (TextUtils.isEmpty(fName)) {
            Toast.makeText(getActivity(), "Please enter a first name.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(sName)){
            Toast.makeText(getActivity(), "Please enter a second name.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(email) && !isVisiting){
            Toast.makeText(getActivity(), "Please enter an email address.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(role)){
            Toast.makeText(getActivity(), "Please select a role.", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(team)){
            Toast.makeText(getActivity(), "Please select a team.", Toast.LENGTH_SHORT).show();
        } else if ((!email.contains("@") || !email.contains(".")) && !isVisiting){
            Toast.makeText(getActivity(), "Please enter a valid email.", Toast.LENGTH_SHORT).show();
        }
        else if (!password.equals(confPassword) && !isVisiting){
            Toast.makeText(getActivity(), "Passwords do not match.", Toast.LENGTH_SHORT).show();
            Toast.makeText(getActivity(), password +":"+confPassword, Toast.LENGTH_SHORT).show();
        }
        else {
            Log.d(TAG, "Name:" + fName + " " + sName + " Email:" + email + " Role:" + role + " Team:" + team);
            //Formatting the date
            int dobDay = dobPicker.getDayOfMonth();
            int dobMonth = dobPicker.getMonth();
            int dobYear = dobPicker.getYear();

            Timestamp timestamp;

            //Setting date to a string the converting to a date.
            //Wrapping in a try catch as error could be thrown due to null date.
            String dobDate = dobYear + "/" + dobMonth + "/" + dobDay + " 00:00:00";
            Log.w(TAG, "DOB TEST: "+dobDate);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            try {
                Date date = sdf.parse(dobDate);
                timestamp = new Timestamp(date);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            //Starting the database connection
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            //Creating a map to hold all the new database items
            Map<String, Object> newUserMap = new HashMap<>();

            //Setting items to the map
            newUserMap.put("DOB", timestamp);

            //Checking if user has admin privileges or not
            if (role.contains("Manager")) {
                newUserMap.put("admin", true);
            } else {
                newUserMap.put("admin", false);
            }
            newUserMap.put("firstName", fName);
            newUserMap.put("lastName", sName);
            newUserMap.put("role", role);
            newUserMap.put("team", team);
            newUserMap.put("username", email);
            newUserMap.put("clockedIn", false);
            newUserMap.put("deleted", false);

            //Adding to db
            db.collection("username").document(email)
                    .set(newUserMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "New user added!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing doc ", e);
                        }
                    });

            //Setting up the shift patterns
            //Initially setting to all off
            //Creating a new map for the times
            Map<String, Object> newTimesMap = new HashMap<>();

            //Creating a temp map to create the document folder
            Map<String, Object> newTempTimesMap = new HashMap<>();
            //Creating a basic entry
            newTempTimesMap.put("exists", true);
            //Creating the document folder
            db.collection("shiftPattern").document(email)
                    .set(newTempTimesMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "Temp added successfully");
                        }
                    }) .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Temp not added successfully");
                        }
                    });

            //Reusing dob as the time does not matter here
            newTimesMap.put("end", timestamp);
            newTimesMap.put("start", timestamp);
            newTimesMap.put("off", true);

            //Setting each day
            String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

            //Looping through the days array and setting each day
            for (String day : days){
                db.collection("shiftPattern").document(email).collection(day).document("times")
                        .set(newTimesMap)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, day+" added successfully");
                            }
                        }) .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, day+" not added successfully");
                            }
                        });
            }

            //Adding auth to authentication database
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult>
                                                       task) {
                            if (task.isSuccessful()) {
                                Log.d("MainActivity",
                                        "createUserWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().build();
                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(getActivity(),
                                                            "User created",
                                                            Toast.LENGTH_SHORT).show();
                                                    getActivity().onBackPressed();
                                                }
                                            }
                                        });
                                //user has been signed in, use an intent to move to the next activity
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG,
                                        "createUserWithEmail:failure", task.getException());
                                if (password.length() < 6) {
                                    Toast.makeText(getActivity(),
                                            "Authentication failed. Password needs to be at least 6 characters.",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(),
                                            "Authentication failed. User may already exist.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}