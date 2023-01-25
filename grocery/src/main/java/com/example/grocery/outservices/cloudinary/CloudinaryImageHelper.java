package com.example.grocery.outservices.cloudinary;

public class CloudinaryImageHelper {

    public static String getImagePublicIdFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("."));
    }
}
