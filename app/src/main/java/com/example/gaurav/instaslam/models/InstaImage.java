package com.example.gaurav.instaslam.models;

import android.net.Uri;

/**
 * Created by Gaurav on 2017-05-02.
 */

public class InstaImage {

    private Uri imgResourceUrl;

    public InstaImage(Uri imgResourceUrl) {
        this.imgResourceUrl = imgResourceUrl;
    }

    public Uri getImgResourceUrl() {
        return imgResourceUrl;
    }

}
