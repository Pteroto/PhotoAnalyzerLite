package com.example.android.tflitecamerademo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.example.android.tflitecamerademo.Utils.MeasurePerformanceUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {

    private static final int INPUT_SIZE = 224;

    private ArrayList<Bitmap> imagesPath;
    private ArrayList<Bitmap> imagesPathFinal;

    private ProgressDialog dialog;

    private RecyclerView mRecyclerView;

    private ImageClassifier classifier;

    private static final String TAG = "DEMO LITE";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Activity activity = this;

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("test", "clicked");
                dialog = new ProgressDialog(activity);
                dialog.show();

                ArrayList<String> photos = getAllShownImagesPath();

                if (photos.size() > 0)
                    new AnalysePhotos().execute(photos);
            }
        });

        initTensorFlowAndLoadModel();

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        classifier.close();
    }

    private void initTensorFlowAndLoadModel() {
        try {
            classifier = new ImageClassifier(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.");
        }
    }

    private ArrayList<String> getAllShownImagesPath() {
        Uri uri;
        Cursor cursor;
        int column_index_data;
        ArrayList<String> listOfAllImages = new ArrayList<>();
        String absolutePathOfImage;
        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

        cursor = this.getContentResolver().query(uri, projection, null,
                null, null);

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(column_index_data);

            if (absolutePathOfImage.contains("DCIM/Camera"))
                listOfAllImages.add(absolutePathOfImage);
        }
        cursor.close();
        return listOfAllImages;
    }

    private void createResultList(List<List<String>> result, List<Bitmap> photos) {
        RecyclerView.Adapter mAdapter = new ResultAdapter(photos, result, this);
        mRecyclerView.setAdapter(mAdapter);
        dialog.dismiss();
    }

    private class AnalysePhotos extends AsyncTask<ArrayList<String>, Void, List<List<String>>> {

        @Override
        protected List<List<String>> doInBackground(ArrayList<String>... strings) {
            MeasurePerformanceUtil measurePerformanceUtil = new MeasurePerformanceUtil();
            measurePerformanceUtil.addStartTime();

            //long startTime = System.currentTimeMillis();
            Log.d("test", "images getted: " + strings[0].size());
            final List<List<String>> resultList = new ArrayList<>();
            imagesPathFinal = new ArrayList<>();
            imagesPath = new ArrayList<>();

            Stream photos = strings[0].stream().parallel();
            if (photos.isParallel()) {
                photos.forEach(o -> {
                    calculatePhoto((String) o, imagesPath);
                });
                Log.d("test", "teste");
            }

            for (Bitmap photo : imagesPath) {
                //measurePerformanceUtil.addStartTime();

//                BitmapFactory.Options options = new BitmapFactory.Options();
////                options.inJustDecodeBounds = true;
////                BitmapFactory.decodeFile(photo, options);
////
////                options.inSampleSize = calculateInSampleSize(options, INPUT_SIZE, INPUT_SIZE);
////                options.inJustDecodeBounds = false;
////
////                Bitmap image = BitmapFactory.decodeFile(photo, options);
//
//
//                options.inSampleSize = 2;
//
//                Bitmap image = BitmapFactory.decodeFile(photo, options);
//
////                Bitmap image = BitmapFactory.decodeFile(photo);
//
//                measurePerformanceUtil.logMeasuredTime("Decoded Bitmap Single");

                measurePerformanceUtil.addStartTime();
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(photo, INPUT_SIZE, INPUT_SIZE, false);
                measurePerformanceUtil.logMeasuredTime("Scaled Bitmap Single");

                measurePerformanceUtil.addStartTime();

                List<String> results = classifier.classifyFrame(scaledBitmap);

                measurePerformanceUtil.logMeasuredTime("Recognized Image Time Single");

                if (results.get(1).contains("common")) {
                    imagesPathFinal.add(photo);
                } else {
                    resultList.add(results);
                }
            }

            //measurePerformanceUtil.logMeasuredTime("Total time: Decode, scale and recognize images. Total images: " + strings[0].size());
            measurePerformanceUtil.logMeasuredTime("Total time: Decode, scale and recognize images. Total images: " + strings[0].size());

            //long estimatedTime = System.currentTimeMillis() - startTime;
            //Log.d("test", "TOTAL TIME: " + String.valueOf(estimatedTime / 1000) + "s" + "  ::  TOTAL PHOTOS: " + strings[0].size());

            imagesPath.removeAll(imagesPathFinal);
            Log.d("test", "Images Blurred: " + imagesPath.size());
            return resultList;
        }

        @Override
        protected void onPostExecute(List<List<String>> lists) {
            for (int i = 0; i < imagesPath.size(); i++) {
                Log.d("ResultList", imagesPath.get(i) + " :: " + lists.get(i).get(0));
            }
            createResultList(lists, imagesPath);
        }
    }

    private void calculatePhoto(String photo, List<Bitmap> resultList) {
        MeasurePerformanceUtil measurePerformanceUtil = new MeasurePerformanceUtil();
        measurePerformanceUtil.addStartTime();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photo, options);

        options.inSampleSize = calculateInSampleSize(options, INPUT_SIZE, INPUT_SIZE);
        options.inJustDecodeBounds = false;

        Bitmap image = BitmapFactory.decodeFile(photo, options);


//        options.inSampleSize = 2;
//
//        Bitmap image = BitmapFactory.decodeFile(photo, options);

//                Bitmap image = BitmapFactory.decodeFile(photo);

        measurePerformanceUtil.logMeasuredTime("Decoded Bitmap Single");

        resultList.add(image);

//        measurePerformanceUtil.addStartTime();
//        Bitmap scaledBitmap = Bitmap.createScaledBitmap(image, INPUT_SIZE, INPUT_SIZE, false);
//        measurePerformanceUtil.logMeasuredTime("Scaled Bitmap Single");
//
//        measurePerformanceUtil.addStartTime();
//
//        List<String> results = classifier.classifyFrame(scaledBitmap);
//
//        measurePerformanceUtil.logMeasuredTime("Recognized Image Time Single");
//
//        if (results.get(1).contains("common")) {
//            imagesPathFinal.add(photo);
//        } else {
//            resultList.add(results);
//        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
