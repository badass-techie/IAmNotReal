package com.apptasticmobile.iamnotreal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SecondActivity extends AppCompatActivity {
    MappedByteBuffer generator, discriminator;
    Interpreter genInterpreter, discInterpreter;
    TensorflowLiteInference asyncTask;
    private View loadingView, loadedView;
    private ImageView resultView;
    private static final int NUM_LITE_THREADS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        loadingView = (View)findViewById(R.id.loading);
        loadedView = (View)findViewById(R.id.loaded);
        resultView = (ImageView)findViewById(R.id.result);

        try {
            generator = loadModelFile("generator.tflite");
            discriminator = loadModelFile("discriminator.tflite");
        }catch (Exception ex){
            ex.printStackTrace();
        }

        findViewById(R.id.link).setOnClickListener(v -> {
            Intent urlImplicitIntent = new Intent(Intent.ACTION_VIEW);
            urlImplicitIntent.setData(Uri.parse("https://arxiv.org/abs/1812.04948"));
            startActivity(urlImplicitIntent);
        });

        InitializeInterpreters();

        asyncTask = new TensorflowLiteInference(genInterpreter, discInterpreter, loadingView, loadedView, resultView);
        asyncTask.execute();
    }

    private MappedByteBuffer loadModelFile(String fileName) throws IOException {
        AssetFileDescriptor fileDescriptor=this.getAssets().openFd(fileName);
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset=fileDescriptor.getStartOffset();
        long declareLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declareLength);
    }

    private void InitializeInterpreters(){
        Interpreter.Options opt = new Interpreter.Options();
        opt.setNumThreads(NUM_LITE_THREADS);
        genInterpreter = new Interpreter(generator, opt);
        discInterpreter = new Interpreter(discriminator, opt);
    }

    public void saveImage(View view) throws IOException {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }

        Bitmap bitmap = asyncTask.imageBmp;
        FileOutputStream outStream = null;
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/Pictures/IAmNotReal");
        dir.mkdirs();
        String fileName = String.format("%d.jpg", System.currentTimeMillis());
        File outFile = new File(dir, fileName);
        outStream = new FileOutputStream(outFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
        outStream.flush();
        outStream.close();
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(outFile));
        sendBroadcast(intent);
        Toast.makeText(this, "Saved to gallery!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        Bitmap bitmap = asyncTask.imageBmp;
                        FileOutputStream outStream = null;
                        File sdCard = Environment.getExternalStorageDirectory();
                        File dir = new File(sdCard.getAbsolutePath() + "/Pictures/IAmNotReal");
                        dir.mkdirs();
                        String fileName = String.format("%d.jpg", System.currentTimeMillis());
                        File outFile = new File(dir, fileName);
                        outStream = new FileOutputStream(outFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                        outStream.flush();
                        outStream.close();
                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(Uri.fromFile(outFile));
                        sendBroadcast(intent);
                        Toast.makeText(this, "Saved to gallery!", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // permission denied
                    Toast.makeText(this, "Please allow access to your internal storage", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void refresh(View view) {
        recreate();
    }
}