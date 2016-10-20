package org.wzj.im.core;


/**
 * Created by wens on 16/4/19.
 */
public class ReturnResult {

    private boolean success;
    private String desc;
    private Object data;

    public ReturnResult(boolean success, String desc) {
        this(success, desc, null);
    }

    public ReturnResult(boolean success, String desc, Object data) {
        this.success = success;
        this.desc = desc;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public static ReturnResult success(String desc) {
        return new ReturnResult(true, desc);
    }

    public static <T> ReturnResult success(T data) {
        return new ReturnResult(true, "处理成功", data);
    }

    public static ReturnResult fail(String desc) {
        return new ReturnResult(false, desc);
    }


}
