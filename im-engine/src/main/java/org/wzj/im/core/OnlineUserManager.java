package org.wzj.im.core;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.VoidAckCallback;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wzj.im.common.*;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by wens on 15-11-9.
 */
public class OnlineUserManager implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());


    private DelayQueue<UserOffline> userOfflineDelayQueue = new DelayQueue<UserOffline>() ;


    public static class Bind {
        String userId;
        Set<String> groups;
        public Bind(String userId, Set<String> groups) {
            this.userId = userId;
            this.groups = groups;
        }
    }

    public static class UserOffline implements Delayed{

        String userId ;
        long offlineTime ;

        public UserOffline(String userId, long offlineTime) {
            this.userId = userId;
            this.offlineTime = offlineTime;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return offlineTime - System.currentTimeMillis() ;
        }

        @Override
        public int compareTo(Delayed o) {
            UserOffline uo = (UserOffline) o ;
            return (int)( offlineTime - uo.offlineTime) ;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserOffline that = (UserOffline) o;

            return userId != null ? userId.equals(that.userId) : that.userId == null;

        }

        @Override
        public int hashCode() {
            return userId != null ? userId.hashCode() : 0;
        }
    }

    @Override
    public void run() {


        while (true){

            try {
                UserOffline userOffline = userOfflineDelayQueue.take();
                Collection<SocketIOClient> socketIOClients = userId2Clients.get(userOffline.userId);
                if (socketIOClients == null || socketIOClients.size() == 0) {

                    try {
                        DBUtils.changeUserStatus(userOffline.userId, 0);
                    } catch (SQLException e) {
                        throw new PushException(e);
                    }
                    List<User> friends = null;
                    try {
                        friends = DBUtils.queryFriend(userOffline.userId);
                    } catch (SQLException e) {
                        throw new PushException(e) ;
                    }
                    List<String> friendIds  =  new ArrayList<>(friends.size());
                    for(User u : friends){
                        friendIds.add(u.getUserId());
                    }
                    messageExchange.pushOnlineOfflineEvent(userOffline.userId ,friendIds ,false );
                    try {
                        List<Group> groups = DBUtils.queryJoinGroupBy(userOffline.userId);
                        Set<String> groupIds = new HashSet<>();
                        if (groups != null) {
                            for (Group g : groups) {
                                groupIds.add(g.getGroupId());
                            }
                        }
                        messageExchange.pushGroupMemberOnlineOfflineEvent(groupIds.toArray(new String[groupIds.size()]),false);
                    } catch (SQLException e) {
                        log.error("pushGroupMemberOnlineOfflineEvent fail" , e );
                    }

                }
            } catch (Exception e) {
                log.error("Handle user offline fail" ,e);
            }

        }

    }

    public OnlineUserManager(){
        new Thread(this,"offline-thread").start();
    }

    private final Multimap<String, SocketIOClient> userId2Clients = HashMultimap.create();
    private final Multimap<String, SocketIOClient> group2Clients = HashMultimap.create();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private MessageExchange messageExchange;

    public void setMessageExchange(MessageExchange messageExchange) {
        this.messageExchange = messageExchange;
    }

    public void bindClient(User user, List<Group> groups, SocketIOClient client) {

        lock.writeLock().lock();
        try {
            Bind bind = client.get(Constant.BIND_KEY);

            if (bind != null) {
                throw new PushException("Session:" + client.getSessionId() + " had be bind user:" + bind.userId);
            }

            userId2Clients.put(user.getUserId(), client);

            Set<String> groupIds = new HashSet<>();
            if (groups != null) {
                for (Group g : groups) {
                    groupIds.add(g.getGroupId());
                }
            }
            for (String g : groupIds) {
                group2Clients.put(g, client);
            }

            client.set(Constant.BIND_KEY, new Bind(user.getUserId(), groupIds));

            //The same user open two connects or more
            if (userId2Clients.get(user.getUserId()).size() == 1) {
                try {
                    DBUtils.changeUserStatus(user.getUserId(), 1);
                } catch (Exception e) {
                    throw new PushException(e);
                }
            }

        } finally {
            lock.writeLock().unlock();
        }

    }

    public void updateBindClient(User user, List<Group> groups, SocketIOClient client) {

        lock.writeLock().lock();
        try {

            userId2Clients.remove(user.getUserId(), client);
            userId2Clients.put(user.getUserId(), client);

            Set<String> groupIds = new HashSet<>();
            if (groups != null) {
                for (Group g : groups) {
                    groupIds.add(g.getGroupId());
                }
            }
            for (String g : groupIds) {
                group2Clients.remove(g, client);
                group2Clients.put(g, client);
            }

            client.set(Constant.BIND_KEY, new Bind(user.getUserId(), groupIds));

        } finally {
            lock.writeLock().unlock();
        }

    }


    public void unbindClient(SocketIOClient client) {

        lock.writeLock().lock();
        try {

            Bind bind = client.get(Constant.BIND_KEY);
            if (bind == null) {
                return;
            }

            userId2Clients.remove(bind.userId, client);
            for (String g : bind.groups) {
                group2Clients.remove(g, client);
            }



            Collection<SocketIOClient> socketIOClients = userId2Clients.get(bind.userId);
            if (socketIOClients == null || socketIOClients.size() == 0) {
                userOfflineDelayQueue.put(new UserOffline(bind.userId , System.currentTimeMillis() + ( 30 * 1000 )  ));
            }

        } finally {
            lock.writeLock().unlock();
        }

    }

    public boolean pushMessage(final ImMessage imMessage) {
        Collection<SocketIOClient> socketIOClients;
        lock.readLock().lock();
        try {
            socketIOClients = userId2Clients.get(imMessage.getTarget());
        } finally {
            lock.readLock().unlock();
        }


        if (socketIOClients == null || socketIOClients.size() == 0) {
            log.warn("Can not find client for this user : {}", imMessage.getTarget());
            return false;
        }

        for (SocketIOClient client : socketIOClients) {
            client.sendEvent("messageEvent", new VoidAckCallback() {
                @Override
                protected void onSuccess() {
                    log.info("Ack message . userId = {} , msgId = {} ", imMessage.getTarget(), imMessage.getMsgId());
                }
            }, imMessage);
            log.info("Push private offline message . {} ", imMessage);
        }
        return true;
    }

    public boolean pushGroupMessage(final GroupMessage groupMessage) {

        Collection<SocketIOClient> socketIOClients;
        lock.readLock().lock();
        try {
            socketIOClients = group2Clients.get(groupMessage.getGroupId());
        } finally {
            lock.readLock().unlock();
        }

        if (socketIOClients == null || socketIOClients.size() == 0) {
            log.warn("Can not find client for this group: {}", groupMessage.getGroupId());
            return false;
        }

        for (SocketIOClient client : socketIOClients) {
            Bind bind = client.get(Constant.BIND_KEY);
            final String userId = bind.userId;
            if (bind.userId.equals(groupMessage.getSender())) {
                continue;
            }
            client.sendEvent("groupMessageEvent", new VoidAckCallback() {
                @Override
                protected void onSuccess() {
                    log.info("Ack message . userId = {} , msgId = {} ", userId, groupMessage.getMsgId());
                }
            }, groupMessage);
            log.info("Push group' message. userId = {} , {} ", userId, groupMessage);
        }

        return true;
    }

    public void pushOnlineOfflineEvent(String who , List<String> friendIds , boolean isOnlineEvent   ) {

        Collection<SocketIOClient> socketIOClients;

        for(String userId  : friendIds ){

            lock.readLock().lock();
            try {
                socketIOClients = userId2Clients.get(userId) ;
            } finally {
                lock.readLock().unlock();
            }

            if (socketIOClients == null || socketIOClients.size() == 0) {
                log.warn("Can not find client for the user : {}", userId );
                continue;
            }

            for (SocketIOClient client : socketIOClients) {
                client.sendEvent( isOnlineEvent ? "onlineEvent" : "offlineEvent", who );
                log.info("Push onlineEvent or offlineEvent for the  user : {}  ", userId );
            }
        }

    }

    public void pushGroupMemberOnlineOfflineEvent(String[] groups, boolean isOnlineEvent ) {

        Collection<SocketIOClient> socketIOClients;

        for(String group  : groups ){
            lock.readLock().lock();
            try {
                socketIOClients = group2Clients.get(group) ;
            } finally {
                lock.readLock().unlock();
            }

            if (socketIOClients == null || socketIOClients.size() == 0) {
                log.warn("Can not find client for the group : {}", group );
                continue;
            }

            for (SocketIOClient client : socketIOClients) {
                client.sendEvent( isOnlineEvent ? "groupMemberOnlineEvent" : "groupMemberOfflineEvent", group );
                log.info("Push groupMemberOnlineEvent or groupMemberOfflineEvent for the  user : {}  ", group );
            }
        }
    }

    public void pushFriendChangeEvent(List<String> userIds) {

        Collection<SocketIOClient> socketIOClients;

        for(String userId  : userIds ){
            lock.readLock().lock();
            try {
                socketIOClients = userId2Clients.get(userId) ;
            } finally {
                lock.readLock().unlock();
            }

            if (socketIOClients == null || socketIOClients.size() == 0) {
                log.warn("Can not find client for the user : {}", userId );
                continue;
            }

            for (SocketIOClient client : socketIOClients) {
                client.sendEvent("friendChangeEvent");
                log.info("Push onlineEvent or offlineEvent for the  user : {}  ", userId );
            }
        }
    }

    public void pushGroupMemberChangeEvent(String groupId) {

        Collection<SocketIOClient> socketIOClients;

        lock.readLock().lock();
        try {
            socketIOClients = group2Clients.get(groupId) ;
        } finally {
            lock.readLock().unlock();
        }

        if (socketIOClients == null || socketIOClients.size() == 0) {
            log.warn("Can not find client for the group : {}", groupId );
        }

        for (SocketIOClient client : socketIOClients) {
            client.sendEvent("groupMemberChangeEvent" , groupId );
            log.info("Push onlineEvent or offlineEvent for the  group : {}  ", groupId );
        }
    }



}
