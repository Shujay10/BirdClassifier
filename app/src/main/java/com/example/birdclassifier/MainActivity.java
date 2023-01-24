package com.example.birdclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.birdclassifier.ml.Birdclassifier;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView text;
    ImageView image;
    ImageButton fromGal;
    ImageButton fromCam;

    static Uri image_uri;

    private static final int GALLERY_REQUEST_CODE = 1;
    private static final int IMAGE_CAPTURE_CODE = 1000;

    @Override
    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = findViewById(R.id.text);
        image = findViewById(R.id.preview);
        fromGal = findViewById(R.id.gallery);
        fromCam = findViewById(R.id.camara);

        // To Copy text from Textview to Clipboard
        text.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    String text1 = text.getText().toString();
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("text", text1);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getApplicationContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        fromGal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fromGallery();
            }
        });

        fromCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fromCamara();
            }
        });

    }

    private void fromGallery(){

        // To open gallery to select images
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);


    }

    private void fromCamara(){

        // To open camera to capture image
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // From Gallery
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            // Sending the result to ML model [Image URI -> Bitmap -> ML model  ]
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                image.setImageURI(imageUri);
                model(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // From Camera
        if(requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK && data !=null){

            // Sending the result to ML model [Captured Image -> Bitmap -> ML model  ]
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            image.setImageBitmap(bitmap);
            model(bitmap.copy(Bitmap.Config.ARGB_8888,true));

        }

    }

    private void model(Bitmap bitmap){

        // Loaded Bird classifier ML model
        try {
            Birdclassifier model = Birdclassifier.newInstance(MainActivity.this);

            TensorImage image = TensorImage.fromBitmap(bitmap);

            Birdclassifier.Outputs outputs = model.process(image);
            List<Category> probability = (List<Category>) outputs.getProbabilityAsCategoryList();

            int ind = 0;
            float max = probability.get(0).getScore();
            // Finding the highest probability
            for(int i=0;i< probability.size();i++){

                if(max < probability.get(i).getScore()){
                    max = probability.get(i).getScore();
                    ind = i;
                }

            }

            Category out = probability.get(ind);
            text.setText(out.getLabel());

            model.close();
        } catch (IOException ignored) {

        }

    }

}