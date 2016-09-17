package org.wzj.im.common;

import java.util.Date;

/**
 * Created by wens on 16/9/17.
 */
public class Friend {

    private Long aUserId;
    private Long bUserId;
    private String aUsername;
    private String bUsername;
    private Integer aStatus;
    private Integer bStatus;
    private Date createTime;

    public Long getaUserId() {
        return aUserId;
    }

    public void setaUserId(Long aUserId) {
        this.aUserId = aUserId;
    }

    public Long getbUserId() {
        return bUserId;
    }

    public void setbUserId(Long bUserId) {
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
