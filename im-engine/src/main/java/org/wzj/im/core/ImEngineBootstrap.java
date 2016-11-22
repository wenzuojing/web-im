package org.wzj.im.core;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wzj.im.common.*;
import redis.clients.jedis.JedisPool;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Created by wens on 15-11-9.
 */
public class ImEngineBootstrap {

    private final static Logger log = LoggerFactory.getLogger(ImEngineBootstrap.class);

    public static void main(String[] args) throws SQLException {

        Config appConfig = new Config();
        DBUtils.initDB(appConfig);

        Configuration nettySocketIOConfig = new Configuration();
        AddressAndPort addressAndPort = NetAddressUtils.resolve(appConfig.getBindAddress(), 8080);
        log.info("Server bind on : {} ", addressAndPort);
        nettySocketIOConfig.setHostname(addressAndPort.getAddress());
        nettySocketIOConfig.setPort(addressAndPort.getPort());
        nettySocketIOConfig.setBossThreads(appConfig.getBossThreads());
        nettySocketIOConfig.setWorkerThreads(appConfig.getWorkerThreads());
        nettySocketIOConfig.setMaxFramePayloadLength(appConfig.getMaxPayload());
        //nettySocketIOConfig.setTransports(Transport.POLLING);
        //nettySocketIOConfig.setOrigin("*");

        final JedisPool jedisPool = new JedisPool(appConfig.getRedisHost(),appConfig.getRedisPort());

        final SocketIOServer server = new SocketIOServer(nettySocketIOConfig);
        final BlacklistService blacklistService = new BlacklistService(jedisPool);
        final EventService eventService = new EventService(blacklistService);

        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                eventService.onConnect(client);
            }
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                eventService.onDisconnect(client);
            }
        });

        server.addEventListener("enroll", Enroll.class, new DataListener<Enroll>() {
            @Override
            public void onData(SocketIOClient client, Enroll enroll, AckRequest ackRequest) {
                eventService.onEnroll(client, enroll, ackRequest);
            }
        });

        server.addEventListener("login", Login.class, new DataListener<Login>() {
            @Override
            public void onData(SocketIOClient client, Login login, AckRequest ackRequest) {
                eventService.onLogin(client, login, ackRequest);
            }
        });

        server.addEventListener("queryUser", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String keyword, AckRequest ackRequest) {
                eventService.onFindUser(client, keyword, ackRequest);
            }
        });

        server.addEventListener("addFriend", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String userId, AckRequest ackRequest) {
                eventService.onAddFriend(client, userId, ackRequest);
            }
        });

        server.addEventListener("queryGroup", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String keyword, AckRequest ackRequest) {
                eventService.onFindGroup(client, keyword, ackRequest);
            }
        });


        server.addEventListener("joinGroup", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String groupId, AckRequest ackRequest) {
                eventService.onJoinGroup(client, groupId, ackRequest);
            }
        });

        server.addEventListener("createGroup", CreateGroup.class, new DataListener<CreateGroup>() {
            @Override
            public void onData(SocketIOClient client, CreateGroup createGroup, AckRequest ackRequest) {
                eventService.onCreateGroup(client, createGroup, ackRequest);
            }
        });

        server.addEventListener("queryMyFriend", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
                eventService.onQueryMyFriend(client, ackRequest);
            }
        });

        server.addEventListener("queryMyGroup", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
                eventService.onQueryMyGroup(client, ackRequest);
            }
        });


        server.addEventListener("queryGroupHistoryMessage", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String groupId, AckRequest ackRequest) {
                eventService.onQueryGroupHistoryMessage(client, groupId, ackRequest);
            }
        });

        server.addEventListener("queryGroupHistoryMessage2", HistoryMessageQuery.class, new DataListener<HistoryMessageQuery>() {
            @Override
            public void onData(SocketIOClient client, HistoryMessageQuery query, AckRequest ackRequest) {
                eventService.onQueryGroupHistoryMessage(client, query, ackRequest);
            }
        });

        server.addEventListener("queryGroupMember", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String groupId, AckRequest ackRequest) {
                eventService.onQueryGroupMember(client, String.valueOf(groupId), ackRequest);
            }
        });

        server.addEventListener("sendGroupMsg", GroupMessage.class, new DataListener<GroupMessage>() {
            @Override
            public void onData(SocketIOClient client, GroupMessage groupMessage, AckRequest ackRequest) {
                groupMessage.setContent(Jsoup.clean(groupMessage.getContent() , Whitelist.basicWithImages() ));
                eventService.onSendGroupMsg(client, groupMessage, ackRequest);
            }
        });

        server.addEventListener("sendMsg", ImMessage.class, new DataListener<ImMessage>() {
            @Override
            public void onData(SocketIOClient client, ImMessage imMessage, AckRequest ackRequest) {
                imMessage.setContent(Jsoup.clean(imMessage.getContent() , Whitelist.basicWithImages() ));
                eventService.onSendMsg(client, imMessage, ackRequest);
            }
        });

        server.addEventListener("addGroupBlacklist", GroupBlacklist.class, new DataListener<GroupBlacklist>() {
            @Override
            public void onData(SocketIOClient client, GroupBlacklist groupBlacklist, AckRequest ackRequest) {
                eventService.onAddGroupBlacklist(client, groupBlacklist, ackRequest);
            }
        });

        server.addEventListener("delGroupBlacklist", GroupBlacklist.class, new DataListener<GroupBlacklist>() {
            @Override
            public void onData(SocketIOClient client, GroupBlacklist groupBlacklist, AckRequest ackRequest) {
                eventService.onDelGroupBlacklist(client, groupBlacklist, ackRequest);
            }
        });

        server.addEventListener("heartbeat",String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String timestamp, AckRequest ackRequest) {
                eventService.onHeartbeat(client, timestamp, ackRequest);
            }
        });

        log.info("Server is starting ......");
        eventService.start();
        server.start();
        log.info("Server is started.");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Server is stopping ......");
                server.stop();
                eventService.stop();
                jedisPool.close();
                log.info("Server is stopped.");
            }
        });

        HttpApiController.main(args);
    }

}
