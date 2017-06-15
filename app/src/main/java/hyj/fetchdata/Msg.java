package hyj.fetchdata;

import org.litepal.crud.DataSupport;

/**
 * Created by Administrator on 2017/6/6.
 */

public class Msg  extends DataSupport {
    private String privateKey;
    private String groupName;
    private String nickName;
    private String message;
    private String time;


    public Msg(String groupName, String nickName, String message, String time) {
        this.groupName = groupName;
        this.nickName = nickName;
        this.message = message;
        this.time = time;
    }

    public Msg() {
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
