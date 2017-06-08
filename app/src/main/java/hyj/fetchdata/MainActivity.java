package hyj.fetchdata;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import java.io.IOException;

import hyj.fetchdata.util.GetPermissionUtil;
import hyj.fetchdata.util.LogUtil;
import hyj.fetchdata.util.OkHttpUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    EditText urlEt;
    SharedPreferences sharedPreferences;

    private static final String[] m={"1个","2个","3个","4个","5个","6个","7个以上"};
    private TextView view ;
    private Spinner spinner;
    private ArrayAdapter<String> adapter;


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
                    String res = OkHttpUtil.okHttpPost(text,json);
                    LogUtil.d("okHttp",res);

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
}
