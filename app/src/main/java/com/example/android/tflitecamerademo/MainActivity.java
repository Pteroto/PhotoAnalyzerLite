package com.example.android.tflitecamerademo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableList;
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
import android.view.View;
import android.widget.Button;

import com.example.android.tflitecamerademo.Utils.MeasurePerformanceUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {

    private static final int INPUT_SIZE = 224;

    private ObservableArrayList<Bitmap> loadedImages;
    private ArrayList<Bitmap> imagesToRemove;

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
            imagesToRemove = new ArrayList<>();
            loadedImages = new ObservableArrayList<>();

            loadedImages.addOnListChangedCallback(new ObservableList.OnListChangedCallback<ObservableList<Bitmap>>() {
                @Override
                public void onChanged(ObservableList<Bitmap> sender) {
                }

                @Override
                public void onItemRangeChanged(ObservableList<Bitmap> sender, int positionStart, int itemCount) {
                }

                @Override
                public void onItemRangeInserted(ObservableList<Bitmap> sender, int positionStart, int itemCount) {
                    Log.d("test", "positionStart: " + positionStart + "  ::  itemCount: " + itemCount);

                    measurePerformanceUtil.addStartTime();
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(sender.get(positionStart), INPUT_SIZE, INPUT_SIZE, false);
                    measurePerformanceUtil.logMeasuredTime("Scaled Bitmap Single");

                    measurePerformanceUtil.addStartTime();

                    List<String> results = classifier.classifyFrame(scaledBitmap);

                    measurePerformanceUtil.logMeasuredTime("Recognized Image Time Single");

                    if (results.get(1).contains("common")) {
                        imagesToRemove.add(sender.get(positionStart));
                    } else {
                        resultList.add(results);
                    }
                }

                @Override
                public void onItemRangeMoved(ObservableList<Bitmap> sender, int fromPosition, int toPosition, int itemCount) {
                }

                @Override
                public void onItemRangeRemoved(ObservableList<Bitmap> sender, int positionStart, int itemCount) {
                }
            });

            ArrayList<String> imageList = strings[0];

            Stream photos = imageList.stream().parallel();
            if (photos.isParallel()) {
                photos.forEach((Consumer<String>) photo ->
                        calculatePhoto(photo, loadedImages));
            }

//            for (Bitmap photo : loadedImages) {
//                measurePerformanceUtil.addStartTime();
//                Bitmap scaledBitmap = Bitmap.createScaledBitmap(photo, INPUT_SIZE, INPUT_SIZE, false);
//                measurePerformanceUtil.logMeasuredTime("Scaled Bitmap Single");
//
//                measurePerformanceUtil.addStartTime();
//
//                List<String> results = classifier.classifyFrame(scaledBitmap);
//
//                measurePerformanceUtil.logMeasuredTime("Recognized Image Time Single");
//
//                if (results.get(1).contains("common")) {
//                    imagesToRemove.add(photo);
//                } else {
//                    resultList.add(results);
//                }
//            }

            //measurePerformanceUtil.logMeasuredTime("Total time: Decode, scale and recognize images. Total images: " + strings[0].size());
            measurePerformanceUtil.logMeasuredTime("Total time: Decode, scale and recognize images. Total images: " + strings[0].size());

            //long estimatedTime = System.currentTimeMillis() - startTime;
            //Log.d("test", "TOTAL TIME: " + String.valueOf(estimatedTime / 1000) + "s" + "  ::  TOTAL PHOTOS: " + strings[0].size());

            loadedImages.removeAll(imagesToRemove);
            Log.d("test", "Images Blurred: " + loadedImages.size());
            return resultList;
        }

        @Override
        protected void onPostExecute(List<List<String>> lists) {
            for (int i = 0; i < loadedImages.size(); i++) {
                Log.d("ResultList", loadedImages.get(i) + " :: " + lists.get(i).get(1));
            }
            createResultList(lists, loadedImages);
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
//            imagesToRemove.add(photo);
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
//
//            final int halfHeight = height / 2;
//            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((width / inSampleSize) >= reqHeight
                    && (height / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
