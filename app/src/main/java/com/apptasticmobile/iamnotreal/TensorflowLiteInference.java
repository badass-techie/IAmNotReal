package com.apptasticmobile.iamnotreal;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.tensorflow.lite.Interpreter;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TensorflowLiteInference extends AsyncTask<Void, Void, Bitmap> {
    private final int latentDim = 512, imageSize = 256, batchSize = 1;
    private final float thresh = 0.6f;
    Interpreter genInterpreter, discInterpreter;
    public Bitmap imageBmp;
    Random rnd = new Random();

    private WeakReference<View> loadingView, loadedView;
    private WeakReference<ImageView> resultView;

    TensorflowLiteInference(Interpreter genInterpreter, Interpreter discInterpreter, View loading, View loaded, ImageView result) {
        this.genInterpreter = genInterpreter;
        this.discInterpreter = discInterpreter;
        this.loadingView = new WeakReference<>(loading);
        this.loadedView = new WeakReference<>(loaded);
        this.resultView = new WeakReference<>(result);
    }

    @Override
    protected void onPreExecute() {
        //enable loading animation and hide everything else
        loadedView.get().setVisibility(View.GONE);
        loadingView.get().setVisibility(View.VISIBLE);
    }

    @Override
    protected Bitmap doInBackground(Void... voids) {
        //generator output
        float[][][][] image = new float[1][imageSize][imageSize][3];
        Map<Integer, Object> genOutput = new HashMap<>();
        genOutput.put(0, image);

        //discriminator output
        float[][] discOutput = new float[1][1];

        int run = 0;
        do {
            run++;
            //latent input
            float[][] latentVector = new float[batchSize][latentDim];
            for (int i = 0; i < batchSize; ++i) {
                for (int j = 0; j < latentDim; ++j) {
                    latentVector[i][j] = (float) rnd.nextGaussian();
                }
            }

            //noise input
            float[][] noise = new float[batchSize][imageSize * imageSize];
            for (int i = 0; i < imageSize * imageSize; ++i) {
                noise[0][i] = rnd.nextFloat();
            }

            //inputs. the 7 latent vectors to be applied to convolutional blocks, and the noise
            Object[] inputs = {latentVector, latentVector, latentVector, latentVector, latentVector, latentVector, latentVector, noise};

            //get inference from generator
            genInterpreter.runForMultipleInputsOutputs(inputs, genOutput);

            //get inference from discriminator
            discInterpreter.run(image, discOutput);
            //Toast.makeText(ctx, "run " + run + ", prob - " + discOutput[0][0], Toast.LENGTH_SHORT).show();
            Log.e("iamnotreal", "run " + run + ", prob - " + discOutput[0][0]);

        } while (discOutput[0][0] < thresh);

        genInterpreter.close();
        discInterpreter.close();

        //process output
        imageBmp = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888);
        for(int row = 0; row < imageSize; ++row){
            for(int element = 0; element < imageSize; ++element){
                int r = (int)Math.floor(image[0][row][element][0] * 255);
                int g = (int)Math.floor(image[0][row][element][1] * 255);
                int b = (int)Math.floor(image[0][row][element][2] * 255);
                imageBmp.setPixel(element, row, Color.argb(255, r, g, b));
            }
        }
        return imageBmp;
    }

    @Override
    protected void onPostExecute(Bitmap result){
        loadingView.get().setVisibility(View.GONE);
        loadedView.get().setVisibility(View.VISIBLE);
        resultView.get().setImageBitmap(result);
    }
}
