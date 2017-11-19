package eason.bitmapcprs;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.debug.hv.ViewServer;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private ImageView mImageView;
    private ImageView img_glide;
    private Button mBtnSave;
    private final static String ALBUM_PATH
            = Environment.getExternalStorageDirectory() + "/download_test/";
    private Bitmap mBitmap;
    private String mFileName;
    private final static String TAG = "MainActivity";
    private String mSaveMessage;
    private ProgressDialog mSaveDialog = null;
    private static final String IMGURL = "http://imgsrc.baidu.com/imgad/pic/item/b151f8198618367ad995b5f025738bd4b31ce5fb.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewServer.get(this).addWindow(this);
        mImageView = (ImageView) findViewById(R.id.imgSource);
        img_glide = (ImageView) findViewById(R.id.img_glide);
        mBtnSave = (Button) findViewById(R.id.btnSave);
        glideLoadImg();
        new Thread(connectNet).start();
        // 下载图片
        mBtnSave.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mSaveDialog = ProgressDialog.show(MainActivity.this, "保存图片", "图片正在保存中，请稍等...", true);
                new Thread(saveFileRunnable).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ViewServer.get(this).setFocusedWindow(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ViewServer.get(this).removeWindow(this);
    }

    private void glideLoadImg() {
        //圆形裁剪
//        Glide.with(this)
//                .load("http://imgsrc.baidu.com/imgad/pic/item/b151f8198618367ad995b5f025738bd4b31ce5fb.jpg")
//                .bitmapTransform(new CropCircleTransformation(this))
//                .into(img_glide);
//        //圆角处理
//        Glide.with(this)
//                .load(IMGURL)
//                .bitmapTransform(new RoundedCornersTransformation(this, 30, 0, RoundedCornersTransformation.CornerType.ALL))
//                .into(img_glide);
//        //灰度处理
//        Glide.with(this)
//                .load(IMGURL)
//                .bitmapTransform(new GrayscaleTransformation(this))
//                .into(img_glide);

        Glide.with(this).load(IMGURL).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                Log.d(TAG,resource.getByteCount()+"the glide load the image ,the size above");
                img_glide.setImageBitmap(resource);
            }
        });

    }

    private Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mSaveDialog.dismiss();
            Log.d(TAG, mSaveMessage);
            Toast.makeText(MainActivity.this, mSaveMessage, Toast.LENGTH_SHORT).show();
        }
    };

    private Runnable saveFileRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                saveFile(mBitmap, mFileName);
                mSaveMessage = "图片保存成功！";
            } catch (IOException e) {
                mSaveMessage = "图片保存失败！";
                e.printStackTrace();
            }
            messageHandler.sendMessage(messageHandler.obtainMessage());
        }

    };

    /**
     * 保存文件
     *
     * @param bm
     * @param fileName
     * @throws IOException
     */
    public void saveFile(Bitmap bm, String fileName) throws IOException {
        File dirFile = new File(ALBUM_PATH);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        File myCaptureFile = new File(ALBUM_PATH + fileName);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
        bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        bos.flush();
        bos.close();
    }

    /*
     * 连接网络
     * 由于在4.0中不允许在主线程中访问网络，所以需要在子线程中访问
     */
    private Runnable connectNet = new Runnable() {
        @Override
        public void run() {
            try {
//                String filePath = "http://a.hiphotos.baidu.com/image/pic/item/50da81cb39dbb6fda2d331e50324ab18962b376d.jpg";
                String filePath = "http://imgsrc.baidu.com/imgad/pic/item/b151f8198618367ad995b5f025738bd4b31ce5fb.jpg";
                mFileName = "test.jpg";

                //以下是取得图片的两种方法
                //////////////// 方法1：取得的是byte数组, 从byte数组生成bitmap
                byte[] data = getImage(filePath);
                if (data != null) {
                    Log.d(TAG, "the datas length" + data.length);
                    mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);// bitmap
                } else {
                    Toast.makeText(MainActivity.this, "Image error!", 1).show();
                }
                ////////////////////////////////////////////////////////

                //******** 方法2：取得的是InputStream，直接从InputStream生成bitmap ***********/
//                mBitmap = BitmapFactory.decodeStream(getImageStream(filePath));
                //********************************************************************/

                // 发送消息，通知handler在主线程中更新UI
                connectHanlder.sendEmptyMessage(0);
                Log.d(TAG, "set image ...");
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "无法链接网络！", 1).show();
                e.printStackTrace();
            }

        }
    };

    /**
     * Get image from newwork
     *
     * @param path The path of image
     * @return InputStream
     * @throws Exception
     */
    public InputStream getImageStream(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return conn.getInputStream();
        }
        return null;
    }

    /**
     * Get image from newwork
     *
     * @param path The path of image
     * @return byte[]
     * @throws Exception
     */
    public byte[] getImage(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("GET");
        InputStream inStream = conn.getInputStream();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return readStream(inStream);
        }
        return null;
    }

    /**
     * Get data from stream
     *
     * @param inStream
     * @return byte[]
     * @throws Exception
     */
    public static byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        inStream.close();
        return outStream.toByteArray();
    }

    private Handler connectHanlder = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "display image");
            // 更新UI，显示图片
            if (mBitmap != null) {
                mImageView.setImageBitmap(mBitmap);// display image
            }
        }
    };

}
