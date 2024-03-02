package com.example.scfappv1.ui.editShift;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.scfappv1.R;
import com.example.scfappv1.databinding.FragmentEditShiftBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EditShiftFragment extends Fragment {

    private FragmentEditShiftBinding binding;
    private static final String TAG = "TestingMessage";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        EditShiftViewModel notificationsViewModel =
                new ViewModelProvider(this).get(EditShiftViewModel.class);

        binding = FragmentEditShiftBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //Getting the days of the week buttons
        AppCompatTextView[] icons = {getActivity().findViewById(R.id.monday),
                getActivity().findViewById(R.id.tuesday),
                getActivity().findViewById(R.id.wednesday),
                getActivity().findViewById(R.id.thursday),
                getActivity().findViewById(R.id.friday),
                getActivity().findViewById(R.id.saturday),
                getActivity().findViewById(R.id.sunday)};

        //Array for days of the week
        //Used for keeping track of what day the icon is pointing to
        String[] daysOfTheWeek = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

        //Getting all of the time buttons
        Button[][] tmeBtns = {{getActivity().findViewById(R.id.mondayStartTimeBtn),
                getActivity().findViewById(R.id.mondayEndTimeBtn)},
                {getActivity().findViewById(R.id.tuesdayStartTimeBtn),
                getActivity().findViewById(R.id.tuesdayEndTimeBtn)},
                {getActivity().findViewById(R.id.wednesdayStartTimeBtn),
                getActivity().findViewById(R.id.wednesdayEndTimeBtn)},
                {getActivity().findViewById(R.id.thursdayStartTimeBtn),
                getActivity().findViewById(R.id.thursdayEndTimeBtn)},
                {getActivity().findViewById(R.id.fridayStartTimeBtn),
                getActivity().findViewById(R.id.fridayEndTimeBtn)},
                {getActivity().findViewById(R.id.saturdayStartTimeBtn),
                getActivity().findViewById(R.id.saturdayEndTimeBtn)},
                {getActivity().findViewById(R.id.sundayStartTimeBtn),
                getActivity().findViewById(R.id.sundayEndTimeBtn)}
        };

        //Starting the db connection
        //Used for getting current shift pattern
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //Getting email from intent
        Intent intent = getActivity().getIntent();
        String email = intent.getStringExtra("email");

        //Lopping through all the icons and adding an on click method
        //This sets their colour and their active status
        //As well as setting the active status for their respective time buttons
        for (int i = 0; i< icons.length; i++){
            AppCompatTextView icon = icons[i];
            Button[] selectTmeBtns = tmeBtns[i];
            String dayOfTheWeek = daysOfTheWeek[i];
            DocumentReference docRefTimes = db.collection("shiftPattern").document(email).collection(dayOfTheWeek).document("times");
            docRefTimes.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            boolean off = document.getBoolean("off");
                            if (!off){
                                icon.setBackground(getActivity().getDrawable(R.drawable.active_circle));
                                //Using the tag variable as it can hold the extra data
                                icon.setTag("active");
                                //Setting the buttons to be disabled
                                setBtns(icon, selectTmeBtns, dayOfTheWeek, true);
                            }
                            Date startTime = document.getDate("start");
                            Date endTime = document.getDate("end");
                            selectTmeBtns[0].setText(returnTime(startTime));
                            selectTmeBtns[1].setText(returnTime(endTime));

                        }
                    }
                }
            });
            icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, icon.getBackground().toString());
                    if (icon.getTag().toString().contains("inactive")){
                        icon.setBackground(getActivity().getDrawable(R.drawable.active_circle));
                        //Using the tag variable as it can hold the extra data
                        icon.setTag("active");
                        //Setting the buttons to be disabled
                        setBtns(icon, selectTmeBtns, dayOfTheWeek, true);
                    } else {
                        icon.setBackground(getActivity().getDrawable(R.drawable.inactive_circle));
                        icon.setTag("inactive");
                        setBtns(icon, selectTmeBtns, dayOfTheWeek, false);
                    }
                }
            });
        }


        //Adding an on click to each button that opens a time picker and changes the the text to be that time.
        for (Button[] btns : tmeBtns){
            for (Button btn : btns) {
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Calendar currentTime = Calendar.getInstance();
                        String buttonTime = btn.getText().toString();
                        String timesStr[] = buttonTime.split(":");
                        int hour = Integer.valueOf(timesStr[0]);
                        int minute = Integer.valueOf(timesStr[1]);
                        TimePickerDialog timePicker;
                        timePicker = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                                //Setting button text to be selected values
                                btn.setText(selectedHour + ":" + selectedMinute);
                            }
                            //Setting to 24 hour time
                        }, hour, minute, true);
                        timePicker.setTitle("Select Time");
                        timePicker.show();
                    }
                });
            }
        }
        //Storing the shift patterns in the database
        //Creating a new map for the times
        Map<String, Object> newTimesMap = new HashMap<>();

        //Getting the confirm button then setting an on click to upload the times
        Button confBtn = getActivity().findViewById(R.id.setShiftsConfBtn);
        confBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i< icons.length; i++){
                    //Getting the items for this day
                    AppCompatTextView icon = icons[i];
                    Button[] selectTmeBtns = tmeBtns[i];
                    String dayOfTheWeek = daysOfTheWeek[i];

                    //Creating the timestamp for start and end time
                    //Needed for storing time in the database
                    Timestamp startTimestamp;
                    Timestamp endTimestamp;

                    //Setting date to a string the converting to a date.
                    String startTime = "2000/01/01 " + selectTmeBtns[0].getText().toString() +":00";
                    String endTime = "2000/01/01 " + selectTmeBtns[1].getText().toString() +":00";
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    //Wrapping in a try catch as error could be thrown due to null date.
                    try {
                        //Formatting the date and changing to timestamp type
                        Date startDate = sdf.parse(startTime);
                        Date endDate = sdf.parse(endTime);
                        startTimestamp = new Timestamp(startDate);
                        endTimestamp = new Timestamp(endDate);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                    //Seeing if the user has selected they are off that day
                    if (icon.getTag().toString().contains("inactive")){
                        newTimesMap.put("off", true);
                    } else {
                        newTimesMap.put("off", false);
                    }

                    //Setting the values to the map
                    newTimesMap.put("end", endTimestamp);
                    newTimesMap.put("start", startTimestamp);

                    //Attempting to write to the db
                    db.collection("shiftPattern").document(email).collection(dayOfTheWeek).document("times")
                            .set(newTimesMap)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d(TAG, dayOfTheWeek+" added successfully");
                                }
                            }) .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, dayOfTheWeek+" not added successfully");
                                }
                            });
                }
                Toast.makeText(getActivity(), "Shifts set", Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
            }
        });
    }

    //Setting the active and inactive states of each button
    public void setBtns(AppCompatTextView textView, Button[] btns, String day, Boolean active){
        for (Button btn : btns){
            if (active){
                btn.setEnabled(true);
                btn.setBackgroundTintList(getContext().getResources().getColorStateList(R.color.white));
                btn.setTextColor(getActivity().getResources().getColor(R.color.button_dark_back));
            } else {
                btn.setEnabled(false);
                btn.setBackgroundTintList(getContext().getResources().getColorStateList(R.color.button_greyed_out));
                btn.setTextColor(getActivity().getResources().getColor(R.color.white));
            }
        }
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