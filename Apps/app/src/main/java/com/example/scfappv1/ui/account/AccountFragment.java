package com.example.scfappv1.ui.account;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.text.format.DateFormat;

import com.example.scfappv1.R;
import com.example.scfappv1.databinding.FragmentAccountBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import android.graphics.Bitmap;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private ImageView profilePic;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;
    private static final String TAG = "TestingMessage";
    public String imageId;
    public String email;
    public StorageReference storageRef;
    public StorageReference storageReference;
    public FirebaseStorage storage;
    public LinearLayoutCompat btnLayout;
    public ScrollView scrollView;
    public ConstraintLayout constLayout;
    public boolean viewingOtherUser = false;
    public boolean imageFound = false;
    public boolean adminBool = false;
    public Intent intent;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel accountViewModel =
                new ViewModelProvider(this).get(AccountViewModel.class);

        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        intent = getActivity().getIntent();
        if(intent.getBooleanExtra("viewing", false) && !(intent.getStringExtra("viewEmail").contains(intent.getStringExtra("email")))){
            intent.putExtra("viewing", false);
            email = intent.getStringExtra("viewEmail");
            viewingOtherUser = true;
        }
        else{
            email = intent.getStringExtra("email");
        }
        adminBool = intent.getBooleanExtra("admin", false);
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //Load profile image
        loadImage();

        //Fill in user information
        loadInfo();

        //Allowing user to upload a custom profile picture
        btnLayout = getView().findViewById(R.id.btnLayout);
        Button captureButton = getView().findViewById(R.id.btnCapture);
        Button cancelButton = getView().findViewById(R.id.btnCancel);
        Button editProfile = getView().findViewById(R.id.editProfileBtn);
        scrollView = getView().findViewById(R.id.scrollView2);
        constLayout = getView().findViewById(R.id.textureViewLayout);
        textureView = getView().findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        profilePic = getView().findViewById(R.id.profilePic);
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                if (getActivity().getApplicationContext().getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_CAMERA_FRONT)) {
                    if (!viewingOtherUser){
                        reset();
                    }
                } else {
                    Toast.makeText(getActivity(), "Missing permissions to access the front camera", Toast.LENGTH_SHORT).show();
                }
            }
        });
        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("usrEmail", email);
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_bottom_nav);
                navController.navigate(R.id.navigation_add_user);
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
            }
        });

        //Setting the current shifts pattern
        //Getting the days of the week buttons
        AppCompatTextView[] icons = {getActivity().findViewById(R.id.mondayIcon),
                getActivity().findViewById(R.id.tuesdayIcon),
                getActivity().findViewById(R.id.wednesdayIcon),
                getActivity().findViewById(R.id.thursdayIcon),
                getActivity().findViewById(R.id.fridayIcon),
                getActivity().findViewById(R.id.saturdayIcon),
                getActivity().findViewById(R.id.sundayIcon)};

        //Array for days of the week
        //Used for keeping track of what day the icon is pointing to
        String[] daysOfTheWeek = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

        //Starting the db connection
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //Lopping through all the icons and adding an on click method
        //This sets their colour and their active status
        //As well as setting the active status for their respective time buttons
        for (int i = 0; i< icons.length; i++){
            AppCompatTextView icon = icons[i];
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
                            }

                        }
                    }
                }
            });
        }

        //Setting up the change password section
        //Getting the necessary fields
        LinearLayoutCompat chngPwdLayout = getView().findViewById(R.id.chngPwdLayout);
        Button chngPass = getView().findViewById(R.id.chngPwdBtn);
        if (viewingOtherUser){
            chngPass.setVisibility(View.GONE);
        }
        //Setting the change password layout to have an alpha of 0 for the animation to work
        chngPwdLayout.animate().alpha(0.0f);

        //Adding an onclick to open the change password fields
        //Small animation added to give it a natural flow
        chngPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Checking if the layout is visible or not
                if (chngPwdLayout.getVisibility() == View.GONE){
                    //Setting the visibility
                    chngPwdLayout.setVisibility(View.VISIBLE);
                    //Adding the animation to make the layout fade in for 300
                    //Adding a blank onAnimationEnd to overwrite the one created when closing the layout
                    chngPwdLayout.animate()
                            .alpha(1.0f)
                            .setDuration(300)
                            .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                        }
                    });
                } else {
                    chngPwdLayout.animate()
                            .alpha(0.0f)
                            .setDuration(300)
                            .setListener(new AnimatorListenerAdapter() {
                                //Using the onAnimationEnd function to make it gone once done.
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    chngPwdLayout.setVisibility(View.GONE);
                                }
                            });
                }
            }
        });

        //Getting the update password button
        Button updPass = getView().findViewById(R.id.updaterPassConf);

        //Creating an onClick function
        updPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Getting the entered fields
                TextInputEditText newPass = getView().findViewById(R.id.newPass);
                TextInputEditText newPassConf = getView().findViewById(R.id.newPassConf);
                TextInputEditText oldPass = getView().findViewById(R.id.oldPass);

                //Getting values from those fields
                String newPasswordStr = newPass.getEditableText().toString();
                String newPasswordConfStr = newPassConf.getEditableText().toString();
                String oldPasswordStr = oldPass.getEditableText().toString();

                //Checking all fields are filled and the new password matches with the confirmed one
                if (TextUtils.isEmpty(newPasswordStr)){
                    Toast.makeText(getActivity(), "Please enter a new password.", Toast.LENGTH_SHORT).show();
                } else if (TextUtils.isEmpty(newPasswordConfStr)){
                    Toast.makeText(getActivity(), "Please confirm the password.", Toast.LENGTH_SHORT).show();
                } else if (TextUtils.isEmpty(oldPasswordStr)){
                    Toast.makeText(getActivity(), "Please enter the previous password.", Toast.LENGTH_SHORT).show();
                } else if (newPasswordStr.length()<6){
                    Toast.makeText(getActivity(), "New password is too short, must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                } else if (!newPasswordStr.equals(newPasswordConfStr)) {
                    Toast.makeText(getActivity(), "Passwords do not match.", Toast.LENGTH_SHORT).show();
                } else {
                    //Starting the database connection and setting the credentials of the user
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    AuthCredential credential = EmailAuthProvider.getCredential(email, oldPasswordStr);

                    //Checking the credentials match and adding an on complete listened
                    user.reauthenticate(credential)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    //Seeing if the user logged in successfully (Checking their old password works)
                                    if (task.isSuccessful()) {
                                        //Updating with the new password
                                        user.updatePassword(newPasswordStr).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                //Checking if it update or not and letting the user know
                                                if (task.isSuccessful()){
                                                    Toast.makeText(getActivity(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                                                }
                                                else{
                                                    Toast.makeText(getActivity(), "Failed to update password, please try again later.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    } else {
                                        Toast.makeText(getActivity(), "Old password is incorrect.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        //Setting shift patterns
        //Setting the patterns for the users shifts
        Button setShiftPattern = getView().findViewById(R.id.shiftPatternBtn);
        if (!adminBool){
            setShiftPattern.setVisibility(View.GONE);
            editProfile.setVisibility(View.GONE);
        }
        setShiftPattern.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_bottom_nav);
                navController.navigate(R.id.navigation_edit_shift);
            }
        });


    }

    private void loadImage(){
        binding.profilePic.setBackgroundResource(R.drawable.image_loading);
        // Create a storage reference from our app
        storage = FirebaseStorage.getInstance();
        // Create a child reference
        // imagesRef now points to "users_pfp"
        storageRef = storage.getReference("users_pfp/" + email);
        storageRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                for (StorageReference item : listResult.getItems()) {
                    imageFound = true;
                    // All the items under listRef.
                    imageId = "users_pfp/"+email+"/"+item.getName();
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
                                        matrix.postScale(-1, 1, bitmap.getWidth(), bitmap.getHeight());
                                        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                        try{
                                            binding.profilePic.setImageBitmap(rotatedBitmap);
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
                    if (!viewingOtherUser){
                        binding.profilePic.setBackgroundResource(R.drawable.noimgfound);
                    } else {
                        binding.profilePic.setBackgroundResource(R.drawable.noimgfoundblank);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Uh-oh, an error occurred!
            }
        });
    }

    public void loadInfo(){
        //Getting all fields from xml file
        TextView name = getView().findViewById(R.id.nameText);
        TextView team = getView().findViewById(R.id.teamText);
        TextView dob = getView().findViewById(R.id.dobText);
        TextView role = getView().findViewById(R.id.roleText);

        //Getting info to fill fields
        getInfo(name, "name");
        getInfo(team, "team");
        getInfo(dob, "DOB");
        getInfo(role, "role");

    }

    public void getInfo(TextView field, String fileName){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
         DocumentReference docRef = db.collection("username").document(email);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()){
                        switch(fileName){
                            case("DOB"):
                                Date dob = document.getDate(fileName);
                                String date = (String) DateFormat.format("dd/MM/yyyy", dob);
                                field.setText(date);
                                break;
                            case("name"):
                                field.setText(document.getString("firstName") + " " + document.getString("lastName"));
                                break;
                            default:
                                //Log.d(TAG, fileName);
                                field.setText(document.getString(fileName));
                        }
                    }
                    else{
                        Log.d(TAG, "uh oh");
                    }
                }else{
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void reset(){
        if (btnLayout.getVisibility() == View.GONE){
            btnLayout.setVisibility(View.VISIBLE);
            constLayout.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
        } else {
            btnLayout.setVisibility(View.GONE);
            constLayout.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);
        }
    }

    //From camera lab but customised with database info

    private final ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            // Get the captured image from the ImageReader
            Image image = reader.acquireLatestImage();
            if (image != null) {
                // Process the image data (e.g., save it to storage)
                // Example: Save the image to the Downloads folder
                String fileName = "newPfp.jpg";
                File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File imageFile = new File(downloadsDirectory, fileName);
                try (FileOutputStream output = new FileOutputStream(imageFile)) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    output.write(bytes);
                    String filePath = downloadsDirectory.toString()+"/newPfp.jpg";
                    Uri file = Uri.fromFile(new File(filePath));
                    ImageView tempProfile  = getView().findViewById(R.id.tempPfp);
                    tempProfile.setVisibility(View.VISIBLE);
                    textureView.setVisibility(View.GONE);
                    StorageReference pfpImagesRef = storageRef.child("pfp.jpg");
                    UploadTask uploadTask = pfpImagesRef.putFile(file);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Toast.makeText(getActivity(), "Image not uploaded!", Toast.LENGTH_SHORT).show();
                            imageFile.delete();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            try{
                                Toast.makeText(getActivity(), "Image uploaded!", Toast.LENGTH_SHORT).show();
                                imageFile.delete();
                                tempProfile.setVisibility(View.GONE);
                                textureView.setVisibility(View.VISIBLE);
                                reset();
                                loadImage();
                            } catch (Exception e) {
                                Log.d(TAG, "Error: ", e);
                            }
                        }
                    });
                    //Toast.makeText(getActivity(), "Image saved to Downloads folder", Toast.LENGTH_SHORT).show();
                    //Toast.makeText(getActivity(), downloadsDirectory.toString(), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    image.close();
                }
            }
        }
    };

    // -------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------
    // -----------------------------------CAMERA STUFF FROM LAB-----------------------------------
    // -------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // TextureView is available, perform camera setup here
            setupCamera(surface, width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Handle surface texture size change if needed
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            // Handle surface texture destruction if needed
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // Handle surface texture update if needed
        }
    };

    private void setupCamera(SurfaceTexture surfaceTexture, int width, int height) {
        try {
            surfaceTexture.setDefaultBufferSize(width, height);
            Surface surface = new Surface(surfaceTexture);

            CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            String cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // Choose an appropriate size for the ImageReader
            Size imageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), width, height);
            imageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);

            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreview(surface);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size chooseOptimalSize(Size[] choices, int width, int height) {
        // Add your logic to choose the best size based on your requirements
        // For simplicity, just return the first available size
        return choices[1];
    }

    private void createCameraPreview(Surface surface) {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                CaptureRequest request = builder.build();
                                cameraCaptureSession = session; // Assign the created session to cameraCaptureSession
                                session.setRepeatingRequest(request, null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            // Handle configuration failure
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureImage() {
        if (cameraCaptureSession != null) {
            try {
                // Create a CaptureRequest.Builder for still capture
                CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(imageReader.getSurface());

                // Configure the capture request with appropriate settings (e.g., auto focus, flash, etc.)
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                // Determine the rotation of the captured image
                int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(rotation));

                // Capture the image
                cameraCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        // Image captured, handle the captured image if needed
                    }
                }, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getActivity(), "Camera session not initialized", Toast.LENGTH_SHORT).show();
        }
    }


    private int getJpegOrientation(int rotation) {
        CameraCharacteristics characteristics;
        try {
            CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            String cameraId = manager.getCameraIdList()[1];
            characteristics = manager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return 0;
        }

        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int deviceOrientation = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                deviceOrientation = 0;
                break;
            case Surface.ROTATION_90:
                deviceOrientation = 90;
                break;
            case Surface.ROTATION_180:
                deviceOrientation = 180;
                break;
            case Surface.ROTATION_270:
                deviceOrientation = 270;
                break;
        }

        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;
        return jpegOrientation;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}