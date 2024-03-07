package com.example.scfappv1;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.scfappv1.databinding.ActivityBottomNavBinding;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BottomNav extends AppCompatActivity {

    private ActivityBottomNavBinding binding;
    private FirebaseAuth mAuth;
    public FirebaseUser currentUser;
    public NavController navController;
    public BottomNavigationView navView;
    public MaterialButton clockBtn;
    public TextView topText;
    public NestedScrollView currView;
    public LinearLayoutCompat nfcView;
    private static final String TAG = "TestingMessage";

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writingTagFilters[];
    Intent oldIntent;
    Boolean onClockPage = false;
    Boolean isClockedIn = false;
    Tag myTag;
    Context context;
    TextView tvNFCContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //Setting up notifs from the lab
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(BottomNav.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(BottomNav.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        CharSequence name = "Notification channel";
        String description = "Notification description";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel("app_channel", name, importance);
        channel.setDescription(description);

        //Register channel with system
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        //Setting up firebase notifs
        FirebaseMessaging.getInstance().subscribeToTopic("all");
        getFirebaseCloudMessagingToken();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { 
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(BottomNav.this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BottomNav.this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS},101);
                } 
            }
            NotificationChannel notifsChannel = new NotificationChannel("firebase", "Firebase channel", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager =
                    getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notifsChannel);
        }

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        binding = ActivityBottomNavBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        oldIntent = getIntent();

        navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_home)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_bottom_nav);

        NavigationUI.setupWithNavController(binding.navView, navController);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        //Initialising the nfc reader
        setUpNFC();

        verifyStoragePermissions(this);

    }

    //From the notifications lab
    public void handleNotifications(String title, String desc){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "app_channel")
                .setSmallIcon(R.drawable.scf_logo)
                .setContentTitle(title)
                .setContentText(desc)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
            return;
        } else {
            notificationManagerCompat.notify(1, builder.build());
        }
    }

    //Firebase notifs from lab
    private void getFirebaseCloudMessagingToken(){
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null){
                        String token = task.getResult();
                        Log.d ("FCM Token", "Token: "+token);
                    } else {
                        Log.e("FCM Token", "Failed to get token");
                    }
                });
    }

    //Setting up the NFC functionality of the home page
    //Done here as need access to intents
    public void setUpNFC() {
        //Setting up NFC
        //Getting the button
        clockBtn = findViewById(R.id.clockInOutBtn);
        //Getting the cancel button
        MaterialButton cancelClock = findViewById(R.id.cancelClockBtn);

        //Getting the top text
        topText = findViewById(R.id.homeText);

        //Getting the current scroll view
        currView = findViewById(R.id.mainScroll);

        //Getting invisible nfc tag reader view
        nfcView = findViewById(R.id.nfcView);

        //Getting textview for display nfc text --TESTING ONLY--
        tvNFCContent = this.findViewById(R.id.tvNFCContent);

        //Setting context for the NFC class
        context = this;

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            clockBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(context, "This device does not support NFC", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            //Adding on click to open the nfc reader and remove current view
            clockBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //NFC code
                    tvNFCContent.setText("");
                    reset();
                }
            });
            readFromIntent(getIntent());
            pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
            writingTagFilters = new IntentFilter[]{tagDetected};
            //Adding on click to cancel button to get the original view back
            cancelClock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    reset();
                }
            });
        }
    }

    //Resetting the view and switching the boolean value.
    private void reset(){
        if(nfcView.getVisibility()==View.VISIBLE){
            nfcView.setVisibility(View.INVISIBLE);
            currView.setVisibility(View.VISIBLE);
            clockBtn.setVisibility(View.VISIBLE);
            topText.setVisibility(View.VISIBLE);
            onClockPage = false;
        } else {
            nfcView.setVisibility(View.VISIBLE);
            currView.setVisibility(View.INVISIBLE);
            clockBtn.setVisibility(View.INVISIBLE);
            topText.setVisibility(View.INVISIBLE);
            onClockPage = true;
        }
    }

    private void clockUser(){
        //Connecting to the db
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //Getting the user email
        Intent intent = getIntent();
        String email = intent.getStringExtra("email");


        //Getting the current date
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();

        //Formatting the date to match the database
        SimpleDateFormat df = new SimpleDateFormat("dd:MM:yyyy");
        String date = df.format(today).toString();

        DocumentReference docRef = db.collection("username").document(email);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()){
                        isClockedIn = doc.getBoolean("clockedIn");
                    }
                }
            }
        });

        //Setting whether the user is getting clocked out or clocked in
        if (isClockedIn){
            date = date + "(out)";
        } else {
            date = date + "(in)";
        }


        //Creating the map and current timestamp
        Map<String, Object> newClockInfo = new HashMap<>();
        Date d = new Date();
        df = new SimpleDateFormat("dd:MM:yyyy HH:mm");
        String test = df.format(d).toString();
        Log.d(TAG, "TIME: " + test);
        Timestamp currTimestamp = new Timestamp(d);
        newClockInfo.put(email, currTimestamp);

        docRef = db.collection("clocks").document(date);
        String finalDate = date;
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    FieldPath field = FieldPath.of(email);
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()){
                        //updating if there is already an entry the new clock to the db
                        db.collection("clocks").document(finalDate)
                                .update(field, currTimestamp)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "Clocked!");
                                    }
                                });
                    } else {
                        //Creating the entry if there is none
                        db.collection("clocks").document(finalDate)
                                .set(newClockInfo)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "Clocked!");
                                    }
                                });
                    }
                }
            }
        });

        //Updating clocked in info for the user
        db.collection("username").document(email)
                .update("clockedIn", !isClockedIn)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (!isClockedIn){
                            Toast.makeText(context, "Clocked out!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Clocked in!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //QR FUNCTIONS - MAINLY TAKEN FROM YOUTUBE VIDEO

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        //Handle item switch
        if (item.getItemId() == R.id.logout){
            mAuth.signOut();
            finish();
            return true;
        }
        else if (item.getItemId() == R.id.account_menu){
            //Check to make sure we are not already on that page
            int id = navController.getCurrentDestination().getId();
            if (id != R.id.navigation_account) {
                //Changing fragment to the account fragment
                navController.navigate(R.id.navigation_account);

            }
            return true;
        }
        else if (item.getItemId() == R.id.team_menu){
            int id = navController.getCurrentDestination().getId();
            if (id != R.id.navigation_team) {
                //Changing fragment to the account fragment
                navController.navigate(R.id.navigation_team);

            }
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    //NFC STUFF
    public void readFromIntent(Intent intentNFC){
        String action = intentNFC.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intentNFC.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null){
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++){
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }

    private void buildTagViews(NdefMessage[] msgs){
        if (msgs == null || msgs.length == 0) return;

        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;

        try{
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (Exception e){
            Log.e("UnsupportedEncoding", e.toString());
        }
        setIntent(oldIntent);
        if (text.contains("SCFClockingSystem") && onClockPage){
            clockUser();
            reset();
        }
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null){
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, writingTagFilters, null);
        }
    }

    @Override
    protected void onNewIntent(Intent intentNFC){
        super.onNewIntent(intentNFC);
        setIntent(intentNFC);
        readFromIntent(intentNFC);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intentNFC.getAction())){
            myTag = intentNFC.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }
}