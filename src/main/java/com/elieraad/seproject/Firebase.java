package com.elieraad.seproject;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Firebase {
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference imagesRef;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;
    private DatabaseReference predictionsRef;

    public Firebase () {
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        imagesRef = storageRef.child("images");
        database = FirebaseDatabase.getInstance();
        dbRef = database.getReference();
        predictionsRef = dbRef.child("predictions");
    }

    public void storeImg(Context context, Uri uri, final String label, final double proba) {
        System.out.println("Storing Image....");
        String path = getRealPathFromUri(context, uri);
        try {
            InputStream stream = new FileInputStream(new File(path));
            final StorageReference ref = imagesRef.child(System.currentTimeMillis() + ".jpg");
            final UploadTask uploadTask = ref.putStream(stream);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    System.out.println("Unsuccessful upload");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }

                            // Continue with the task to get the download URL
                            return ref.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                System.out.println("Writing to db...");
                                Uri downloadUri = task.getResult();
                                System.out.println("Uri: " + downloadUri.toString());
                                Result result = new Result(downloadUri.toString(), label, proba);
                                DatabaseReference resultInstance = predictionsRef.child(System.currentTimeMillis() + "");
                                resultInstance.setValue(result);
                            } else {
                                // Handle failures
                                // ...
                            }
                        }
                    });
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
