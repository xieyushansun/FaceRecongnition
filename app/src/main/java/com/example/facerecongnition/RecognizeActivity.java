package com.example.facerecongnition;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.alibaba.fastjson.JSONObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.nanchen.compresshelper.CompressHelper;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;

public class RecognizeActivity extends AppCompatActivity {
    public static final int TAKE_PHOTO = 1;
    public static final int CHOOSE_PHOTO = 2;
    private String outputImagePath = "";
    private Uri imageUri;
    String pictureName; // 服务器传回来的图片名
    String pictureType; // 服务器传回来的图片后缀
    @BindView(R.id.iv_picture)
    MyImageView picture;
    @BindView(R.id.tv_name)
    TextView textRecognize;

    @OnClick(R.id.btn_take_photo) void takephoto(){
//    创建file对象，用于存储拍照后的图片；
        textRecognize.setText("识别结果:");
        File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
        outputImagePath = outputImage.getAbsolutePath();
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= 24) {
            imageUri = FileProvider.getUriForFile(RecognizeActivity.this,
                    BuildConfig.APPLICATION_ID + ".fileProvider", outputImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
        }

        //启动相机程序
        if (ContextCompat.checkSelfPermission(RecognizeActivity.this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
            //如果没有请求权限在这里请求
            ActivityCompat.requestPermissions(RecognizeActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }

    @OnClick(R.id.btn_choose_from_album) void choosealbum(){
        textRecognize.setText("识别结果:");
        if (ContextCompat.checkSelfPermission(RecognizeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RecognizeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            openAlbum();
        }
    }

    @OnClick(R.id.btn_recognize) void Recognize(){
        File file = new File(outputImagePath);
        if (!file.exists()){
            Toasty.error(getApplicationContext(), "请先上传图片！", Toast.LENGTH_SHORT, true).show();
            return;
        }
        if (!file.canRead()){
            Toasty.error(getApplicationContext(), "请先上传图片！", Toast.LENGTH_SHORT, true).show();
            return;
        }
        Toasty.success(getApplicationContext(), "识别中，请勿重复点击... ...", Toast.LENGTH_SHORT, true).show();

        // 图像压缩
        File newFile = new CompressHelper.Builder(this)
                .setQuality(30)    // 默认压缩质量为80
                .build()
                .compressToFile(file);

        // 上传图片到服务器
        OkGo.<String>post(Constant.URL + "/picture/uploadRecognitionPicture")
                .tag(this)
                .params("file", newFile)
                .isMultipart(true)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        String res = response.body();
                        JSONObject jo_temp = JSONObject.parseObject(res);
                        String body = jo_temp.getString("body");
                        JSONObject jo = JSONObject.parseObject(body);
                        String staffName = jo.getString("staffName");
                        if (staffName == null){
                            staffName = "无法识别";
                        }
                        textRecognize.setText("识别结果：" + staffName);
                        textRecognize.invalidate();
//                        getRecognizeResult();
                    }
                    @Override
                    public void onError(Response<String> response) {
                        Toasty.error(getApplicationContext(), "上传出错", Toast.LENGTH_SHORT, true).show();
                    }
                });
    }

    public void getRecognizeResult(){
        if (pictureName.isEmpty() || pictureType.isEmpty()){
            Toasty.error(getApplicationContext(), "请先上传图片！", Toast.LENGTH_SHORT, true).show();
            return;
        }
        OkGo.<String>post(Constant.URL + "/picture/uploadRecognitionPicture")
                .tag(this)
                .params("pictureName ", pictureName)
                .params("pictureType", pictureType)
                .isMultipart(true)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
//                        String res = response.body();
//                        JSONObject jo_temp = JSONObject.parseObject(res);
//                        String body = jo_temp.getString("body");
//                        JSONObject jo = JSONObject.parseObject(body);
//                        picture.setImageURL("http://121.48.163.57:18080/FaceRecognition/picture/PreviewPictureRecordFile?pictureName=33ede0227d21432cb8f61ee4949d2f2012701&pictureType=png");
                        String url = String.format("http://121.48.163.57:18080/FaceRecognition/picture/PreviewPictureRecordFile?pictureName=%s&pictureType=%s", pictureName, pictureType);
                        picture.setImageURL(url);
                        picture.invalidate();
                        textRecognize.setText("识别结果：张三");
                        pictureName = "";
                        pictureType = "";

                        Toasty.success(getApplicationContext(), "识别成功！", Toast.LENGTH_SHORT, true).show();
                    }
                    @Override
                    public void onError(Response<String> response) {
                        Toasty.error(getApplicationContext(), "识别出错", Toast.LENGTH_SHORT, true).show();
                    }
                });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//remove title bar  即隐藏标题栏
        Objects.requireNonNull(getSupportActionBar()).hide();// 隐藏ActionBar
        setContentView(R.layout.activity_test);
        ButterKnife.bind(this);
    }

    //打开相册
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "you denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { // 显示拍照图片
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bm = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        ExifInterface exifInterface = new ExifInterface(outputImagePath);
                        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        float degree = 0;
                        switch (orientation) {
                            case ExifInterface.ORIENTATION_ROTATE_90:
                                degree = 90;
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_180:
                                degree = 180;
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_270:
                                degree = 270;
                                break;
                        }
                        picture.setImageBitmap(bm);
                        picture.setPivotX(picture.getWidth()/2);
                        picture.setPivotY(picture.getHeight()/2);
                        // 旋转
                        picture.setRotation(degree);
//                        String path = imageUri.getEncodedPath();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {  //4.4及以上的系统使用这个方法处理图片；
                        try {
                            handleImageOnKitKat(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            handleImageBeforeKitKat(data);  //4.4及以下的系统使用这个方法处理图片
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            default:
                break;
        }
    }
    // 4.4及以下的系统使用这个方法处理图片
    private void handleImageBeforeKitKat(Intent data) throws IOException {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) throws IOException {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            float degree = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }

            System.out.println("高度：" + bitmap.getHeight());
            System.out.println("宽度：" + bitmap.getWidth());
            picture.setPivotX(picture.getWidth()/2);
            picture.setPivotY(picture.getHeight()/2);
            // 旋转90度
            picture.setRotation(degree);
            picture.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 4.4及以上的系统使用这个方法处理图片
     *
     * @param data
     */
    // 相册中选择
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) throws IOException {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果document类型的Uri,则通过document来处理
            String docID = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docID.split(":")[1];     //解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;

                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/piblic_downloads"), Long.valueOf(docID));
                imagePath = getImagePath(contentUri, null);
            }

        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型的uri，则使用普通方式使用
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是file类型的uri，直接获取路径即可
            imagePath = uri.getPath();
        }
        outputImagePath = imagePath;
        displayImage(imagePath);
    }
}
