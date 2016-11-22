package org.wzj.im.core;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wzj.im.common.*;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by wens on 16/9/17.
 */
public class EventService {

    private final static Logger log = LoggerFactory.getLogger(EventService.class);

    private final OnlineUserManager onlineUserManager = new OnlineUserManager();
    private final MessageExchange messageExchange = new MessageExchange();
    private BlacklistService blacklistService ;

    public EventService(BlacklistService blacklistService) {
        onlineUserManager.setMessageExchange(messageExchange);
        messageExchange.setOnlineUserManager(onlineUserManager);
        this.blacklistService = blacklistService ;
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
            List<String> friendIds  =  new ArrayList<>(friends.size());
            for(User u : friends){
                friendIds.add(u.getUserId());
            }
            List<String> groupIds  = new ArrayList<>(myGroups.size()) ;
            for(Group g : myGroups ){
                groupIds.add(g.getGroupId()) ;
            }
            messageExchange.pushOnlineOfflineEvent(  user.getUserId() ,friendIds ,true );
            messageExchange.pushGroupMemberOnlineOfflineEvent(groupIds.toArray(new String[groupIds.size()]), true );
            ackRequest.sendAckData("ok", user, friends, myGroups);
        } catch (Exception e) {
            client.disconnect();
            log.warn(e.getMessage());
            return;
        }
    }

    public void onQueryGroupHistoryMessage(SocketIOClient client, String groupId, AckRequest ackRequest) {

        List<GroupMessage> groupMessages = null;
        try {
            groupMessages = DBUtils.queryGroupHistoryMessage(groupId);
        } catch (SQLException e) {
            log.error("queryGroupHistoryMessage() fail", e);
            groupMessages = Collections.emptyList();
        }
        ackRequest.sendAckData("ok" ,groupMessages);
    }

    public void onQueryGroupHistoryMessage(SocketIOClient client, HistoryMessageQuery query, AckRequest ackRequest) {

        List<GroupMessage> groupMessages = null;
        try {
            groupMessages = DBUtils.queryGroupHistoryMessage(query);
        } catch (SQLException e) {
            log.error("queryGroupHistoryMessage() fail", e);
            groupMessages = Collections.emptyList();
        }
        ackRequest.sendAckData("ok" ,groupMessages);
    }

    public void onQueryGroupMember(SocketIOClient client, String groupId, AckRequest ackRequest) {
        List<GroupUser> users = null;
        try {
            List<User> userList = DBUtils.queryGroupMembers(groupId);
            Map<String, Boolean> groupBlackListMap = blacklistService.getGroupBlackList(groupId);
            users = new ArrayList<>(userList.size());
            for(User user : userList ){
                GroupUser groupUser = new GroupUser(user);
                groupUser.setInGropuBlackList(groupBlackListMap.containsKey(user.getUserId()));
                users.add(groupUser);
            }
        } catch (SQLException e) {
            log.error("queryGroupMembers() fail", e);
            users = Collections.EMPTY_LIST;
        }
        ackRequest.sendAckData("ok",users);
    }

    public void onSendGroupMsg(SocketIOClient client, GroupMessage groupMessage, AckRequest ackRequest) {
        try {

            if(blacklistService.inGroupBlackList(groupMessage.getGroupId(),groupMessage.getSender())){
                ackRequest.sendAckData("fail","你被管理员禁言了");
                return ;
            }

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


    public void onAddFriend(SocketIOClient client, String userId, AckRequest ackRequest) {
        try {
            User friend = DBUtils.getUser(userId);
            if (friend == null) {
                ackRequest.sendAckData("fail");
                return;
            }

            OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);
            if (DBUtils.saveFriend(bind.userId, userId)) {
                messageExchange.pushFriendChangeEvent(Arrays.asList(bind.userId ,userId));
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
            OnlineUserManager.Bind bind  = client.get(Constant.BIND_KEY) ;
            if (users == null) {
                users = Collections.EMPTY_LIST;
            }else if(bind != null ){
                List<User> friends  = DBUtils.queryFriend(bind.userId) ;
                List<User> list  = new ArrayList<>(users.size()) ;
                for(User u : users ){
                    boolean f =  false ;
                    for(User friend : friends){
                        if(friend.getUserId().equals(u.getUserId())){
                           f = true ;
                        }
                    }

                    if( !f  && !u.getUserId().equals(bind.userId)){
                        list.add(u);
                    }

                }

                users = list ;
            }



            ackRequest.sendAckData("ok", users);
        } catch (Exception e) {
            log.error("Query user fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onFindGroup(SocketIOClient client, String keyword, AckRequest ackRequest) {

        try {
            OnlineUserManager.Bind bind  = client.get(Constant.BIND_KEY) ;
            List<Group> groups = DBUtils.queryGroupByKeywork(keyword);
            if (groups == null) {
                groups = Collections.EMPTY_LIST;
            }else if(bind != null ){
                List<Group> myGroups = DBUtils.queryJoinGroupBy(bind.userId);
                List<Group> list = new ArrayList<>(groups.size() );

                for(Group g  : groups ){
                    boolean f  = false ;
                    for(Group gg : myGroups ){
                        if(gg.getGroupId().equals(g.getGroupId())){
                            f = true ;
                            break;
                        }
                    }
                    if(!f ){
                        list.add(g) ;
                    }
                }
                groups = list ;
            }
            ackRequest.sendAckData("ok", groups);
        } catch (Exception e) {
            log.error("Query group fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onEnroll(SocketIOClient client, Enroll enroll, AckRequest ackRequest) {

        try {
            User user  = DBUtils.saveUser(String.valueOf( IdWorker.getId()), enroll.getUsername(), enroll.getNickname(), enroll.getPassword() ) ;
            if ( user != null ) {
                ackRequest.sendAckData("ok" , user );
            } else {
                ackRequest.sendAckData("fail");
            }
        } catch (Exception e) {
            log.error("Enroll fail", e);
            ackRequest.sendAckData("fail");
        }

    }

    public void onJoinGroup(SocketIOClient client, String groupId, AckRequest ackRequest) {

        try {
            OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);
            Group  group = DBUtils.getGroup(groupId);
            if(group == null ){
                ackRequest.sendAckData("fail");
                return ;
            }
            if( DBUtils.saveGroupUser(bind.userId, groupId ) ){
                messageExchange.pushGroupMemberChangeEvent(groupId);
                List<Group> myGroups = DBUtils.queryJoinGroupBy(bind.userId);
                onlineUserManager.updateBindClient(DBUtils.getUser(bind.userId), myGroups, client);
            }
            ackRequest.sendAckData("ok");
        } catch (Exception e) {
            log.error("Create group fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onCreateGroup(SocketIOClient client, CreateGroup createGroup, AckRequest ackRequest) {
        try {
            OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);
            Group group  = DBUtils.saveGroup(bind.userId, createGroup.getToken(), createGroup.getGroupName());
            if (group != null ) {
                ackRequest.sendAckData("ok" , group );
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
            List<User> users = DBUtils.queryFriend(bind.userId);
            ackRequest.sendAckData("ok", users);
        } catch (Exception e) {
            log.error("Query my friend fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onQueryMyGroup(SocketIOClient client, AckRequest ackRequest) {

        try {
            OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);
            List<Group> groups = DBUtils.queryJoinGroupBy(bind.userId);
            ackRequest.sendAckData("ok", groups);
        } catch (Exception e) {
            log.error("Query my group fail", e);
            ackRequest.sendAckData("fail");
        }
    }

    public void onAddGroupBlacklist(SocketIOClient client, GroupBlacklist groupBlacklist, AckRequest ackRequest) {

        try {
            blacklistService.addGroupBlackList(groupBlacklist.getGroupId(),groupBlacklist.getUserId());
            ackRequest.sendAckData("ok");
        } catch (Exception e) {
            log.error("Add group black list fail", e);
            ackRequest.sendAckData("fail");
        }

    }

    public void onDelGroupBlacklist(SocketIOClient client, GroupBlacklist groupBlacklist, AckRequest ackRequest) {

        try {
            blacklistService.delGroupBlackList(groupBlacklist.getGroupId(),groupBlacklist.getUserId());
            ackRequest.sendAckData("ok");
        } catch (Exception e) {
            log.error("Del group black list fail", e);
            ackRequest.sendAckData("fail");
        }

    }

    public void start() {
        messageExchange.start();
    }

    public void stop() {
        messageExchange.stop();
    }

    public void onHeartbeat(SocketIOClient client, String timestamp, AckRequest ackRequest) {
        try {
            ackRequest.sendAckData("ok" , System.currentTimeMillis() );
        } catch (Exception e) {
            log.error("Send heartbeat fail", e);
            ackRequest.sendAckData("fail");
        }
    }
}
