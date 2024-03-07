package com.example.scfappv1.ui.notifications;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.scfappv1.R;
import com.example.scfappv1.databinding.FragmentNotificationsBinding;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
public class NotificationsFragment extends Fragment {

    private static final String TAG = "TestingMessage";
    final Calendar myCalendar= Calendar.getInstance();
    public SimpleDateFormat dfText;
    public SimpleDateFormat dfCheck;
    public LinearLayoutCompat existingLin;

private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState){
        //Setting intent values to blank
        //Used later when displaying entries
        //Setting some empty ones for diary entries later
        Intent intent = getActivity().getIntent();
        intent.putExtra("diaryTitle", "");
        intent.putExtra("diaryDesc", "");
        intent.putExtra("diaryDate", "");
        intent.putExtra("diarySig", "");
        intent.putExtra("diaryImgPath", "");
        intent.putExtra("diaryEmail", "");

        //Getting the current date
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();

        existingLin = getActivity().findViewById(R.id.existingLayout);

        //Formatting the date to be what is in the database
        dfCheck = new SimpleDateFormat("dd:MM:yyyy", Locale.getDefault());

        //Calling the function to get today's diary entries
        checkExists(dfCheck.format(today).toString());

        //Getting the date button
        MaterialButton dateBtn = getActivity().findViewById(R.id.dateButton);
        dfText = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        dateBtn.setText(dfText.format(today));

        //Creating an on listener that activates once the user has picked another date
        DatePickerDialog.OnDateSetListener date =new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                //Sets the calendar to the selected date
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH,month);
                myCalendar.set(Calendar.DAY_OF_MONTH,day);
                //Sets the text to the new date
                dateBtn.setText(dfText.format(myCalendar.getTime()));
                existingLin.removeAllViews();
                checkExists(dfCheck.format(myCalendar.getTime()).toString());
            }
        };
        //Setting the on click for the date to open the date picker
        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(getActivity(),date,myCalendar.get(Calendar.YEAR),myCalendar.get(Calendar.MONTH),myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        //Adding functionality to the fab
        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_bottom_nav);
                navController.navigate(R.id.navigation_add_diary_entry);
            }
        });
    }

    public void checkExists(String date){
        //Connecting to the database for text info
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //Getting the document for the date specified
        DocumentReference diaryEntry = db.collection("diaryEntries").document(date);

        //Checking if the document exists
        diaryEntry.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()){
                    getDiaryEntries(db, date, 1);
                }
            }
        });
    }

    public void getDiaryEntries(FirebaseFirestore db, String date, int diaryEntryInt ){
        String diaryEntry = Integer.toString(diaryEntryInt);
        //Getting the collection for the diary date
        CollectionReference diaryCollection = db.collection("diaryEntries").document(date).collection(diaryEntry);
        //Getting the data from the diary entry
        diaryCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Boolean deleted = doc.getBoolean("deleted");
                        if (!deleted) {
                            //Collecting all the data
                            String title = doc.getString("title");
                            String desc = doc.getString("desc");
                            //Timestamp time = doc.getTimestamp("time");
                            String sig = doc.getString("sig");
                            String email = doc.getId();
                            String userFirstLetter = (doc.getString("sig")).substring(0, 1);
                            //Getting the first letter of the users name for the user icon
                            try {
                                displayData(title, desc, sig, userFirstLetter, email, date, diaryEntry);
                            } catch (Exception e) {
                                Log.w(TAG, "Error: ", e);
                            }
                        }
                        getDiaryEntries(db, date, diaryEntryInt + 1);
                    }
                }
            }
        });
    }

    public void displayData(String title, String desc, String sig, String userLetter, String email, String date, String diaryEntry){

        //Getting the existing linear layout to find the width of the device
        int width = existingLin.getWidth();

        //Getting layout variables to create
        FlexboxLayout flexLayout = new FlexboxLayout(getActivity());
        LinearLayoutCompat newLineLayout = new LinearLayoutCompat(getActivity());
        ConstraintLayout usrIconConstraint = new ConstraintLayout(getActivity());
        AppCompatTextView profilePic = new AppCompatTextView(getActivity());
        LinearLayoutCompat textLin = new LinearLayoutCompat(getActivity());
        AppCompatTextView titleView = new AppCompatTextView(getActivity());
        AppCompatTextView descView = new AppCompatTextView(getActivity());
        MaterialCardView imgViewCard = new MaterialCardView(getActivity());
        AppCompatImageView imgView = new AppCompatImageView(getActivity());
        Space space = new Space(getActivity());

        //Setting variable attributes
        //Linear layout for the whole section
        newLineLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        newLineLayout.setBackground(getActivity().getDrawable(R.drawable.diary_entry_border));
        newLineLayout.setOrientation(LinearLayoutCompat.HORIZONTAL);
        //Adding an on click listener to open the diary entry
        newLineLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = getActivity().getIntent();
                intent.putExtra("diaryTitle", title);
                intent.putExtra("diaryDesc", desc);
                intent.putExtra("diarySig", sig);
                intent.putExtra("diaryEmail", email);
                intent.putExtra("diaryDate", date);
                intent.putExtra("diaryEntry", diaryEntry);
                intent.putExtra("diaryImgPath", "diary/diary_imgs/" + date + "/" + diaryEntry);
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_bottom_nav);
                navController.navigate(R.id.navigation_add_diary_entry);
            }
        });

        //Flexbox to space out the items
        flexLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width*1/5));
        flexLayout.setJustifyContent(JustifyContent.SPACE_BETWEEN);

        //Constraint layout for the user icon
        usrIconConstraint.setLayoutParams(new ViewGroup.LayoutParams(width*1/5, width*1/5));

        //User icon
        profilePic.setLayoutParams(new ViewGroup.LayoutParams(width*1/5, width*1/5));
        //Creating a circle for the icon
        profilePic.setBackground(getActivity().getDrawable(R.drawable.inactive_circle));
        profilePic.setBackgroundTintList(getContext().getResources().getColorStateList(R.color.diary_btn_pink));
        //Making the letter float in the center of the circle
        profilePic.setGravity(Gravity.CENTER);
        profilePic.setScaleX((float) 0.7);
        profilePic.setScaleY((float) 0.7);
        profilePic.setText(userLetter);
        profilePic.setTextSize(30);
        profilePic.setTextColor(getActivity().getResources().getColor(R.color.button_dark_back));
        profilePic.setTypeface(profilePic.getTypeface(), Typeface.BOLD);

        //Setting linear layout for text area
        LinearLayout.LayoutParams textLinParams = new LinearLayout.LayoutParams(width*28/50, ViewGroup.LayoutParams.MATCH_PARENT);
        textLinParams.topMargin = 10;
        textLinParams.bottomMargin = 10;
        textLin.setOrientation(LinearLayoutCompat.VERTICAL);

        //Setting up the title field
        titleView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width*1/10));
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextAlignment(descView.TEXT_ALIGNMENT_TEXT_START);
        titleView.setMaxLines(1);
        titleView.setText(title);
        titleView.setTextSize(20);
        titleView.setTextColor(getActivity().getResources().getColor(R.color.black));
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);

        //Setting up the description area
        descView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width*1/10));
        descView.setEllipsize(TextUtils.TruncateAt.END);
        descView.setGravity(Gravity.CENTER);
        descView.setTextAlignment(descView.TEXT_ALIGNMENT_TEXT_START);
        descView.setMaxLines(1);
        descView.setText(desc);
        descView.setTextSize(15);

        //Setting up the image view card
        imgViewCard.setLayoutParams(new ViewGroup.LayoutParams(width*1/5, width*1/5));
        imgViewCard.setStrokeWidth(0);
        imgViewCard.setRadius(25);
        imgViewCard.setVisibility(View.INVISIBLE);

        //Setting up the diary entry image
        imgView.setLayoutParams(new ViewGroup.LayoutParams(width*1/5, width*1/5));
        imgView.setAdjustViewBounds(true);
        imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        getDiaryImg(date, diaryEntry, imgView, imgViewCard);

        textLin.setLayoutParams(textLinParams);

        //Creating space between each diary entry
        space.setLayoutParams(new ViewGroup.LayoutParams(1, width*1/10));

        //Adding all elements to their layouts
        usrIconConstraint.addView(profilePic);

        textLin.addView(titleView);
        textLin.addView(descView);

        imgViewCard.addView(imgView);

        flexLayout.addView(usrIconConstraint);
        flexLayout.addView(textLin);
        flexLayout.addView(imgViewCard);

        newLineLayout.addView(flexLayout);

        existingLin.addView(newLineLayout);
        existingLin.addView(space);
    }

    public void getDiaryImg(String date, String diaryEntry, ImageView diaryPicView, MaterialCardView diaryPicViewCard) {
        // Create a storage reference from our app
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a child reference
        // imagesRef now points to "users_pfp"
        StorageReference storageRef = storage.getReference("diary/diary_imgs/" + date + "/" + diaryEntry);
        storageRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                //Looping through the results although their should only be one
                for (StorageReference item : listResult.getItems()) {
                    //Getting the name of the image and getting the path from that
                    String imageId = "diary/diary_imgs/" + date + "/" + diaryEntry + "/" + item.getName();
                    StorageReference storageReference = FirebaseStorage.getInstance().getReference(imageId);
                    try {
                        File localfile = File.createTempFile("tempfile", ".jpg");
                        storageReference.getFile(localfile)
                                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                        //Getting image inside of bitmap variable
                                        Bitmap bitmap = BitmapFactory.decodeFile(localfile.getAbsolutePath());
                                        //Rotating the image as it comes in sideways
                                        Matrix matrix = new Matrix();
                                        matrix.postRotate(180);
                                        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                        try{
                                            diaryPicViewCard.setVisibility(View.VISIBLE);
                                            diaryPicView.setImageBitmap(rotatedBitmap);
                                        } catch (Exception e) {
                                            Log.d(TAG, "Error: " + e);
                                        }
                                    }
                                });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Uh-oh, an error occurred!
            }
        });
    }

@Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}