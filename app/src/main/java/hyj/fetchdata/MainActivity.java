package hyj.fetchdata;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import hyj.fetchdata.util.AutoUtil;
import hyj.fetchdata.util.GetPermissionUtil;
import hyj.fetchdata.util.LogUtil;
import hyj.fetchdata.util.OkHttpUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    EditText urlEt;
    SharedPreferences sharedPreferences;

    private static final String[] m={"1个","2个","3个","4个","5个","6个","7个以上"};
    private TextView view ;
    private Spinner spinner;
    private ArrayAdapter<String> adapter;
    WeixinAutoHandler handler = null;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GetPermissionUtil.getReadAndWriteContactPermision(this,MainActivity.this);




        sharedPreferences = GlobalApplication.getContext().getSharedPreferences("url",MODE_PRIVATE);
        String url = sharedPreferences.getString("url","");
        setContentView(R.layout.activity_main);
        urlEt = (EditText)findViewById(R.id.text_url);
        urlEt.setText(url);
        Button testSend = (Button)findViewById(R.id.test_sendMsg);
        Button btn = (Button)findViewById(R.id.open_assist);
        //Button restartWx = (Button)findViewById(R.id.restart_wx);
        testSend.setOnClickListener(testSendListen);
        btn.setOnClickListener(tbnListen);
        //restartWx.setOnClickListener(restartWxListen);

        Button uploadLog = (Button)findViewById(R.id.del_upload_file);
        uploadLog.setOnClickListener(uploadListen);

        view = (TextView) findViewById(R.id.spinnerText);
        spinner = (Spinner) findViewById(R.id.Spinner01);
        //将可选内容与ArrayAdapter连接起来
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,m);
        //设置下拉列表的风格
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //将adapter 添加到spinner中
        spinner.setAdapter(adapter);
        //添加事件Spinner事件监听
        spinner.setOnItemSelectedListener(new SpinnerSelectedListener());
        //设置默认值
        spinner.setVisibility(View.VISIBLE);

        CheckBox autoAddFr = (CheckBox)this.findViewById(R.id.fetch_pic);
        autoAddFr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handler.IS_AUTO_ADDFR=isChecked;
                Toast.makeText(MainActivity.this,isChecked?"开启抓图片":"关闭抓图片",Toast.LENGTH_SHORT).show();
            }
        });

    }
    private View.OnClickListener testSendListen = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SharedPreferences.Editor editor= sharedPreferences.edit();
            final String text = urlEt.getText().toString();
            editor.putString("url",text);
            editor.commit();
            final String json = "[{'groupName':'微信群1','message':'你好！','nickName':'张三','time':'2017-06-07 11:01:55'}]";
            Toast.makeText(MainActivity.this, "已发送数据:\n"+"URL:"+text+"\n"+"data:"+json, Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int count=0;
                    while (true){
                        count = count+1;
                        System.out.println("---发送请求---》"+count);
                        String res = OkHttpUtil.okHttpPost(text,json);
                        LogUtil.d("okHttp",res);
                        AutoUtil.sleep(5000);
                    }

                }
            }).start();
        }
    };
    private View.OnClickListener tbnListen = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            System.out.println("--->dd");
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(MainActivity.this, "找到对应软件名称开启辅助权限", Toast.LENGTH_LONG).show();
        }
    };
    private View.OnClickListener restartWxListen = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ActivityManager am = (ActivityManager) GlobalApplication.getContext().getSystemService(ACTIVITY_SERVICE);
            System.out.println("--->"+ JSON.toJSONString(am));
            am.killBackgroundProcesses("com.tencent.mm");
            Intent intent = new Intent();
            ComponentName cmp = new ComponentName("com.tencent.mm","com.tencent.mm.ui.LauncherUI");
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(cmp);
            startActivityForResult(intent, 0);
        }
    };
    //使用数组形式操作
    class SpinnerSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
            view.setText("监控群数量："+m[arg2]);

            SharedPreferences.Editor editor= sharedPreferences.edit();
            editor.putString("qNum",m[arg2]);
            editor.commit();
        }
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }
    private View.OnClickListener uploadListen = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int a = 5/0;
            File imags = new File("/sdcard/tencent/MicroMsg/WeiXin");
            if(imags.exists()){
                File[] imagsList = imags.listFiles();
                if(imagsList!=null&&imagsList.length>0){
                    for(File f:imagsList){
                        f.delete();
                    }
                }
            }
            File logFiles = new File("/sdcard/A_hyj_log/");
            File crashFiles = new File("/sdcard/A_hyj_crash/");
            doFile(logFiles);
            doFile(crashFiles);
        }
    };

    private  void doFile(File file){
        if(file.exists()){
            File[] logFileList = file.listFiles();
            if(logFileList!=null&&logFileList.length>0){
                for(int i = 0, l = logFileList.length; i<l; i++){
                    if(i==logFileList.length-1){
                        uploadMultiFile("http://39.108.72.49:8080/upload",logFileList[i],logFileList[i].getName(),0);
                    }else{
                        uploadMultiFile("http://39.108.72.49:8080/upload",logFileList[i],logFileList[i].getName(),1);
                    }
                }
            }
        }
    }

    //文件上传到服务器
    public  void uploadMultiFile(final String url, final File file, final String fileName,final int isDel) {
        //开启子线程执行上传，避免主线程堵塞
        new Thread(new Runnable() {
            @Override
            public void run() {
                RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file",fileName, fileBody)
                        .addFormDataPart("name",fileName)//name是对方接收的另一个参数，文件名
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();
                final okhttp3.OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
                OkHttpClient okHttpClient  = httpBuilder
                        //设置超时
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build();
                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "uploadMultiFile() e=" + e);
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseStr = response.body().string();
                        if(isDel==1&&responseStr.indexOf("文件上传成功")>-1){
                            file.delete();
                        }
                        Log.i(TAG, "uploadMultiFile() response=" + responseStr);
                    }
                });
            }
        }).start();
    }
}
