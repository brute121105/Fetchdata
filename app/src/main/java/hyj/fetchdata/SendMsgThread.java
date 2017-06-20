package hyj.fetchdata;

import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import hyj.fetchdata.util.AutoUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.ContentValues.TAG;

/**
 * Created by asus on 2017/6/18.
 */

public class SendMsgThread extends Thread{
    String url;
    List<Msg> msgs;
    File file;
    String fileName;
    //List<List<Msg>> sendMsgs;

    public SendMsgThread(String url, File file,String fileName){
        this.url = url;
        //this.msgs = msgs;
        this.file = file;
        this.fileName = fileName;
        //this.sendMsgs = sendMsgs;
    }
  @Override
    public void run() {
        super.run();
        while (true){
            synchronized (this){
                if(WeixinAutoHandler.sendMsgs.size()>0){
                    final  List<Msg> msgs = WeixinAutoHandler.sendMsgs.remove(0);
                    String json = JSON.toJSONString(msgs);
                    System.out.println("------开始发送消息66--------"+json);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            uploadMultiFile(url,msgs,null,null);
                        }
                    }).start();

                }
            }
        }

    }
    public void up(){
        List<Msg> msgs = WeixinAutoHandler.sendMsgs.remove(0);
        uploadMultiFile(url,msgs,null,null);
    }
    //文件上传到服务器
    private synchronized void  uploadMultiFile(String url, final List<Msg> msgs, File file, final String fileName) {
        if(file==null) file = new File("");
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        if(fileName!=null){
            System.out.println("fileName--->"+fileName);
            builder. addFormDataPart("file",fileName, fileBody)
                    .addFormDataPart("name",fileName);
        }

        if(msgs!=null){
            builder. addFormDataPart("data",JSON.toJSONString(msgs));
        }

        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        final okhttp3.OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient  = httpBuilder
                //设置超时
                .readTimeout(90, TimeUnit.SECONDS)
                .connectTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "请求失败 e=" + e);
                if(fileName!=null){
                    Log.i(TAG, "图片上传失败-->" +fileName );
                    //uploadFiles.add(fileName);
                }else if(msgs!=null){
                    //sendMsgs.add(msgs);
                    Log.i(TAG, "数据发送失败-->" +JSON.toJSONString(msgs));
                }
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseStr = response.body().string();
                if(fileName!=null){
                    if(responseStr.indexOf("文件上传成功")>-1){
                        Log.i(TAG, "图片上传成功 response=" +responseStr );
                    }else{
                        Log.i(TAG, "图片上传失败 response=" +responseStr );
                        //uploadFiles.add(fileName);
                    }
                }else if(msgs!=null){
                    if(responseStr.indexOf("已上传数据")>-1){
                        Log.i(TAG, "数据发送成功 response=" +responseStr );
                    }else{
                        Log.i(TAG, "数据发送失败 response=" +responseStr );
                        //sendMsgs.add(msgs);
                    }
                }
            }
        });
    }
}
