package org.wzj.im.core;

import java.util.Arrays;

/**
 * Created by wens on 15-11-9.
 */
public class Join {

    private String userId;

    private String username;

    private String token;

    private String[] groups;

    private boolean ignoreOfflineMsg;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String[] getGroups() {
        return groups;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    public boolean isIgnoreOfflineMsg() {
        return ignoreOfflineMsg;
    }

    public void setIgnoreOfflineMsg(boolean ignoreOfflineMsg) {
        this.ignoreOfflineMsg = ignoreOfflineMsg;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "Join{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", token='" + token + '\'' +
                ", groups=" + Arrays.toString(groups) +
                ", ignoreOfflineMsg=" + ignoreOfflineMsg +
                '}';
    }
}
