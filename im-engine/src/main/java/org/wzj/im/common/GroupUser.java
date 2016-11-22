package org.wzj.im.common;


import org.springframework.beans.BeanUtils;

/**
 * Created by wens on 16-11-18.
 */
public class GroupUser extends User {

    private Boolean inGropuBlackList ;

    public GroupUser(User user) {
        this.setCreateTime(user.getCreateTime());
        this.setHeartTime(user.getHeartTime());
        this.setNickname(user.getNickname());
        this.setPassword(user.getPassword());
        this.setStatus(user.getStatus());
        this.setUserId(user.getUserId());
        this.setUsername(user.getUsername());
    }

    public Boolean getInGropuBlackList() {
        return inGropuBlackList;
    }

    public void setInGropuBlackList(Boolean inGropuBlackList) {
        this.inGropuBlackList = inGropuBlackList;
    }
}
