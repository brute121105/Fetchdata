package hyj.fetchdata;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import hyj.fetchdata.util.AutoUtil;
import hyj.fetchdata.util.LogUtil;
import hyj.fetchdata.util.OkHttpUtil;

public class MyService extends Service {
    public MyService() {
    }

    @Override
    public void onCreate() {
        System.out.println("---myService start----");
        SharedPreferences sharedPreferences = GlobalApplication.getContext().getSharedPreferences("url",MODE_PRIVATE);
        final String url = sharedPreferences.getString("url","");
        new Thread(new Runnable() {
            @Override
            public void run() {
                int count=0;
                while (true){
                    count = count+1;
                    System.out.println("---发送请求---》"+count);
                    String res = OkHttpUtil.okHttpPost(url,"123");
                    LogUtil.d("okHttp",res);
                    AutoUtil.sleep(5000);
                }

            }
        }).start();

        Notification.Builder builder = new Notification.Builder(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MyService.class), 0);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.a);
        builder.setTicker("Foreground Service Start");
        builder.setContentTitle("Foreground Service");
        builder.setContentText("Make this service run in the foreground.");
        Notification notification = builder.build();
        this.startForeground(1,notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
