package com.example.scfappv1.ui.team;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.scfappv1.R;
import com.example.scfappv1.databinding.FragmentTeamBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

public class TeamFragment extends Fragment {

    private FragmentTeamBinding binding;
    private static final String TAG = "TestingMessage";
    public String username;
    public String team;
    public String email;
    public StorageReference storageRef;
    public StorageReference storageReference;
    public FirebaseStorage storage;
    public Intent intent;
    public boolean imageFound = false;
    public int width;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TeamViewModel teamViewModel =
                new ViewModelProvider(this).get(TeamViewModel.class);

        binding = FragmentTeamBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        intent = getActivity().getIntent();
        email = intent.getStringExtra("email");
        team = intent.getStringExtra("team");
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        loadData();
    }

    public void loadData(){
        // Create a storage reference from our app
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference users = db.collection("username");
        Button addUsrBtn = getView().findViewById(R.id.addUserBtn);
        users.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                TextView teamName = getView().findViewById(R.id.teamNameText);
                teamName.setText(team + " Team");
                for (DocumentSnapshot item : queryDocumentSnapshots.getDocuments()){

                        if (item.getData().get("team").toString().contains(team)){
                            // Getting the linear layout for the right role
                            LinearLayoutCompat linLayout;
                            String role = item.getData().get("role").toString();
                            if (role.contains("Manager")){
                                linLayout = getView().findViewById(R.id.teamAdminLinLayout);
                            } else if (role.contains("Staff")) {
                                linLayout = getView().findViewById(R.id.teamStaffLinLayout);
                            } else {
                                linLayout = getView().findViewById(R.id.teamVolunteerLinLayout);
                            }
                            width = linLayout.getWidth();

                            //Creating the new items
                            LinearLayoutCompat newLineLayout = new LinearLayoutCompat(getActivity());
                            ImageView profilePic = new ImageView(getActivity());
                            TextView name = new TextView(getActivity());
                            LinearLayoutCompat linksLayout = new LinearLayoutCompat(getActivity());
                            MaterialButton remove = new MaterialButton(getActivity());
                            MaterialButton edit = new MaterialButton(getActivity());

                            //Creating specific layout for image to allow for gravity to work
                            RelativeLayout imageLayout = new RelativeLayout(getActivity());

                            //Setting variable attributes
                            //Linear layout for the whole section
                            newLineLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            newLineLayout.setOrientation(LinearLayoutCompat.HORIZONTAL);
                            newLineLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    openAccount(item.getData().get("username").toString(), item.getId());
                                }
                            });

                            //Layout for image
                            imageLayout.setLayoutParams( new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

                            //Profile pic image view
                            profilePic.setAdjustViewBounds(true);
                            profilePic.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            RelativeLayout.LayoutParams profileParams = new RelativeLayout.LayoutParams(width*2/9, width*2/9);
                            profileParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                            profilePic.setLayoutParams(profileParams);

                            //Setting the user profile picture with the one on the database
                            getUserImage(item.getData().get("username").toString(), profilePic);

                            //Setting image in container
                            imageLayout.addView(profilePic);

                            //Name text field
                            name.setText(item.getData().get("firstName").toString() + " " + item.getData().get("lastName").toString());
                            name.setTextSize(20);
                            name.setPadding(30, 0, 0, 30);
                            name.setGravity(Gravity.CENTER_VERTICAL);
                            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(width*5/9, ViewGroup.LayoutParams.MATCH_PARENT);
                            name.setLayoutParams(nameParams);

                            //Layout for the buttons
                            linksLayout.setOrientation(LinearLayoutCompat.VERTICAL);
                            LinearLayout.LayoutParams linksParams = new LinearLayout.LayoutParams(width*2/9, ViewGroup.LayoutParams.WRAP_CONTENT);
                            linksLayout.setLayoutParams(linksParams);

                            //Only allowing admins to see the edit and remove buttons
                            if (!intent.getBooleanExtra("admin", false)){
                                remove.setVisibility(View.INVISIBLE);
                                remove.setClickable(false);
                                edit.setVisibility(View.INVISIBLE);
                                addUsrBtn.setVisibility(View.INVISIBLE);
                            }

                            //Remove profile button
                            remove.getBackground().setTint(getActivity().getResources().getColor(R.color.button_dark_back));
                            remove.setPadding(0, 0, 0, 10);
                            remove.setText("Remove");
                            remove.setTextSize(10);
                            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            removeParams.weight = 1.0f;
                            removeParams.gravity = Gravity.CENTER;
                            remove.setLayoutParams(removeParams);

                            if (item.getId().toString().contains(email)){
                                remove.getBackground().setTint(getActivity().getResources().getColor(R.color.button_greyed_out));
                                remove.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Toast.makeText(getActivity(), "Cannot delete your own account.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            //Edit profile button
                            edit.getBackground().setTint(getActivity().getResources().getColor(R.color.button_dark_back));
                            edit.setPadding(0, 0, 0, 10);
                            edit.setText("Edit");
                            edit.setTextSize(10);
                            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            editParams.weight = 1.0f;
                            editParams.gravity = Gravity.CENTER;
                            edit.setLayoutParams(editParams);

                            //Adding it all together
                            //Adding the items to the links layout
                            linksLayout.addView(remove);
                            linksLayout.addView(edit);

                            //Adding the items to the new linear layout
                            newLineLayout.setWeightSum(9f);
                            newLineLayout.addView(imageLayout);
                            newLineLayout.addView(name);
                            newLineLayout.addView(linksLayout);

                            //Adding it all to the existing layout
                            linLayout.addView(newLineLayout);
                        }
                        else{
                        }

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Error: " + e);
            }
        });

        addUsrBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_bottom_nav);
                navController.navigate(R.id.navigation_add_user);
            }
        });
    }

    //Create a new string extra and have acc check that. If exists, set to new acc, else,, swt to defauklt. Maybe add a bool var to check instaed of if void? Remeberer to rset field after sertting the data
    public void openAccount(String username, String email){
        Intent intent = getActivity().getIntent();
        intent.putExtra("viewEmail", email);
        intent.putExtra("viewing", true);
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_bottom_nav);
        navController.navigate(R.id.navigation_account);
    }

    public void getUserImage(String newUsername, ImageView profile){
        //Setting a placeholder image while it loads
        imageFound = false;
        profile.setBackgroundResource(R.drawable.image_loading);
        // Create a storage reference from our app
        storage = FirebaseStorage.getInstance();
        // Create a child reference
        // imagesRef now points to "users_pfp"
        storageRef = storage.getReference("users_pfp/" + newUsername);
        storageRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {

                for (StorageReference item : listResult.getItems()) {
                    imageFound = true;
                    // All the items under listRef.
                    String imageId = "users_pfp/"+newUsername+"/"+item.getName();
                    storageReference = FirebaseStorage.getInstance().getReference(imageId);
                    try {
                        File localfile = File.createTempFile("tempfile",".jpg");
                        storageReference.getFile(localfile)
                                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                        //Getting image inside of bitmap variable
                                        Bitmap bitmap = BitmapFactory.decodeFile(localfile.getAbsolutePath());
                                        //Rotating the image as it comes in sideways
                                        Matrix matrix = new Matrix();
                                        matrix.postRotate(270);
                                        matrix.postScale(-1, 1, bitmap.getWidth(), bitmap.getHeight());
                                        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                        try{
                                            profile.setImageBitmap(rotatedBitmap);
                                        } catch (Exception e) {
                                            Log.d(TAG, "Error: " + e);
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
                if (!imageFound){
                    profile.setBackgroundResource(R.drawable.noimgfoundblank);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Uh-oh, an error occurred!
            }
        });
    }
}