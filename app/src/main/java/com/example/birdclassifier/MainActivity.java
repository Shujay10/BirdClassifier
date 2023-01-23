package com.example.birdclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.TextView;

import com.example.birdclassifier.ml.Birdclassifier;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView text;

    @Override
    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = findViewById(R.id.text);

        Drawable drawable = getResources().getDrawable(R.drawable.kf1);
        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();

        model(bitmap);

    }

    private void model(Bitmap bitmap){

        try {
            Birdclassifier model = Birdclassifier.newInstance(MainActivity.this);

            // Creates inputs for reference.
            TensorImage image = TensorImage.fromBitmap(bitmap);

            // Runs model inference and gets result.
            Birdclassifier.Outputs outputs = model.process(image);
            List<Category> probability = (List<Category>) outputs.getProbabilityAsCategoryList();

            int ind = 0;
            float max = probability.get(0).getScore();

            for(int i=0;i< probability.size();i++){

                if(max < probability.get(i).getScore()){
                    max = probability.get(i).getScore();
                    ind = i;
                }

            }

            Category out = probability.get(ind);
            System.out.println(out.getLabel());

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }

    }

}