package org.wzj.im.common;

import java.util.Date;

/**
 * Created by wens on 16/9/17.
 */
public class Friend {

    private String aUserId;
    private String bUserId;
    private String aUsername;
    private String bUsername;
    private Integer aStatus;
    private Integer bStatus;
    private Date createTime;

    public String getaUserId() {
        return aUserId;
    }

    public void setaUserId(String aUserId) {
        this.aUserId = aUserId;
    }

    public String getbUserId() {
        return bUserId;
    }

    public void setbUserId(String bUserId) {
        this.bUserId = bUserId;
    }

    public String getaUsername() {
        return aUsername;
    }

    public void setaUsername(String aUsername) {
        this.aUsername = aUsername;
    }

    public String getbUsername() {
        return bUsername;
    }

    public void setbUsername(String bUsername) {
        this.bUsername = bUsername;
    }

    public Integer getaStatus() {
        return aStatus;
    }

    public void setaStatus(Integer aStatus) {
        this.aStatus = aStatus;
    }

    public Integer getbStatus() {
        return bStatus;
    }

    public void setbStatus(Integer bStatus) {
        this.bStatus = bStatus;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
