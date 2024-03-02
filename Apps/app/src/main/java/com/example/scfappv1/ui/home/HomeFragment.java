package com.example.scfappv1.ui.home;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.scfappv1.BottomNav;
import com.example.scfappv1.HomePage;
import com.example.scfappv1.R;
import com.example.scfappv1.databinding.FragmentHomeBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FirebaseAuth mAuth;
    public FirebaseUser currentUser;
    private static final String TAG = "TestingMessage";


private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){

        //Attempting to set up the nfc reader
        //Wrapped in a try as it does not work on first load, but the call in BottomActivity does
        try{
            ((BottomNav)getActivity()).setUpNFC();
        } catch (Exception e){
            Log.d(TAG, "Error: ", e);
        }

        //Creating an array fo all the time textViews
        TextView[] times = {getView().findViewById(R.id.todayTimeHomePage),
                getView().findViewById(R.id.tomorrowTimeHomePage),
                getView().findViewById(R.id.thirdDayTimeHomePage),
                getView().findViewById(R.id.forthDayTimeHomePage),
                getView().findViewById(R.id.fifthDayTimeHomePage),
        };

        //Finding the date texts so that they can be updated
        TextView firstDayText = getView().findViewById(R.id.todayTextHomePage);
        TextView secondDayText = getView().findViewById(R.id.tomorrowTextHomePage);
        TextView thirdDayText = getView().findViewById(R.id.thirdDayTextHomePage);
        TextView forthDayText = getView().findViewById(R.id.forthDayTextHomePage);
        TextView fifthDayText = getView().findViewById(R.id.fifthDayTextHomePage);

        //Getting the dates of the next 5 days
        Calendar cal = Calendar.getInstance();
        Date first = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH,1);
        Date second = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH,1);
        Date third = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH,1);
        Date forth = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH,1);
        Date fifth = cal.getTime();

        //Converting all dates to get their day of the week
        SimpleDateFormat df = new SimpleDateFormat("EEEE", Locale.getDefault());
        //Setting all days to lower case to meet database format
        String[] days = {df.format(first).toLowerCase(Locale.ROOT), df.format(second).toLowerCase(Locale.ROOT),
                df.format(third).toLowerCase(Locale.ROOT), df.format(forth).toLowerCase(Locale.ROOT)
                , df.format(fifth).toLowerCase(Locale.ROOT)};

        //Formatting the text dates to be correct
        df = new SimpleDateFormat("EEEE dd MMMM", Locale.getDefault());
        firstDayText.setText("Today ("+df.format(first)+")");
        secondDayText.setText("Tomorrow ("+df.format(second)+")");
        thirdDayText.setText(df.format(third));
        forthDayText.setText(df.format(forth));
        fifthDayText.setText(df.format(fifth));

        //Getting the email for finding the dates on the database
        Intent intent = getActivity().getIntent();
        String email = intent.getStringExtra("email");

        //Connecting to the database
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //Wrapping in a try in case user leaves page before all are set
        try{
            for (int i = 0; i< times.length; i++){
                getTime(email, times[i], days[i], db);
            }
        } catch (Exception e) {
            Log.d(TAG, "Warning: ", e);
        }
    }

    public void getTime(String email, TextView time, String day, FirebaseFirestore db){
        DocumentReference docRefTimes = db.collection("shiftPattern").document(email).collection(day).document("times");
        docRefTimes.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        boolean off = document.getBoolean("off");
                        if (off){
                            time.setText("OFF");
                        } else {
                            Date startTime = document.getDate("start");
                            Date endTime = document.getDate("end");
                            String startTimeStr = returnTime(startTime);
                            String endTimeStr = returnTime(endTime);
                            time.setText(startTimeStr + " - " + endTimeStr);
                        }
                    }
                }
            }
        });
    }

    public String returnTime(Date date){
        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);
        int hours = dateCal.get(Calendar.HOUR_OF_DAY);
        int mins = dateCal.get(Calendar.MINUTE);
        String hourStr = String.valueOf(hours);
        String minStr = String.valueOf(mins);
        if(hours < 10){
            hourStr = "0" + hourStr;
        }
        if(mins < 10){
            minStr = "0" + minStr;
        }
        return(hourStr+":"+minStr);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}