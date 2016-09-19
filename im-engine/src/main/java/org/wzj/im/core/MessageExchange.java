package org.wzj.im.core;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.corundumstudio.socketio.SocketIOClient;
import org.jgroups.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wzj.im.common.*;

import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by wens on 15-11-10.
 */
public class MessageExchange extends ReceiverAdapter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private JChannel channel;

    private OnlineUserManager onlineUserManager;

    public void setOnlineUserManager(OnlineUserManager onlineUserManager) {
        this.onlineUserManager = onlineUserManager;
    }

    public void start() {
        URL xml = MessageExchange.class.getClassLoader().getResource("jgroups-config.xml");

        try {
            channel = new JChannel(xml);
            channel.setReceiver(this);
            channel.connect("push");
        } catch (Exception e) {
            throw new PushException(e);
        }
    }

    public void stop() {
        channel.disconnect();
    }

    @Override
    public void receive(Message msg) {

        byte[] buffers = msg.getRawBuffer();

        if (buffers.length < 1) {
            log.warn("ImMessage is empty.");
            return;
        }

        //不处理发送给自己的消息
        if (msg.getSrc().equals(channel.getAddress()))
            return;

        ByteArrayInputStream input = new ByteArrayInputStream(buffers);
        DataInputStream inputStream = new DataInputStream(input);
        try {

            int cmdLen = inputStream.readInt();

            byte[] cmdBytes = new byte[cmdLen];
            inputStream.readFully(cmdBytes);

            int paramNum = inputStream.readInt();

            String[] params = new String[paramNum];

            for (int i = 0; i < paramNum; i++) {
                int plen = inputStream.readInt();
                byte[] pBytes = new byte[plen];
                inputStream.readFully(pBytes);
                params[i] = new String(pBytes, "utf-8");
            }
            handleReceiver(cmdBytes, params);
        } catch (Exception e) {
            log.error("Unable to handle received msg", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    //ignore
                }
            }

            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    private void handleReceiver(byte[] cmdBytes, String[] params) throws UnsupportedEncodingException {
        String cmd = new String(cmdBytes, "utf-8");
        if (Constant.PUSH_MESSAGE.equals(cmd)) {
            if (params.length == 1) {
                try {
                    ImMessage imMessage = JSONObject.parseObject(params[0], ImMessage.class);
                    this.onlineUserManager.pushMessage(imMessage);
                } catch (Exception e) {
                    log.error("Fail to push private message.", e);
                }
            } else {
                log.warn("Receive a bad message : {}", params[0]);
            }
        } else if (Constant.PUSH_GROUP_MESSAGE.equals(cmd)) {
            if (params.length == 1) {
                try {
                    GroupMessage groupMessage = JSONObject.parseObject(params[0], GroupMessage.class);
                    this.onlineUserManager.pushGroupMessage(groupMessage);
                } catch (Exception e) {
                    log.error("Fail to push group message.", e);
                }
            } else {
                log.warn("Receive a bad message : {}", params[0]);
            }
        } else if (Constant.PUSH_FRIEND_ONLINE_EVENT.equals(cmd) || Constant.PUSH_FRIEND_OFFLINE_EVENT.equals(cmd) ) {
            if (params.length == 2) {
                try {
                    String who  = String.valueOf(params[0]);
                    List<String> friendIds = JSONObject.parseObject(params[1] , new TypeReference<List<String>>(){}) ;
                    this.onlineUserManager.pushOnlineOfflineEvent(who,friendIds,Constant.PUSH_FRIEND_ONLINE_EVENT.equals(cmd) );
                } catch (Exception e) {
                    log.error("Fail to push PUSH_FRIEND_ONLINE_EVENT or PUSH_FRIEND_OFFLINE_EVENT message.", e);
                }
            } else {
                log.warn("Receive a bad message : {}", params[0]);
            }
        } else if (Constant.PUSH_GROUP_MEMBER_ONLINE_EVENT.equals(cmd) || Constant.PUSH_GROUP_MEMBER_OFFLINE_EVENT.equals(cmd) ) {
            if (params.length == 1) {
                try {
                    String[] groups = JSONObject.parseObject(params[0] , new TypeReference<String[]>(){}) ;
                    this.onlineUserManager.pushGroupMemberOnlineOfflineEvent(groups,Constant.PUSH_GROUP_MEMBER_ONLINE_EVENT.equals(cmd) );
                } catch (Exception e) {
                    log.error("Fail to push PUSH_GROUP_MEMBER_ONLINE_EVENT or PUSH_GROUP_MEMBER_OFFLINE_EVENT message.", e);
                }
            } else {
                log.warn("Receive a bad message : {}", params[0]);
            }
        }else if(Constant.PUSH_FRIEND_CHANGE_EVENT.equals(cmd)){
            if (params.length == 1) {
                try {
                    List<String> userIds = JSONObject.parseObject(params[0] , new TypeReference<List<String>>(){}) ;
                    this.onlineUserManager.pushFriendChangeEvent(userIds);
                } catch (Exception e) {
                    log.error("Fail to push PUSH_FRIEND_CHANGE_EVENT message.", e);
                }
            } else {
                log.warn("Receive a bad message : {}", params[0]);
            }
        }else if(Constant.PUSH_GROUP_MEMBER_CHANGE_EVENT.equals(cmd)){
            if (params.length == 1) {
                try {
                    this.onlineUserManager.pushGroupMemberChangeEvent(params[0]);
                } catch (Exception e) {
                    log.error("Fail to push PUSH_GROUP_MEMBER_CHANGE_EVENT message.", e);
                }
            } else {
                log.warn("Receive a bad message : {}", params[0]);
            }
        }
    }

    @Override
    public void viewAccepted(View view) {
        StringBuffer sb = new StringBuffer("Group Members Changed, LIST: ");
        List<Address> addrs = view.getMembers();
        for (int i = 0; i < addrs.size(); i++) {
            if (i > 0)
                sb.append(',');
            sb.append(addrs.get(i).toString());
        }
        log.info(sb.toString());
    }


    public void pushGroupMessage(SocketIOClient client, GroupMessage groupMessage) {

        OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);

        if (bind == null ||groupMessage.getMsgType() == null ||groupMessage.getGroupId() == null || StringUtils.isEmpty(groupMessage.getContent())) {
            throw new PushException("Illegal args");
        }

        groupMessage.setStatus(0);
        groupMessage.setCreateTime(Calendar.getInstance().getTime());
        groupMessage.setMsgId(String.valueOf(IdWorker.getId()));
        groupMessage.setSender(bind.userId);

        try {
            DBUtils.saveGroupMessage(groupMessage);
        } catch (SQLException e) {
            log.error("saveGroupMessage() fail", e);
        }

        try {
            groupMessage.setSenderName(DBUtils.getUser(groupMessage.getSender()).getNickname());
            groupMessage.setGroupName(DBUtils.getGroup(groupMessage.getGroupId()).getGroupName());
        } catch (SQLException e) {
            throw new PushException(e);
        }

        onlineUserManager.pushGroupMessage(groupMessage);

        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        DataOutputStream writer = new DataOutputStream(out);
        try {
            writeString(writer, Constant.PUSH_GROUP_MESSAGE);
            writer.writeInt(1);
            String json = JSONObject.toJSONString(groupMessage);
            writeString(writer, json);

            Message message = new Message();
            writer.flush();
            message.setBuffer(out.toByteArray());
            channel.send(message);
        } catch (Exception e) {
            log.error("send broadcast fail", e);
            throw new PushException(e);
        }finally {
            try {
                writer.close();
            } catch (IOException e) {
                //
            }
            try {
                out.close();
            } catch (IOException e) {
                //
            }
        }

    }

    public void pushMessage(SocketIOClient client, ImMessage imMessage) {

        OnlineUserManager.Bind bind = client.get(Constant.BIND_KEY);

        if (bind == null || imMessage.getMsgType() == null || imMessage.getTarget() == null || StringUtils.isEmpty(imMessage.getContent())) {
            throw new PushException("Illegal args");
        }

        imMessage.setType(1);
        imMessage.setStatus(0);

        imMessage.setSender(bind.userId);
        imMessage.setCreateTime(Calendar.getInstance().getTime());
        imMessage.setMsgId(String.valueOf(IdWorker.getId()));

        try {
            DBUtils.saveMessage(imMessage);
        } catch (SQLException e) {
            log.error("saveMessage() fail", e);
        }

        try {
            imMessage.setTargetName(DBUtils.getUser(imMessage.getTarget()).getNickname());
            imMessage.setSenderName(DBUtils.getUser(imMessage.getSender()).getNickname());
        } catch (SQLException e) {
            throw new PushException(e);
        }

        if (!onlineUserManager.pushMessage(imMessage)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            DataOutputStream writer = new DataOutputStream(out);
            try {
                writeString(writer, Constant.PUSH_MESSAGE);
                writer.writeInt(1);
                String json = JSONObject.toJSONString(imMessage);
                writeString(writer, json);
                Message message = new Message();
                writer.flush();
                message.setBuffer(out.toByteArray());
                channel.send(message);
            } catch (Exception e) {
                log.error("send broadcast fail", e);
                throw new PushException(e);
            }finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    //
                }
                try {
                    out.close();
                } catch (IOException e) {
                    //
                }
            }
        }

    }

    public static void writeString(DataOutputStream writer, String str) throws IOException {
        byte[] bytes = str.getBytes("utf-8");
        writer.writeInt(bytes.length);
        writer.write(bytes);
    }

    public void pushOnlineOfflineEvent(String userId , List<String> friendIds , boolean isOnline ) {

        onlineUserManager.pushOnlineOfflineEvent( userId , friendIds ,isOnline ) ;
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        DataOutputStream writer = new DataOutputStream(out);

        try {
            writeString(writer, isOnline ? Constant.PUSH_FRIEND_ONLINE_EVENT : Constant.PUSH_FRIEND_OFFLINE_EVENT );
            writer.writeInt(2);
            writeString(writer, userId );
            writeString(writer, JSONObject.toJSONString(friendIds));
            Message message = new Message();
            writer.flush();
            message.setBuffer(out.toByteArray());
            channel.send(message);
        } catch (Exception e) {
            log.error("send broadcast fail", e);
            throw new PushException(e);
        }finally {
            try {
                writer.close();
            } catch (IOException e) {
                //
            }
            try {
                out.close();
            } catch (IOException e) {
                //
            }
        }

    }

    public void pushGroupMemberOnlineOfflineEvent(String[] groups , boolean isOnline ) {
        onlineUserManager.pushGroupMemberOnlineOfflineEvent( groups ,isOnline ) ;
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        DataOutputStream writer = new DataOutputStream(out);
        try {
            writeString(writer, isOnline ? Constant.PUSH_GROUP_MEMBER_ONLINE_EVENT : Constant.PUSH_GROUP_MEMBER_OFFLINE_EVENT );
            writer.writeInt(1);
            writeString(writer, JSONObject.toJSONString(groups));
            Message message = new Message();
            writer.flush();
            message.setBuffer(out.toByteArray());
            channel.send(message);
        } catch (Exception e) {
            log.error("send broadcast fail", e);
            throw new PushException(e);
        }finally {
            try {
                writer.close();
            } catch (IOException e) {
                //
            }
            try {
                out.close();
            } catch (IOException e) {
                //
            }
        }

    }


    public void pushFriendChangeEvent(List<String> userIds ) {

        onlineUserManager.pushFriendChangeEvent(userIds) ;

        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        DataOutputStream writer = new DataOutputStream(out);
        try {
            writeString(writer,Constant.PUSH_FRIEND_CHANGE_EVENT);
            writer.writeInt(1);

            writeString(writer, JSONObject.toJSONString(userIds));
            Message message = new Message();
            writer.flush();
            message.setBuffer(out.toByteArray());
            channel.send(message);
        } catch (Exception e) {
            log.error("send broadcast fail", e);
            throw new PushException(e);
        }finally {
            try {
                writer.close();
            } catch (IOException e) {
                //
            }
            try {
                out.close();
            } catch (IOException e) {
                //
            }
        }


    }

    public void pushGroupMemberChangeEvent(String groupId) {
        onlineUserManager.pushGroupMemberChangeEvent(groupId) ;

        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        DataOutputStream writer = new DataOutputStream(out);
        try {
            writeString(writer,Constant.PUSH_GROUP_MEMBER_CHANGE_EVENT);
            writer.writeInt(1);
            writeString(writer, groupId);
            Message message = new Message();
            writer.flush();
            message.setBuffer(out.toByteArray());
            channel.send(message);
        } catch (Exception e) {
            log.error("send broadcast fail", e);
            throw new PushException(e);
        }finally {
            try {
                writer.close();
            } catch (IOException e) {
                //
            }
            try {
                out.close();
            } catch (IOException e) {
                //
            }
        }
    }
}
