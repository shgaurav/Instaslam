package com.example.gaurav.instaslam.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.gaurav.instaslam.R;
import com.example.gaurav.instaslam.models.InstaImage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MediaActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    final int PERMISSION_READ_EXTERNAL = 111;

    private ArrayList<InstaImage> images = new ArrayList<>();
    private ImageView selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_media);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        //Declaring SelectedImage view and setting it up with its layout file...
        selectedImage = (ImageView)findViewById(R.id.selected_image);

        //Declaring the recycler view and setting it up with its layout file...
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.content_images);
        //Initializing an ImageAdapter...
        ImagesAdapter adapter = new ImagesAdapter(images);

        //Setting the adapter on the recylerview...
        recyclerView.setAdapter(adapter);

        //Intializing a new Gridlayoutmanager with 4 grids...
        GridLayoutManager layoutManager = new GridLayoutManager(getBaseContext(),4);
        //Setting grid layout orientation to VERTICAL...
        layoutManager.setOrientation(GridLayoutManager.VERTICAL);

        //Assigning the gridlayout to the recyclerview...
        recyclerView.setLayoutManager(layoutManager);

        //Conditions for checking if the user has given permission for reading storage...if no, ask the user or else  retrieve the images...
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL);

        } else {
            retrieveAndSetImages();
        }


        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    //A function for checking the result from the permission request to the user and then take appropriate actions...
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_READ_EXTERNAL: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    retrieveAndSetImages();
                }
            }
        }
    }

    //A function to retrive and set images from the phone...
    public void retrieveAndSetImages() {
        //Running a new task on android to run cursor to cycle through images in the background task...
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                //Clear image before adding the image...
                images.clear();
                //create and declare a cursor to cycle through images on the phones storage...
                Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,null,null,null,null);

                //Cycling through all the images on the phone...
                if (cursor != null) {
                    cursor.moveToFirst();

                    for (int x = 0; x < cursor.getCount(); x++) {
                        cursor.moveToPosition(x);
                        Log.v("FISH", "URL: " + cursor.getString(1));
                        InstaImage img = new InstaImage(Uri.parse(cursor.getString(1)));
                        images.add(img);
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //set images on recyclerview adapter
                        //update images
                    }
                });
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    //An adapter class to be used with the recycler view...
    public class ImagesAdapter extends RecyclerView.Adapter<ImageViewHolder> {
        //declaring arraylist which will create our model class InstaImage aka images...
        private ArrayList<InstaImage> images;
        //Contructor...
        public ImagesAdapter(ArrayList<InstaImage> images) {
            this.images = images;
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            final InstaImage image = images.get(position);
            //updating the image on that specific view holder...
            holder.updateUI(image);

            final ImageViewHolder vHolder = holder;
            //creating a onclicklistener on the holder that get pressed or the image that get selected and then displayed on the selectedImage view...
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedImage.setImageDrawable(vHolder.imageView.getDrawable());
                }
            });
        }

        //get total itemcount (Image count) to be displayed on the grid...
        @Override
        public int getItemCount() {
            return images.size();
        }


        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //Creating a new view holder of type cardview...and setting it on the ImageViewHolder...
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_image, parent, false);
            return new ImageViewHolder(card);
        }
    }

    //Creating a ImageViewHolder holder class...
    public class ImageViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView)itemView.findViewById(R.id.image_thumb);
        }

        public void updateUI(InstaImage image) {

            DecodeBitmap task = new DecodeBitmap(imageView,image);
            task.execute();
        }
    }

    //creating a class called Decode Bitmap which inherits methods from AsyncTask, to be able to Decode Bitmap using a reference to the image view and      then run the decoding process in background to make the UI faster...
    class DecodeBitmap extends AsyncTask<Void, Void, Bitmap> {
        private final WeakReference<ImageView> mImageViewWeakReference;
        private InstaImage image;

        public DecodeBitmap(ImageView imageView, InstaImage image) {
            mImageViewWeakReference =  new WeakReference<ImageView>(imageView);
            this.image = image;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            return decodeURI(image.getImgResourceUrl().getPath());
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            final ImageView img = mImageViewWeakReference.get();
            //check to see if img is null simply because if a user switches a screen while you are decoding images, you might loose your reference in               the memory...
            if (img != null) {
                img.setImageBitmap(bitmap);
            }
        }
    }

    //A function to decode the image paths...
    public Bitmap decodeURI(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Only scale if we need to ...increase processing time by scaling the image down...
        // (16384 buffer for img processing)
        Boolean scaleByHeight = Math.abs(options.outHeight - 100) >= Math.abs(options.outWidth - 100);
        if(options.outHeight * options.outWidth * 2 >= 16384){
            // Load, scaling to smallest power of 2 that'll get it <= desired dimensions
            double sampleSize = scaleByHeight
                    ? options.outHeight / 1000
                    : options.outWidth / 1000;
            options.inSampleSize =
                    (int)Math.pow(2d, Math.floor(
                            Math.log(sampleSize)/Math.log(2d)));
        }

        options.inJustDecodeBounds = false;
        options.inTempStorage = new byte[512];
        Bitmap output = BitmapFactory.decodeFile(filePath, options);
        return output;
    }

}
