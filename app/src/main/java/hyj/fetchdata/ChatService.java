package hyj.fetchdata;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hyj.fetchdata.util.AutoUtil;
import hyj.fetchdata.util.Constants;
import hyj.fetchdata.util.LogUtil;
import hyj.fetchdata.util.OkHttpUtil;

public class ChatService extends AccessibilityService {
    Map<String,Set<String>> temp = new HashMap<String,Set<String>>();
    Map<String,String> record = new HashMap<String,String>();
    static boolean isOver=true;
    static boolean isStart;
    static int baseNum = 9;
    Object lock = new Object();
    AccessibilityNodeInfo root=null;
    String url;
    int qNum;
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
        if(isStart){
            new CheckThread().start();
        }
        isStart=false;
        int eventType = event.getEventType();
        root = getRootInActiveWindow();
        LogUtil.d("isOver|action|eventType","isOver:"+isOver+"--"+"action:"+record.get("recordAction")+"--"+"eventType:"+eventType+"");
        if(isOver){
            MyThread thread = new MyThread(root);
            thread.start();
        }
    }
    public static void performClick(AccessibilityNodeInfo nodeInfo) {
        if(nodeInfo == null)  return;
        if(nodeInfo.isClickable()) {
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            performClick(nodeInfo.getParent());
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
    private class SendMsg extends Thread{
        String json;
        public SendMsg(String json){
            this.json = json;
        }
        @Override
        public void run() {
            LogUtil.d("okHttp",OkHttpUtil.okHttpPost(url,json));
        }
    }
    private class MyThread extends Thread{
        AccessibilityNodeInfo root;
        List<Msg> msgs = new ArrayList<Msg>();
        public MyThread(AccessibilityNodeInfo root){
            this.root = root;
        }
        @Override
        public void run() {
            if(root==null){
                LogUtil.d("root",root+"");
                return;
            }
            isOver = false;
            //LogUtil.d("isove","置为false");
            if(AutoUtil.checkAction(record,Constants.CHAT_LISTENING)){
                List<AccessibilityNodeInfo> listView= root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bny");
                if(listView==null||listView.size()==0){
                    isOver = true;
                    return;
                }
                int childCount = listView.get(0).getChildCount();
                int index = qNum>childCount?childCount:qNum;
                for(int i=baseNum,l=childCount;i<l;i++){
                    String imgText = "",qNickText = "";
                    AccessibilityNodeInfo childNode = listView.get(0).getChild(i);
                    AccessibilityNodeInfo imgNode = AutoUtil.findNodeInfosById(childNode,"com.tencent.mm:id/ia");
                    AccessibilityNodeInfo nickNode = AutoUtil.findNodeInfosById(childNode,"com.tencent.mm:id/agw");
                    if(imgNode!=null){
                        imgText = imgNode.getText().toString();
                    }
                    if(nickNode!=null){
                        qNickText = nickNode.getText().toString();
                    }
                    if(!"".equals(imgText)&&!"腾讯新闻".equals(qNickText)){
                        qName = qNickText;
                        AccessibilityNodeInfo personNode = AutoUtil.fineNodeByIdAndText(getRootInActiveWindow(),"com.tencent.mm:id/agw",qNickText);
                        AutoUtil.performClick(personNode,record,Constants.CHAT_ACTION_04,1000);
                        isOver=true;
                        return;
                    }
                }
                isOver=true;
                return;
            }
            if(AutoUtil.checkAction(record,Constants.CHAT_ACTION_04)){
                List<AccessibilityNodeInfo> receviceMsgs = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/p");
                List<AccessibilityNodeInfo> huaweiMsgs = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/agy");

                Set<String> oldSet = temp.get(qName);
                Set<String> newSet = new HashSet<String>();
                //---huawei手机----
                List<AccessibilityNodeInfo> iks = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ik");
                List<AccessibilityNodeInfo> ims = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/im");
                if(iks!=null&&iks.size()>0&&ims!=null&&ims.size()>0){
                    for(int i=0,l=iks.size();i<l;i++){
                        String nickName = (iks.get(i).getContentDescription()+"").replace("头像","");
                        String text = ims.get(i).getText()+"";
                        newSet.add(nickName+text);
                        if(oldSet==null){
                            Msg msg = new Msg();
                            msg.setGroupName(qName);
                            msg.setNickName(nickName);
                            msg.setMessage(text);
                            msg.setTime(AutoUtil.getCurrentTime());
                            msgs.add(msg);
                            System.out.println("-------msgText--->群名:"+qName+" "+"昵称："+nickName+" 消息："+text);
                        }else{
                            if(!oldSet.contains(nickName+text)){
                                Msg msg = new Msg();
                                msg.setGroupName(qName);
                                msg.setNickName(nickName);
                                msg.setMessage(text);
                                msg.setTime(AutoUtil.getCurrentTime());
                                msgs.add(msg);
                                System.out.println("--------------msgText--->群名:"+qName+" "+"昵称："+nickName+" 消息："+text);
                            }
                        }
                    }
                    LogUtil.d("msg",JSON.toJSONString(msgs));
                    if(msgs.size()>0){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                LogUtil.d("okHttp",OkHttpUtil.okHttpPost(url,JSON.toJSONString(msgs)));
                            }
                        }).start();
                    }
                    temp.put(qName,newSet);
                    AccessibilityNodeInfo backBtn = AutoUtil.findNodeInfosById(root,"com.tencent.mm:id/go");
                    AutoUtil.performClick(backBtn,record,"返回");
                    //AutoUtil.performBack(ChatService.this,record,"全局返回");
                    if(AutoUtil.checkAction(record,"返回")){
                        AutoUtil.recordAndLog(record,Constants.CHAT_LISTENING);
                    }
                    isOver = true;
                    return;
                }
                //---huawei手机--end----
               /* if(receviceMsgs==null||receviceMsgs.size()==0){
                    isOver = true;
                    return;
                }
                for(AccessibilityNodeInfo tt:receviceMsgs){
                    System.out.println("---tt--"+tt.getChildCount());
                    String text="",nickName="";
                     if(tt!=null&&tt.getChildCount()==2){
                         CharSequence child0 = tt.getChild(0).getText();
                         CharSequence child1 = tt.getChild(1).getText();
                         if(child1==null||child0==null) continue;
                         text = tt.getChild(1).getText().toString();
                         nickName = tt.getChild(0).getContentDescription().toString().replace("头像","");
                     }else if(tt!=null&&tt.getChildCount()==3){
                        CharSequence child2 = tt.getChild(2).getText();
                        if(child2==null) continue;
                        text = tt.getChild(2).getText().toString();
                        nickName = tt.getChild(0).getContentDescription().toString().replace("头像","");
                    }else{
                         continue;
                     }
                    //System.out.println("**msgText--->群名:"+qName+" "+"昵称："+nickName+" 消息："+text);
                    newSet.add(nickName+text);
                    if(oldSet==null){
                        Msg msg = new Msg();
                        msg.setGroupName(qName);
                        msg.setNickName(nickName);
                        msg.setMessage(text);
                        msg.setTime(AutoUtil.getCurrentTime());
                        msgs.add(msg);
                        System.out.println("-------msgText--->群名:"+qName+" "+"昵称："+nickName+" 消息："+text);
                    }else{
                        if(!oldSet.contains(nickName+text)){
                            Msg msg = new Msg();
                            msg.setGroupName(qName);
                            msg.setNickName(nickName);
                            msg.setMessage(text);
                            msg.setTime(AutoUtil.getCurrentTime());
                            msgs.add(msg);
                            System.out.println("--------------msgText--->群名:"+qName+" "+"昵称："+nickName+" 消息："+text);
                        }
                    }
                }
                LogUtil.d("msg",JSON.toJSONString(msgs));
                if(msgs.size()>0){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("okHttp",OkHttpUtil.okHttpPost(url,JSON.toJSONString(msgs)));
                        }
                    }).start();
                }
                temp.put(qName,newSet);
                AccessibilityNodeInfo backBtn = AutoUtil.findNodeInfosById(root,"com.tencent.mm:id/go");
                AutoUtil.performClick(backBtn,record,"返回");
                //AutoUtil.performBack(ChatService.this,record,"全局返回");
                AutoUtil.recordAndLog(record,Constants.CHAT_LISTENING);*/
            }
            isOver = true;
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
                AutoUtil.sleep(1000*20);
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
}
