package org.wzj.im.core;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wzj.im.common.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by wens on 16/9/17.
 */
public class EventService {

    private final static Logger log = LoggerFactory.getLogger(EventService.class);

    private final OnlineUserManager onlineUserManager = new OnlineUserManager();
    private final MessageExchange messageExchange = new MessageExchange();

    public EventService() {
        onlineUserManager.setMessageExchange(messageExchange);
        messageExchange.setOnlineUserManager(onlineUserManager);
    }


    public void onConnect(SocketIOClient client) {
        if (log.isDebugEnabled()) {
            log.debug("connect : sessionId={},remoteAddress = {}", client.getSessionId(), client.getRemoteAddress());
        }
    }

    public void onDisconnect(SocketIOClient client) {
        if (log.isDebugEnabled()) {
            log.debug("disconnect : sessionId={},remoteAddress = {}", client.getSessionId(), client.getRemoteAddress());
        }
        onlineUserManager.unbindClient(client);
    }

    public void onLogin(SocketIOClient client, Login login, AckRequest ackRequest) {
        try {
            User user = DBUtils.queryUserByUsername(login.getUsername());

            if (user == null || !user.getPassword().equals(login.getPassword())) {
                log.warn("This connection is not auth , force disconnect : username = {} , sessionId={},remoteAddress = {} ", login.getUsername(), client.getSessionId(), client.getRemoteAddress());
                client.disconnect();
                return;
            }
            List<Group> myGroups = DBUtils.queryJoinGroupBy(user.getUserId());
            onlineUserManager.bindClient(user, myGroups, client);
            List<User> friends = DBUtils.queryFriend(user.getUserId());
            ackRequest.sendAckData("ok", user, friends, myGroups);
        } catch (Exception e) {
            client.disconnect();
            log.warn(e.getMessage());
            return;
        }
    }

    public void onQueryGroupHistoryMessage(SocketIOClient client, Long groupId, AckRequest ackRequest) {

        List<GroupMessage> groupMessages = null;
        try {
            groupMessages = DBUtils.queryGroupHistoryMessage(groupId);
        } catch (SQLException e) {
            log.error("queryGroupHistoryMessage() fail", e);
            groupMessages = Collections.emptyList();
        }
        ackRequest.sendAckData(groupMessages);
    }

    public void onQueryGroupMember(SocketIOClient client, String groupId, AckRequest ackRequest) {
        List<User> users = null;
        try {
            users = DBUtils.queryGroupMembers(Long.valueOf(groupId));
        } catch (SQLException e) {
            log.error("queryGroupMembers() fail", e);
            users = Collections.EMPTY_LIST;
        }
        ackRequest.sendAckData(users);
    }

    public void onSendGroupMsg(SocketIOClient client, GroupMessage groupMessage, AckRequest ackRequest) {
        try {
            messageExchange.pushGroupMessage(client, groupMessage);
            ackRequest.sendAckData("ok");
        } catch (Exception e) {
            log.error("Handle GroupMessage fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onSendMsg(SocketIOClient client, ImMessage imMessage, AckRequest ackRequest) {
        try {
            messageExchange.pushMessage(client, imMessage);
            ackRequest.sendAckData("ok");
        } catch (Exception e) {
            log.error("Handle GroupMessage fail", e);
            ackRequest.sendAckData("fail");
        }
    }


    public void onAddFriend(SocketIOClient client, Long userId, AckRequest ackRequest) {
        try {
            User friend = DBUtils.getUser(userId);
            if (friend == null) {
                ackRequest.sendAckData("fail");
                return;
            }

            OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);
            if (DBUtils.saveFriend(Long.valueOf(bind.userId), userId)) {
                messageExchange.pushFriendChangeEvent(Arrays.asList(Long.valueOf(bind.userId) ,userId));
            }
            ackRequest.sendAckData("ok");
        } catch (Exception e) {
            log.error("Add friend fail", e);
            ackRequest.sendAckData("fail");
        }

    }

    public void onFindUser(SocketIOClient client, String keyword, AckRequest ackRequest) {
        try {
            List<User> users = DBUtils.queryUserByKeywork(keyword);
            if (users == null) {
                users = Collections.EMPTY_LIST;
            }
            ackRequest.sendAckData("ok", users);
        } catch (Exception e) {
            log.error("Query user fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onFindGroup(SocketIOClient client, String keyword, AckRequest ackRequest) {

        try {
            List<Group> groups = DBUtils.queryGroupByKeywork(keyword);
            if (groups == null) {
                groups = Collections.EMPTY_LIST;
            }
            ackRequest.sendAckData("ok", groups);
        } catch (Exception e) {
            log.error("Query group fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onEnroll(SocketIOClient client, Enroll enroll, AckRequest ackRequest) {

        try {
            if (DBUtils.saveUser(enroll.getUsername(), enroll.getNickname(), enroll.getPassword())) {
                ackRequest.sendAckData("ok");
            } else {
                ackRequest.sendAckData("ok");
            }
        } catch (Exception e) {
            log.error("Enroll fail", e);
            ackRequest.sendAckData("fail");
        }

    }

    public void onJoinGroup(SocketIOClient client, Long groupId, AckRequest ackRequest) {

        try {
            OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);
            Group  group = DBUtils.getGroup(groupId);
            if(group == null ){
                ackRequest.sendAckData("fail");
                return ;
            }
            if( DBUtils.saveGroupUser(Long.valueOf(bind.userId), groupId ) ){

                List<User> users = DBUtils.queryGroupMembers(groupId);
                List<Long> userIds  = new ArrayList<>(users.size());
                for(User u : users ){
                    userIds.add(u.getUserId());
                }
                messageExchange.pushFriendChangeEvent(userIds);
            }
        } catch (Exception e) {
            log.error("Create group fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onCreateGroup(SocketIOClient client, CreateGroup createGroup, AckRequest ackRequest) {

        try {
            OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);
            if (DBUtils.saveGroup(Long.valueOf(bind.userId), createGroup.getToken(), createGroup.getGroupName())) {
                ackRequest.sendAckData("ok");
            } else {
                ackRequest.sendAckData("ok");
            }
        } catch (Exception e) {
            log.error("Create group fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onQueryMyFriend(SocketIOClient client, AckRequest ackRequest) {

        try {
            OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);
            List<User> users = DBUtils.queryFriend(Long.valueOf(bind.userId));
            ackRequest.sendAckData("ok", users);
        } catch (Exception e) {
            log.error("Query my friend fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onQueryMyGroup(SocketIOClient client, AckRequest ackRequest) {

        try {
            OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);
            List<Group> groups = DBUtils.queryJoinGroupBy(Long.valueOf(bind.userId));
            ackRequest.sendAckData("ok", groups);
        } catch (Exception e) {
            log.error("Query my group fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void start() {


    }

    public void stop() {


    }



}
