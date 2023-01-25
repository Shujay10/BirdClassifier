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
import android.util.Log;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    TextView coName;
    TextView scName;
    ImageView image;
    ImageButton fromGal;
    ImageButton fromCam;

    private static final int GALLERY_REQUEST_CODE = 1;
    private static final int IMAGE_CAPTURE_CODE = 1000;

    Retrofit retro;
    Api api;

    @Override
    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coName = findViewById(R.id.cName);
        scName = findViewById(R.id.sName);
        image = findViewById(R.id.preview);
        fromGal = findViewById(R.id.gallery);
        fromCam = findViewById(R.id.camara);

        //API
        retro = new Retrofit.Builder()
                .baseUrl("https://birdsapi-production.up.railway.app/")
                .addConverterFactory(GsonConverterFactory.create()).build();

        api = retro.create(Api.class);

        // To Copy text from Textview to Clipboard
        scName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    String text1 = scName.getText().toString();
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

//        setCname("Struthio camel");
//        setCname("Struthio camelus");

    }

    private void fromGallery(){
        clear();
        // To open gallery to select images
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);


    }

    private void fromCamara(){
        clear();
        // To open camera to capture image
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }

    private void clear(){
        image.setImageDrawable(null);
        coName.setText("Select Image");
        scName.setText("");
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

    // Loaded Bird classifier ML model
    private void model(Bitmap bitmap){

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
//            scName.setText(out.getLabel());
//            System.out.println(out.getLabel()+"  "+ind);
            setCname(out.getLabel());

            model.close();
        } catch (IOException ignored) {

        }

    }

    private void setCname(String Sname){

        if(Sname.equalsIgnoreCase("None")){
            coName.setText("None");
            return;
        }

        // API call method to send scientific name and get common name
        Call<Data> call = api.getData(Sname);

        call.enqueue(new Callback<Data>() {
            @Override
            public void onResponse(Call<Data> call, Response<Data> response) {

                Data data = response.body();

                try {
                    coName.setText(data.getData());
                    scName.setText(Sname);

                }catch (NullPointerException e){
                    coName.setText("NA");
                    scName.setText(Sname);
                }

            }

            @Override
            public void onFailure(Call<Data> call, Throwable t) {
                //toast(t.toString());
                coName.setText("NA");
                scName.setText(Sname);
                toast("API - Offline");
            }
        });
    }

    private void toast(String txt){

        Toast.makeText(getApplicationContext(),txt,1).show();
    }

}