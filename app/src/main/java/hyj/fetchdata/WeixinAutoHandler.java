package hyj.fetchdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/5/16.
 */

public class WeixinAutoHandler {
    public Map<String, String> controller = new HashMap<String, String>();
    private static WeixinAutoHandler weixinAutoHandler = new WeixinAutoHandler();
    private WeixinAutoHandler(){}
    public static WeixinAutoHandler getInstance(){
        return weixinAutoHandler;
    }

    public static boolean IS_AUTO_ADDFR=false;
    //暂停
    public static boolean IS_PAUSE=false;
    public static boolean IS_START_SERVICE=false;

    public static  List<List<Msg>> sendMsgs = new ArrayList<List<Msg>>();

}
