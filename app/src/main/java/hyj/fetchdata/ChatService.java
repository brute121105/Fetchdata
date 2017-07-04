package hyj.fetchdata;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import hyj.fetchdata.util.AutoUtil;
import hyj.fetchdata.util.Constants;
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
import static android.os.PowerManager.*;
import static hyj.fetchdata.WeixinAutoHandler.sendMsgs;

public class ChatService extends AccessibilityService {
    Map<String,Set<String>> temp = new HashMap<String,Set<String>>();
    Map<String,String> record = new HashMap<String,String>();
    static boolean isOver=true;
    static boolean isStart;
    static int baseNum = 9;
    AccessibilityNodeInfo root=null;
    static String url;
    int qNum;
    List<Msg> saveTempDbMsgs = new ArrayList<Msg>();
    Set<String> oldSet;
    static  List<String> uploadFiles = new ArrayList<String>();
    public ChatService() {
        SharedPreferences sharedPreferences = GlobalApplication.getContext().getSharedPreferences("url",MODE_PRIVATE);
        url = sharedPreferences.getString("url","");
        qNum = Integer.parseInt(sharedPreferences.getString("qNum","").replaceAll("[个以上]",""));
        LogUtil.d("url|qnum",url+"--"+qNum);
        AutoUtil.recordAndLog(record,Constants.CHAT_LISTENING);
        isStart=true;

    }
    String qName="";
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String className = event.getClassName()+"";
        String packageName = event.getPackageName()+"";
        isStart=false;
        int eventType = event.getEventType();
        System.out.println("---->"+eventType);
        //---jiesuo
        if(!pm.isScreenOn()&&eventType==AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED){
            System.out.println("---->"+eventType+" packageName:"+packageName+" className:"+className);
            nocificationPro(event);
            AutoUtil.recordAndLog(record,Constants.CHAT_ACTION_04);
            return;
        }
        //--jiesuo
        root = getRootInActiveWindow();
        //LogUtil.d("isOver|action|eventType","isOver:"+isOver+"--"+"action:"+record.get("recordAction")+"--"+"eventType:"+eventType+"");
        if(isOver){

            if(root==null){
                Toast.makeText(GlobalApplication.getContext(), "开启失败，请退出微信重新打开!", Toast.LENGTH_LONG).show();
                LogUtil.d("开启失败，请退出微信重新打开root",root+"");
                return;
            }

            if(AutoUtil.checkAction(record,Constants.CHAT_LISTENING)){
                List<AccessibilityNodeInfo> listView= root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bny");
                if(listView==null||listView.size()==0){
                    LogUtil.d("chatService","列表信息为空");
                    return;
                }
                getqImageANdNameThenClidk(listView);
            }


            if(AutoUtil.checkAction(record,Constants.CHAT_ACTION_04)){
                MyThread thread = new MyThread(root);
                thread.start();
            }

        }
    }

    public  static void getChild(AccessibilityNodeInfo node){
        System.out.println("-----------start---------");
        if(node!=null){
            int count = node.getChildCount();
            System.out.println("child count"+count+"node text-->"+node.getText()+"  node clsName-->"+node.getClassName()+" desc"+node.getContentDescription());
            if(count>0){
                for(int i=0,l=count;i<l;i++){
                    AccessibilityNodeInfo child = node.getChild(i);
                    //getChild(child);
                    System.out.println(i+" child text-->"+child.getText()+" child clsName-->"+child.getClassName()+" desc"+node.getContentDescription());
                }
            }
        }
        System.out.println("-----------end---------");
    }
    @Override
    public void onInterrupt() {
    }
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        //sendMsgs = Collections.synchronizedList(sendMsgs);
        pm=(PowerManager)GlobalApplication.getContext().getSystemService(Context.POWER_SERVICE);

        ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(5);
        stpe.scheduleWithFixedDelay(new SendMsgByTCP(),3,1,TimeUnit.SECONDS);
        if(WeixinAutoHandler.IS_AUTO_ADDFR){
            stpe.scheduleWithFixedDelay(new FileThread(),3,2,TimeUnit.SECONDS);
        }
        //stpe.scheduleWithFixedDelay(new SendMsgRunner(),5,5,TimeUnit.SECONDS);
    }
    private class MyThread extends Thread{
        AccessibilityNodeInfo root;
        public MyThread(AccessibilityNodeInfo root){
            this.root = root;
        }
        @Override
        public void run() {
            System.out.println("----------run start-----------");
            isOver = false;

            List<AccessibilityNodeInfo> ims=null;
            List<AccessibilityNodeInfo> images1 = null;
            int tryCount =0;
            while (true){
                AutoUtil.sleep(200);
                AccessibilityNodeInfo msgRoot = getRootInActiveWindow();
                if(msgRoot==null){
                    isOver = true;
                    return;
                }
                qName = getQnameById(msgRoot);
                ims = msgRoot.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/im");
                images1 =  msgRoot.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a72");
                if(ims==null&&images1==null||"".equals(qName)){
                    LogUtil.d("chatService","消息列表为空 or qName is null");
                    tryCount = tryCount+1;
                    if(tryCount==20){
                        AutoUtil.recordAndLog(record,Constants.CHAT_LISTENING);
                        isOver = true;
                        return;
                    }
                    continue;
                }
                break;
            }
            if(WeixinAutoHandler.IS_AUTO_ADDFR){
                forImageProcess(images1);
            }

            AccessibilityNodeInfo msgRoot2 = getRootInActiveWindow();
            AccessibilityNodeInfo backBtn = AutoUtil.findNodeInfosById(msgRoot2,"com.tencent.mm:id/go");;
            int wait2MsgNum=0;
            while (backBtn==null){
                AutoUtil.sleep(300);
                LogUtil.d("myLog","等待获取返回按钮"+wait2MsgNum);
                msgRoot2 = getRootInActiveWindow();
                backBtn = AutoUtil.findNodeInfosById(msgRoot2,"com.tencent.mm:id/go");
                wait2MsgNum = wait2MsgNum+1;
                if(wait2MsgNum==20){
                    isOver = true;
                    AutoUtil.recordAndLog(record,Constants.CHAT_LISTENING);
                    LogUtil.d("myLog","等待超时，尝试20次无法进入消息列表，重置监听状态!");
                    return;
                }
            }
            ims = msgRoot2.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/im");
            messageProcess(ims,msgRoot2);

            AutoUtil.performClick(backBtn,record,"返回好友列表");
            if(AutoUtil.checkAction(record,"返回好友列表")){
                AutoUtil.recordAndLog(record,Constants.CHAT_LISTENING);
            }
            isOver = true;
            System.out.println("----------run end-----------");
        }

        @Override
        public void destroy() {
            super.destroy();
            isOver=true;
        }

    }

    private void getqImageANdNameThenClidk(List<AccessibilityNodeInfo> listView){
        int childCount = listView.get(0).getChildCount();
        //int index = qNum>childCount?childCount:qNum;
        if(childCount>baseNum)
            for(int i=baseNum,l=childCount;i<l;i++){
                String imgText = "",qNickText = "";
                AccessibilityNodeInfo childNode = listView.get(0).getChild(i);
                AccessibilityNodeInfo imgNode = AutoUtil.findNodeInfosById(childNode,"com.tencent.mm:id/ia");
                AccessibilityNodeInfo nickNode = AutoUtil.findNodeInfosById(childNode,"com.tencent.mm:id/agw");
                if(imgNode!=null){
                    imgText = (imgNode.getText()+"").replace("null","").replace("NULL","");
                }
                if(nickNode!=null){
                    qNickText = nickNode.getText()+"";
                }
                if(!"".equals(imgText)&&!"腾讯新闻".equals(qNickText)){
                    qName = qNickText;
                    AccessibilityNodeInfo personNode = AutoUtil.fineNodeByIdAndText(getRootInActiveWindow(),"com.tencent.mm:id/agw",qNickText);
                    AutoUtil.performClick(personNode,record,Constants.CHAT_ACTION_04,1000);
                    break;
                }
            }
    }

    private void forImageProcess(List<AccessibilityNodeInfo> images){
        if(images==null||images.isEmpty()) return;
        int count = 0;
        while (true){
            imageProcess(images.get(count));
            AccessibilityNodeInfo backBtn=null;
            AccessibilityNodeInfo root=null;
            while (backBtn==null){
                AutoUtil.sleep(200);
                root = getRootInActiveWindow();
                backBtn = AutoUtil.findNodeInfosById(root,"com.tencent.mm:id/go");
                if(backBtn!=null){
                    break;
                }
                System.out.println("record--->图片等待获取返回按钮");
            }
            images = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a72");
            if(images.size()-1<=count){
                break;
            }
            count = count+1;
        }
    }
    private boolean imageProcess(AccessibilityNodeInfo image){
        boolean isSameImage = true;
        if(image!=null){

            String nickName = "";
            AccessibilityNodeInfo pParent = image.getParent().getParent();
            if(pParent.getChildCount()==3){
                nickName = pParent.getChild(1).getText()+"";
            }

            AutoUtil.performClick(image,record,"点击图片",1500);
            if(AutoUtil.checkAction(record,"点击图片")){
                boolean isSaveImage=false;
                int countTimes = 0;
                while (!isSaveImage){
                    countTimes = countTimes+1;
                    if(countTimes==80){
                        AutoUtil.performBack(ChatService.this,record,"等待8s退出");
                        List<Msg> msgs = new ArrayList<Msg>();
                        Msg msg = new Msg(qName,nickName,"系统提示：图片加载失败 or 视频或其他不支持",AutoUtil.getCurrentTime());
                        msgs.add(msg);
                        //sendMsgs.add(msgs);

                        return isSameImage;
                    }
                    AccessibilityNodeInfo rootImage = getRootInActiveWindow();
                    AutoUtil.sleep(100);
                    AccessibilityNodeInfo saveImageNode = AutoUtil.findNodeInfosById(rootImage,"com.tencent.mm:id/b8w");
                    if(saveImageNode==null){
                        LogUtil.d("chatService","保存按钮获取失败"+countTimes);
                        continue;
                    }
                    AutoUtil.performClick(saveImageNode,record,"保存图片");
                    String filename =checkFileExist();
                    if(!"".equals(filename)){
                        isSameImage = false;
                        uploadFiles.add(filename);
                        List<Msg> msgs = new ArrayList<Msg>();
                        Msg msg = new Msg(qName,nickName,filename,AutoUtil.getCurrentTime());
                        msgs.add(msg);
                        sendMsgs.add(msgs);
                    }
                    isSaveImage = true;
                    if(AutoUtil.checkAction(record,"保存图片")){
                        AutoUtil.performBack(ChatService.this,record,"保存图片->全局返回");
                    }
                }
            }
        }
        return isSameImage;
    }
    private String checkFileExist(){
        String filename = "";
        File file = new File("/sdcard/tencent/MicroMsg/WeiXin");
        if(file.exists()){
            File[] files = file.listFiles();
            if(files!=null&&files.length>0){
                boolean fileIsExist = false;
                int fileNums = files.length;
                File newFile = files[fileNums-1];
                if(files.length>1){
                    for(int i=fileNums;i>=2;i--){
                        if(newFile.length()==files[i-2].length()){
                            fileIsExist = true;
                            newFile.delete();
                            System.out.println("----->删除图片");
                        }
                    }
                }
                if(!fileIsExist){
                    System.out.println("----->新图片");
                    filename = newFile.getName();
                }
            }
        }
        return filename;
    }
    class CompratorByLastModified implements Comparator<File> {
        public int compare(File f1, File f2) {
            long diff = f1.lastModified() - f2.lastModified();
            if (diff > 0)
                return 1;
            else if (diff == 0)
                return 0;
            else
                return -1;
        }

        public boolean equals(Object obj) {
            return true;
        }
    }
    class FileThread implements Runnable{
        @Override
        public void run() {
            if(uploadFiles.size()>0){
                String filename = uploadFiles.remove(0);
                File uploadFile = new File("/sdcard/tencent/MicroMsg/WeiXin/"+filename);
                LogUtil.d("autoFetch","------开始上传文件----"+filename);
                uploadMultiFile(url,null,uploadFile,filename);
                //uploadMultiFile("http://39.108.72.49:8080/upload",null,uploadFile,filename);
            }
        }
    }

    static class SendMsgByTCP implements Runnable{
        @Override
        public void run() {
            if(sendMsgs.size()>0){
                List<Msg> msgs = sendMsgs.remove(0);
                String json = JSON.toJSONString(msgs);
                LogUtil.d("autoFetch","----TCP--开始发送消息--------"+json);
                uploadMultiFile(url,msgs,null,null);
            }
        }
    }

    //文件上传到服务器
    private  static void  uploadMultiFile(String url, final List<Msg> msgs, File file, final String fileName) {
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
                    uploadFiles.add(fileName);
                }else if(msgs!=null){
                    sendMsgs.add(msgs);
                    Log.i(TAG, "数据发送失败-->" +JSON.toJSONString(msgs));
                }
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseStr = response.body().string();
                if(fileName!=null){
                    if(validRes(responseStr)){
                        LogUtil.d("autoFetch", "图片上传成功 response=" +responseStr );
                    }else{
                        LogUtil.d("autoFetch", "图片上传失败 response=" +responseStr );
                        //uploadFiles.add(fileName);
                    }
                }else if(msgs!=null){
                    if(validRes(responseStr)){
                        LogUtil.d("autoFetch", "数据发送成功 response=" +responseStr );
                    }else{
                        LogUtil.d("autoFetch", "数据发送失败 response=" +responseStr );
                        //sendMsgs.add(msgs);
                    }
                }
            }
        });
    }
    private static boolean validRes(String res){
        boolean flag = false;
        if(res!=null){
            if(res.contains("status\":1")||res.contains("status':1")||res.contains("status\":\"1")||res.contains("status':'1")){
                System.out.println("----true----");
                flag = true;
            }
        }
        return flag;
    }

    private String getQnameById(AccessibilityNodeInfo root){
        String qName = "";
        AccessibilityNodeInfo qNode = AutoUtil.findNodeInfosById(root,"com.tencent.mm:id/gp");
        if(qNode!=null){
            qName = (qNode.getText()+"").replaceAll("\\([\\d]{1,3}\\)","");
        }
        return qName;
    }
    private void messageProcess(List<AccessibilityNodeInfo> ims,AccessibilityNodeInfo msgRoot){
        oldSet = temp.get(qName);
        if(oldSet==null||oldSet.isEmpty()){
            oldSet = new HashSet<String>();
            List<Msg> dMsgs = DataSupport.where("groupName=?",qName).find(Msg.class);
            for(Msg msg:dMsgs){
                oldSet.add(msg.getNickName()+msg.getMessage());
            }
        }
        System.out.println("oldSet---->"+oldSet);
        if(ims!=null&&ims.size()>0){
            List<Msg> msgs = new ArrayList<Msg>();
            Set<String> newSet = new HashSet<String>();
            for(int i=0,l=ims.size();i<l;i++){
                AccessibilityNodeInfo im = ims.get(i);
                if(im==null) continue;
                String nickName = "";
                String text = im.getText()+"";
                //11
                AccessibilityNodeInfo textParent = im.getParent();
                if(textParent==null) continue;
                //getChild(textParent.getChild(0));
                if(textParent.getChildCount()==3){
                    nickName = textParent.getChild(1).getText()+"";
                }else if(textParent.getChildCount()==4){//带时间 或 声音
                    nickName = textParent.getChild(2).getText()+"";
                    if("null".equals(nickName)){//声音
                        text = textParent.getChild(3).getText()+"";
                        if(text.matches("[\\d]{1,2}\"")){
                            nickName = textParent.getChild(1).getText()+"";
                            text = ("语音"+text+"秒").replace("\"","");
                        }
                    }
                }
                //--个人聊天--nickname---start,个人抓自己发送信息，然后头顶是时间将会出现nickname==text
                if("".equals(nickName)||"null".equals(nickName)){
                    if(textParent.getChildCount()==2)
                    nickName = (textParent.getChild(1).getContentDescription()+"").replace("头像","");
                }
                if ("null".equals(nickName)) {
                    if(textParent.getChildCount()==1)
                    nickName = (textParent.getChild(0).getContentDescription()+"").replace("头像","");
                }
                LogUtil.d("autoFetch","nickName:"+nickName+" text:"+text);
                if(text.equals(nickName)) continue;
                if("".equals(nickName)||"null".equals(nickName)) continue;
                if("".equals(text)||"null".equals(text)) continue;
                //--个人聊天--nickname---end
                //22
                Msg msg = new Msg(qName,nickName,text,AutoUtil.getCurrentTime());
                saveTempDbMsgs.add(msg);

                newSet.add(nickName+text);
                if(oldSet.isEmpty()){
                    msgs.add(msg);
                    //System.out.println("-------msgText--->群名:"+qName+" "+"昵称："+nickName+" 消息："+text);
                }else{
                    if(!oldSet.contains(nickName+text)){
                        msgs.add(msg);
                        //System.out.println("--------------msgText--->群名:"+qName+" "+"昵称："+nickName+" 消息："+text);
                    }
                }
            }
            LogUtil.d("msg",JSON.toJSONString(msgs));
            DataSupport.deleteAll(Msg.class,"groupName=?",qName);
            DataSupport.saveAll(saveTempDbMsgs);

            if(msgs.size()>0){
                sendMsgs.add(msgs);
            }
            temp.put(qName,newSet);
        }
    }


    private void nocificationPro(AccessibilityEvent event){
        List<CharSequence>  texts = event.getText();
        if (!texts.isEmpty()) {
            for (CharSequence text : texts) {
                String content = text.toString();
                LogUtil.d("demo---->", "text:" + content);
                //--
                if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                    wakeAndUnlock(true);
                    LogUtil.d("dem---->o", "canGet=true");
                    //canGet = true;
                    try {
                        Notification notification = (Notification) event.getParcelableData();
                        PendingIntent pendingIntent = notification.contentIntent;
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }
                //--

            }
        }
    }

    //唤醒屏幕和解锁
    private PowerManager pm;
    private PowerManager.WakeLock wl = null;
    private void wakeAndUnlock(boolean unLock) {
        if(unLock){
            if(!pm.isScreenOn()) {
                wl = pm.newWakeLock(SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP , "bright");
                wl.acquire();
                LogUtil.d("demo-----》", "亮屏");
            }
        }
    }

}
