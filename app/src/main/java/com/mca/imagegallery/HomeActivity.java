package com.mca.imagegallery;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.mca.imagegallery.Model.GalleryImage;
import com.mca.imagegallery.Model.Image;
import com.mca.imagegallery.Model.User;
import com.mca.imagegallery.helper.NavMenu;
import com.mca.imagegallery.helper.Permissions;
import com.mca.imagegallery.helper.Utils;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout cardContainer, galleryLeft, galleryRight;

    private FirebaseFirestore db;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_home);

        new NavMenu(this);

        cardContainer = findViewById(R.id.card_container);
        galleryLeft = findViewById(R.id.gallery_left);
        galleryRight = findViewById(R.id.gallery_right);

        db = FirebaseFirestore.getInstance();

        fetchImages();
    }

    private void fetchImages() {

        cardContainer.removeAllViews();
        galleryLeft.removeAllViews();
        galleryRight.removeAllViews();

        db.collection("images")
            .orderBy("id", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    List<GalleryImage> images = task.getResult().toObjects(GalleryImage.class);
                    setRecentImages(images);
                    setBrowseAllImages(images);
                } else {
                    Utils.toast(this, task.getException().getMessage());
                }
            })
            .addOnFailureListener(ex -> Utils.toast(this, ex.getMessage()));
    }

    private void setRecentImages(List<GalleryImage> images) {

        int i = 0;
        for (GalleryImage image: images) {

            if(i >= 10) break;

            LinearLayout card = (LinearLayout) getLayoutInflater().inflate(R.layout.home_card, null);
            ImageView imageView = card.findViewById(R.id.iv_home_img);
            int size = Resources.getSystem().getDisplayMetrics().widthPixels - (cardContainer.getPaddingRight() * 2);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            ImageView ivProfile = card.findViewById(R.id.iv_profile);
            TextView tvName = card.findViewById(R.id.tv_name);
            TextView tvUserId = card.findViewById(R.id.tv_user_id);

            Picasso.get().load(image.getUrl()).into(imageView);

            image.getUser()
                .get()
                .addOnCompleteListener(task -> {
                    User user = task.getResult().toObject(User.class);
                    Picasso.get().load(user.getProfile_url()).into(ivProfile);
                    tvName.setText(user.getName());
                    tvUserId.setText("@".concat(user.getEmail().substring(0, user.getEmail().lastIndexOf('@'))));
                    card.setOnClickListener(view -> viewImage(image.getUrl(), user));
                })
                .addOnFailureListener(ex -> Utils.toast(this, ex.getMessage()));

            cardContainer.addView(card);
            i++;
        }
    }

    private void setBrowseAllImages(List<GalleryImage> images) {

        if(images.size() > 0) {
            int randomNum = new Random().nextInt(images.size());

            GalleryImage image = images.get(randomNum);

            ImageView imageView = (ImageView) getLayoutInflater().inflate(R.layout.gallery_image, null);
            Picasso.get().load(image.getUrl()).into(imageView);

            image.getUser()
                    .get()
                    .addOnCompleteListener(task -> {
                        User user = task.getResult().toObject(User.class);
                        imageView.setOnClickListener(view -> viewImage(image.getUrl(), user));
                    })
                    .addOnFailureListener(ex -> Utils.toast(this, ex.getMessage()));

            if (galleryRight.getChildCount() < galleryLeft.getChildCount())
                galleryRight.addView(imageView);
            else
                galleryLeft.addView(imageView);

            images.remove(randomNum);
            setBrowseAllImages(images);
        }
    }

    private void browseAllImages() {
        galleryLeft.removeAllViews();
        galleryRight.removeAllViews();

        db.collection("users")
            .get()
            .addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    List<User> users = task.getResult().toObjects(User.class);
                    for (User user : users) {
                        displayImages(user);
                    }
                } else {
                    Utils.toast(this, task.getException().getMessage());
                }
            })
            .addOnFailureListener(ex -> Utils.toast(this, ex.getMessage()));
    }

    private void displayImages(User user) {
        db.collection("users")
            .document(Utils.getID(user.getEmail()))
            .collection("images")
            .get()
            .addOnCompleteListener(task -> {
                List<Image> images = task.getResult().toObjects(Image.class);
                int i = galleryLeft.getChildCount() + galleryRight.getChildCount();
                for (Image image : images) {
                    ImageView imageView = (ImageView) getLayoutInflater().inflate(R.layout.gallery_image, null);
                    imageView.setOnClickListener(view -> viewImage(image.getUrl(), user));
                    Picasso.get().load(image.getUrl()).into(imageView);

                    if (i % 2 == 0) galleryLeft.addView(imageView);
                    else galleryRight.addView(imageView);
                    i++;
                }
            })
            .addOnFailureListener(ex -> Utils.toast(this, ex.getMessage()));
    }

    private void viewImage(String imageUrl, User user) {
        Intent intent = new Intent(this, ViewImageActivity.class);
        intent.putExtra("profile_url", user.getProfile_url());
        intent.putExtra("name", user.getName());
        intent.putExtra("city", user.getCity());
        intent.putExtra("email", user.getEmail());
        intent.putExtra("image_url", imageUrl);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        NavMenu.onActivityResult(requestCode, resultCode, data);
    }

    // Override function for exiting application when double back clicked
    private boolean doubleBackToExitPressedOnce = false;
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            ActivityCompat.finishAffinity(this);
            return;
        }
        doubleBackToExitPressedOnce = true;
        Utils.toast(this, "Press BACK again to exit");

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }
}
