package com.example.scfappv1.ui.addDiaryEntry;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.widget.TextViewCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.scfappv1.BottomNav;
import com.example.scfappv1.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddDiaryEntryFragment extends Fragment {
    private static final String TAG = "TestingMessage";
    final Calendar myCalendar= Calendar.getInstance();
    public String title;
    public String desc;
    public Date today;
    public String date;
    public MaterialButton rmv;
    public TextInputEditText titleField;
    public TextInputEditText descField;
    public Button captureButton;
    public Button cancelButton;
    public MaterialButton submit;
    public MaterialButton camera;
    public TextView imgTxt;
    public File imageFile;
    public File downloadsDirectory;
    public boolean hasImage = false;
    public boolean preExisted = false;
    public String diaryDate;
    public String usrDiaryEntry;
    public String diaryEmail;
    public MaterialButton dateBtn;
    public TextView dateText;

    //Camera features
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;
    private TextureView textureView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_diary_entry, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();
        diaryEmail = intent.getStringExtra("diaryEmail");
        usrDiaryEntry = intent.getStringExtra("diaryEntry");
        diaryDate = intent.getStringExtra("diaryDate");
        //Setting up the camera features
        //Setting up the texture view
        textureView = getView().findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(surfaceTextureListener);

        //Getting the post fields
        titleField = getActivity().findViewById(R.id.title);
        descField = getActivity().findViewById(R.id.desc);

        //Getting the submit button
        submit = getActivity().findViewById(R.id.addUserBtn);

        //Getting the image button
        camera = getActivity().findViewById(R.id.addPhotos);

        //Setting the remove image button as not clickable
        rmv = getActivity().findViewById(R.id.rmvImg);
        rmv.setClickable(false);

        imgTxt = getActivity().findViewById(R.id.imgTknTxt);

        //Grabbing both camera buttons
        captureButton = getView().findViewById(R.id.btnCapture);
        cancelButton = getView().findViewById(R.id.btnCancel);

        //Getting the date fields
        dateBtn = getView().findViewById(R.id.changeDate);
        dateText = getView().findViewById(R.id.dateText);


        //Setting up the view for existing diary entries
        String diaryTitle = intent.getStringExtra("diaryTitle");
        if (diaryTitle.equals("")){
            //Setting entry fields
            addEntry();
        } else {
            //Displaying  entry fields
            fillWithExistingData(intent);
        }
    }

    public void fillWithExistingData(Intent intent){
        //Setting new entry elements to invisible
        submit.setVisibility(View.GONE);
        rmv.setVisibility(View.GONE);
        imgTxt.setVisibility(View.GONE);
        camera.setVisibility(View.GONE);
        dateBtn.setVisibility(View.GONE);
        dateText.setVisibility(View.GONE);

        //Making new elements visible
        TextView sigView = getActivity().findViewById(R.id.sigText);
        sigView.setVisibility(View.VISIBLE);

        //Changing the description size
        ViewGroup.LayoutParams layoutParams =descField.getLayoutParams();
        layoutParams.height=ViewGroup.LayoutParams.WRAP_CONTENT;
        descField.setLayoutParams(layoutParams);

        //Making the edit fields no longer editable
        descField.setFocusable(false);
        descField.setFocusableInTouchMode(false);
        descField.setClickable(false);

        titleField.setFocusable(false);
        titleField.setFocusableInTouchMode(false);
        titleField.setClickable(false);

        //Getting the existing data
        titleField.setText(intent.getStringExtra("diaryTitle"));
        descField.setText(intent.getStringExtra("diaryDesc"));
        sigView.setText("By "+intent.getStringExtra("diarySig"));

        //Getting and setting the diary image if there is one
        if (!intent.getStringExtra("diaryImgPath").isEmpty()) {
            hasImage = true;
            ImageView postImgView = getActivity().findViewById(R.id.postImg);
            postImgView.setVisibility(View.VISIBLE);

            //Getting the string for image location
            String imgLocation = intent.getStringExtra("diaryImgPath");
            // Create a storage reference from our app
            FirebaseStorage storage = FirebaseStorage.getInstance();
            // Create a child reference
            // imagesRef now points to "users_pfp"
            StorageReference storageRef = storage.getReference(imgLocation);
            storageRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                @Override
                public void onSuccess(ListResult listResult) {
                    //Looping through the results although their should only be one
                    for (StorageReference item : listResult.getItems()) {
                        //Getting the name of the image and getting the path from that
                        Log.d(TAG, imgLocation);
                        StorageReference storageReference = FirebaseStorage.getInstance().getReference(imgLocation+"/"+item.getName());
                        try {
                            File localfile = File.createTempFile("tempfile", ".jpg");
                            storageReference.getFile(localfile)
                                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            //Getting image inside of bitmap variable
                                            downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                                            imageFile = new File(downloadsDirectory, "diaryImg.jpg");
                                            FileOutputStream fOut;
                                            try {
                                                fOut = new FileOutputStream(imageFile);
                                            } catch (FileNotFoundException e) {
                                                throw new RuntimeException(e);
                                            }
                                            Bitmap bitmap = BitmapFactory.decodeFile(localfile.getAbsolutePath());
                                            //Rotating the image as it comes in sideways
                                            Matrix matrix = new Matrix();
                                            matrix.postRotate(180);
                                            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                            try{
                                                postImgView.setImageBitmap(rotatedBitmap);
                                            } catch (Exception e) {
                                                Log.d(TAG, "Error: " + e);
                                            }
                                            rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                                            rmv.setTextColor(getActivity().getResources().getColor(R.color.button_dark_back));
                                            rmv.setClickable(true);
                                            TextView imgAddedTxt = imgTxt;
                                            imgAddedTxt.setText("Image added!");
                                            imgAddedTxt.setTag("full");
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error here: ", e);
                                        }
                                    });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    }
                }
            });
        }
        //Setting up fabs if this is the users post
        if (intent.getStringExtra("diaryEmail").equals(intent.getStringExtra("email"))){
            FloatingActionButton editEntry = getActivity().findViewById(R.id.editFab);
            FloatingActionButton delEntry = getActivity().findViewById(R.id.deleteFab);
            editEntry.setVisibility(View.VISIBLE);
            delEntry.setVisibility(View.VISIBLE);
            editEntry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (hasImage) {
                        View postImgView = getActivity().findViewById(R.id.postImg);
                        postImgView.setVisibility(View.GONE);
                    }
                    //Setting new entry elements back to visible
                    submit.setVisibility(View.VISIBLE);
                    rmv.setVisibility(View.VISIBLE);
                    imgTxt.setVisibility(View.VISIBLE);
                    camera.setVisibility(View.VISIBLE);
                    dateBtn.setVisibility(View.VISIBLE);
                    dateText.setVisibility(View.VISIBLE);

                    //Reverting new elements to invisible
                    sigView.setVisibility(View.GONE);
                    editEntry.setVisibility(View.GONE);
                    delEntry.setVisibility(View.GONE);

                    //Making the edit fields editable
                    descField.setFocusable(true);
                    descField.setFocusableInTouchMode(true);
                    descField.setClickable(true);

                    titleField.setFocusable(true);
                    titleField.setFocusableInTouchMode(true);
                    titleField.setClickable(true);

                    //Setting the function to say this item already exists in the db
                    preExisted = true;
                    addEntry();
                }
            });

            delEntry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteEntry();
                    getActivity().onBackPressed();
                }
            });
        }
    }

    public void addEntry(){
        //Adding an on click to start submitting the entry
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkFilled();
            }
        });

        rmv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageFile.delete();
                imgTxt.setTag("empty");
                imgTxt.setText("No image taken ");
            }
        });

        //Adding an on click to open the camera preview
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Setting up the camera features
                //Setting up the texture view
                textureView.setSurfaceTextureListener(surfaceTextureListener);
                if (getActivity().getApplicationContext().getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_CAMERA)) {
                    //Function to make the camera elements invisible or visible
                    reset();
                } else {
                    Toast.makeText(getActivity(), "Missing permissions to access the back camera", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Adding on clicks to the button
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

        //Getting the current date
        Calendar cal = Calendar.getInstance();
        today = cal.getTime();

        //Formatting the date
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        //Formatting the date for the database
        SimpleDateFormat dbDf = new SimpleDateFormat("dd:MM:yyyy", Locale.getDefault());

        //Setting the date to either tha passed through date or the current date
        if (!diaryDate.equals("")){
            dateText.setText(diaryDate.replace(":", "/"));
            date = diaryDate;
        }
        else{
            dateText.setText(df.format(today));
            date = dbDf.format(today);
        }

        //Creating an on listener that activates once the user has picked another date
        DatePickerDialog.OnDateSetListener datePicker =new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                //Sets the calendar to the selected date
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH,month);
                myCalendar.set(Calendar.DAY_OF_MONTH,day);
                //Sets the text to the new date
                dateText.setText(df.format(myCalendar.getTime()));
                date = dbDf.format(myCalendar.getTime());
            }
        };
        //Setting the on click for the date to open the date picker
        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(getActivity(),datePicker,myCalendar.get(Calendar.YEAR),myCalendar.get(Calendar.MONTH),myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }

    public void checkFilled(){
        title = titleField.getText().toString();
        desc = descField.getText().toString();
        if (title.isEmpty()){
            Toast.makeText(getActivity(), "Please enter a title.", Toast.LENGTH_SHORT).show();
        } else if (desc.isEmpty()){
            Toast.makeText(getActivity(), "Please enter a description.", Toast.LENGTH_SHORT).show();
        } else {
            uploadNewEntryToFirestore();
        }
    }

    public void uploadNewEntryToFirestore(){
        //Creating a map to hold all the new database items
        Map<String, Object> newUserMap = new HashMap<>();
        //Putting in the new database items
        newUserMap.put("title", title);
        newUserMap.put("desc", desc);

        //Connecting to the database
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //Getting the document for the date specified
        DocumentReference diaryEntry = db.collection("diaryEntries").document(date);

        //Checking if the document exists
        diaryEntry.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                String docVal;
                DocumentSnapshot document = task.getResult();
                if (!document.exists()){
                    createTempDoc(db);
                } else {
                    getLatestDocVal(db, 1);
                }
            }
        });
    }

    public void createTempDoc(FirebaseFirestore db){
        //Creating a temp map to create the document folder
        Map<String, Object> newTempMap = new HashMap<>();
        //Creating a basic entry
        newTempMap.put("exists", true);

        //Making the directory
        db.collection("diaryEntries").document(date)
                .set(newTempMap).addOnSuccessListener(new OnSuccessListener<Void>() {
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
        createNewEntry(db, "1");
    }

    public void getLatestDocVal(FirebaseFirestore db, int diaryEntryInt){
        String diaryEntry = Integer.toString(diaryEntryInt);

        //Getting the collection for the diary date
        CollectionReference diaryCollection = db.collection("diaryEntries").document(date).collection(diaryEntry);

        //Getting the data from the diary entry
        diaryCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                Log.w(TAG, "HERE1: " + preExisted);
                if (!task.getResult().isEmpty() && !date.equals(diaryDate)) {
                    getLatestDocVal(db, diaryEntryInt+1);
                } else if (preExisted) {
                    createNewEntry(db, usrDiaryEntry);
                } else {
                    createNewEntry(db, diaryEntry);
                }
            }
        });
    }

    public void createNewEntry(FirebaseFirestore db, String diaryEntry){
        //Creating a temp map to create the document folder
        Map<String, Object> newMap = new HashMap<>();
        newMap.put("desc", desc);
        newMap.put("title", title);
        newMap.put("deleted", false);

        //Getting the username for the signature as well as the email for the db
        Intent intent = getActivity().getIntent();
        //Creating the signature
        String sig = (intent.getStringExtra("firstName") + " " + intent.getStringExtra("lastName").substring(0,1) + ".");
        //adding to map
        newMap.put("sig", sig);

        //Getting the email
        String email = intent.getStringExtra("email");
        if (preExisted){
            email = diaryEmail;
        }

        if (!diaryDate.equals("") && diaryDate != date){
            deleteEntry();
        }

        //Adding the current timestamp
        Timestamp time = new Timestamp(today);
        newMap.put("time", time);

        //Setting to db
        db.collection("diaryEntries").document(date).collection(diaryEntry).document(email)
                .set(newMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Toast.makeText(getActivity().getApplicationContext(), "New entry added successfully.", Toast.LENGTH_SHORT).show();
                    }
                }) .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Not added successfully");
                    }
                });

        //Uploading the image
        if(imgTxt.getTag().toString().contains("full")){
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference("diary/diary_imgs/" + date + "/" + diaryEntry);
            String filePath = downloadsDirectory.toString()+"/diaryImg.jpg";
            Uri file = Uri.fromFile(new File(filePath));
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
                        imageFile.delete();
                    } catch (Exception e) {
                        Log.d(TAG, "Error: ", e);
                    }
                }
            });
        } else if (hasImage){
            deleteImg();
        }
        //Creating the users name for the notification
        String name = (intent.getStringExtra("firstName") + " " + intent.getStringExtra("lastName"));
        ((BottomNav)getActivity()).handleNotifications("New diary entry!","A new diary entry has been created by "+name+" about '"+title+"'.");
        getActivity().onBackPressed();
    }

    public void deleteImg(){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference childRef = storageRef.child("diary/diary_imgs/"+diaryDate+"/"+usrDiaryEntry+"/pfp.jpg");
        childRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(TAG, "Image deleted from database");
            }
        });
    }

    public void deleteEntry(){
        Map<String, Object> delMap = new HashMap<>();
        delMap.put("deleted", true);
        //Connecting to the database
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("diaryEntries").document(diaryDate).collection(usrDiaryEntry).document(diaryEmail)
                .update(delMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "Deleted entry!");
                    }
                });
        Toast.makeText(getActivity().getApplicationContext(), "Deleted entry", Toast.LENGTH_SHORT);
    }

    private void reset(){
        View btnLayout = getView().findViewById(R.id.btnLayout);
        View scrollView = getView().findViewById(R.id.scrollView2);
        View constLayout = getView().findViewById(R.id.textureViewLayout);

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
                String fileName = "diaryImg.jpg";
                downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                imageFile = new File(downloadsDirectory, fileName);
                try (FileOutputStream output = new FileOutputStream(imageFile)) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    output.write(bytes);
                    Toast.makeText(getActivity(), "Image saved!", Toast.LENGTH_SHORT).show();
                    //textureView.setVisibility(View.GONE);
                    rmv.setTextColor(getActivity().getResources().getColor(R.color.button_dark_back));
                    rmv.setClickable(true);
                    TextView imgAddedTxt = imgTxt;
                    imgAddedTxt.setText("Image added!");
                    imgAddedTxt.setTag("full");
                    reset();
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
            String cameraId = manager.getCameraIdList()[0];
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
    }
}