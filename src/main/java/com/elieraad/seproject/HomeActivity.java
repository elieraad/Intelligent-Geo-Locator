package com.elieraad.seproject;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private Button btn, yes, no, done;
    private ImageView imageview;
    private TextView resultText, feedbackText, correctionText;
    private RadioGroup answers;
    private RadioButton frem, zakhem, medical;
    private Uri filePath;
    private int GALLERY = 1, CAMERA = 2;
    private Firebase firebase;
    private String label;
    private double probability;

    private Bitmap bitmap;
    private ContentValues values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        firebase = new Firebase();
        requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar().hide(); // hide the title bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); //enable full screen

        setContentView(R.layout.activity_home);
        requestMultiplePermissions();

        btn = findViewById(R.id.btn);
        done = findViewById(R.id.done);
        yes = findViewById(R.id.yes);
        no = findViewById(R.id.no);
        imageview = findViewById(R.id.iv);
        resultText = findViewById(R.id.result);
        feedbackText = findViewById(R.id.feedback);
        correctionText = findViewById(R.id.correctionText);
        answers = findViewById(R.id.correction);
        frem = findViewById(R.id.frem);
        zakhem = findViewById(R.id.zakhem);
        medical = findViewById(R.id.medical);

        initialState();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPictureDialog();
            }
        });


        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebase.storeImg(HomeActivity.this, filePath, label, probability);
                initialState();
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                correctionState();

                done.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (medical.isSelected()){
                            firebase.storeImg(HomeActivity.this, filePath, "Medical", 50.0);
                        } else if (frem.isSelected()) {
                            firebase.storeImg(HomeActivity.this, filePath, "Frem", 50.0);
                        } else if (zakhem.isSelected()) {
                            firebase.storeImg(HomeActivity.this, filePath, "Zakehm", 50.0);
                        }
                        initialState();
                    }

                });

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        initialState();
    }

    private void showPictureDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Upload photo from gallery",
                "Take new photo" };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                choosePhotoFromGallary();
                                break;
                            case 1:
                                takePhotoFromCamera();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }

    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {
        values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        filePath = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, filePath);
        startActivityForResult(intent, CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageview.setImageBitmap(bitmap);
                uploadImage(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }


        } else if (requestCode == CAMERA && resultCode == RESULT_OK) {
            System.out.println("Filepath:" + filePath);
            try {
                bitmap  = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageview.setImageBitmap(bitmap);
                filePath = getImageUri(HomeActivity.this, bitmap);
                uploadImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void uploadImage(final Bitmap bitmap) {
        if(bitmap == null)
            return;
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://192.168.43.182:5000/predict";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject result = new JSONObject(response);
                            boolean success = result.getBoolean("success");
                            String prediction;
                            if(success) {
                                JSONArray tokenList = result.getJSONArray("predictions");
                                JSONObject oj = tokenList.getJSONObject(0);
                                label = oj.getString("label");
                                probability = oj.getDouble("probability");
                                String strDouble = String.format("%.2f", probability);
                                prediction = label + " " + strDouble + "%";
                                System.out.println(prediction);
                            } else {
                                prediction = "unsuccessful prediction";
                            }
                            //Toast.makeText(HomeActivity.this, prediction, Toast.LENGTH_SHORT).show();
                            resultState();
                            resultText.setText(prediction);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "That didn't work!", Toast.LENGTH_SHORT).show();
            }
        })

// Add the request to the RequestQueue.
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("image", imgToString(bitmap));
                return params;
            }
        };

        MySingleton.getInstance(HomeActivity.this).addToRequest(stringRequest);

    }

    private void requestMultiplePermissions() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Toast.makeText(getApplicationContext(), "All permissions are granted!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }

    private String imgToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imgBytes = baos.toByteArray();
        return Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }

    private void initialState (){
        btn.setVisibility(View.VISIBLE);
        yes.setVisibility(View.INVISIBLE);
        no.setVisibility(View.INVISIBLE);
        imageview.setVisibility(View.VISIBLE);
        feedbackText.setVisibility(View.INVISIBLE);
        correctionText.setVisibility(View.INVISIBLE);
        answers.setVisibility(View.INVISIBLE);
        resultText.setVisibility(View.INVISIBLE);
        done.setVisibility(View.INVISIBLE);
    }

    private void correctionState () {
        btn.setVisibility(View.INVISIBLE);
        yes.setVisibility(View.INVISIBLE);
        no.setVisibility(View.INVISIBLE);
        imageview.setVisibility(View.VISIBLE);
        feedbackText.setVisibility(View.INVISIBLE);
        correctionText.setVisibility(View.VISIBLE);
        answers.setVisibility(View.VISIBLE);
        resultText.setVisibility(View.INVISIBLE);
        done.setVisibility(View.VISIBLE);
    }

    public void  resultState(){
        btn.setVisibility(View.VISIBLE);
        resultText.setVisibility(View.VISIBLE);
        yes.setVisibility(View.VISIBLE);
        no.setVisibility(View.VISIBLE);
        imageview.setVisibility(View.VISIBLE);
        feedbackText.setVisibility(View.VISIBLE);
        correctionText.setVisibility(View.INVISIBLE);
        answers.setVisibility(View.INVISIBLE);
        done.setVisibility(View.INVISIBLE);
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}