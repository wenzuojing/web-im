package org.wzj.im.common;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.wzj.im.core.Config;
import org.wzj.im.core.HistoryMessageQuery;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Created by wens on 16-9-14.
 */
public class DBUtils {

    private static DruidDataSource dataSource;

    public static void initDB(Config config) throws SQLException {
        dataSource = new DruidDataSource();
        dataSource.setLoginTimeout(10 * 60);
        dataSource.setMaxActive(100);
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl(config.getDbUrl());
        dataSource.setUsername(config.getDbUser());
        dataSource.setPassword(config.getDbPassword());
        dataSource.setInitialSize(20);
    }






    /*public static List<PrivateMessage> queryUnreadPrivateMessages(String userId) {
        return null;
    }

    public static List<GroupMessage> queryUnreadGroupMessages(String userId, String group) {
        return null;
    }

    public static void changeReadFlagForPrivateMessage(String msgId) {

    }

    public static void changeReadFlagForGroupMessage(String userId, String msgId) {

    }*/

    public static void changeUserStatus(String userId, int status) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        runner.update("update im_user set status  = ?  where user_id = ? ", status, userId);
    }


    public static List<GroupMessage> queryGroupHistoryMessage(String groupId) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        BeanListHandler<GroupMessage> resultHandler = new BeanListHandler<>(GroupMessage.class);
        return runner.query("select m.msg_id as msgId ,m.group_id as groupId,m.sender as sender ,m.sender_name as senderName , m.content as content , m.create_time as createTime,m.group_name as groupName  " +
                "from im_group_message m " +
                "where m.group_id = ?  order by m.create_time ", resultHandler, groupId);
    }

    public static List<GroupMessage> queryGroupHistoryMessage(HistoryMessageQuery query) throws SQLException {

        if(query.getLimit() == null ){
            query.setLimit(20);
        }
        if(query.getSince() == null){
            query.setSince(new Date().getTime());
        }

        QueryRunner runner = new QueryRunner(dataSource);
        BeanListHandler<GroupMessage> resultHandler = new BeanListHandler<>(GroupMessage.class);
        return runner.query("select m.msg_id as msgId ,m.group_id as groupId,m.sender as sender ,m.sender_name as senderName , m.content as content , m.create_time as createTime,m.group_name as groupName  " +
                "from im_group_message m " +
                "where m.group_id = ? and m.create_time < ?  order by m.create_time limit ?  ", resultHandler, query.getGroupId() , new Date(query.getSince()) , query.getLimit() );
    }

    public static List<User> queryGroupMembers(String groupId) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        BeanListHandler<User> resultHandler = new BeanListHandler<>(User.class);
        return runner.query("select u.user_id as userId , u.username as username , u.nickname as nickname, u.status as `status`  from im_group_user gu left join im_user u  on gu.user_id  =  u.user_id where gu.group_id = ? ", resultHandler, groupId);

    }


    public static void saveMessage(ImMessage message) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        runner.update("insert into im_message (msg_id,type,msg_type,target,sender,status,content,create_time,target_name,sender_name) values (?,?,?,?,?,?,?,?,?,?)",
                message.getMsgId(), message.getType(), message.getMsgType(), message.getTarget(), message.getSender(), message.getStatus(), message.getContent(),
                message.getCreateTime(),message.getTargetName(),message.getSenderName());
    }

    public static void saveGroupMessage(GroupMessage message) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        runner.update("insert into im_group_message (msg_id,group_id,sender,content,create_time,sender_name,group_name) values (?,?,?,?,?,?,?)",
                message.getMsgId(), message.getGroupId(), message.getSender() , message.getContent(), message.getCreateTime(),message
        .getSenderName(),message.getGroupName());
    }


    public static User queryUserByUsername(String username) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        return runner.query("select u.user_id as userId , u.username as username , u.nickname as nickname, u.status as `status` , u.password as password , u.create_time as createTime , u.heart_time as heartTime from im_user u where u.username = ? ", new BeanHandler<User>(User.class), username);
    }

    public static List<Group> queryJoinGroupBy(String userId) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        BeanListHandler<Group> resultHandler = new BeanListHandler<>(Group.class);
        return runner.query("select g.group_id as groupId , g.group_name as groupName  from im_group_user gu left join im_group g on gu.group_id  = g.group_id where gu.user_id = ? ", resultHandler, userId);
    }

    public static List<User> queryFriend(String userId) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        BeanListHandler<User> resultHandler = new BeanListHandler<>(User.class);
        return runner.query("select u.user_id as userId , u.username as username , u.nickname as nickname, u.status as `status`  from im_friend f left join im_user u on f.a_user_id = u.user_id where f.b_user_id = ? " +
                " union select u.user_id as userId , u.username as username , u.nickname as nickname, u.status as `status`  from im_friend f left join im_user u on f.b_user_id = u.user_id where f.a_user_id = ? ", resultHandler, userId, userId);
    }

    public static User getUser(String userId) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        return runner.query("select u.user_id as userId , u.username as username , u.nickname as nickname, u.status as `status` , u.password as password , u.create_time as createTime , u.heart_time as heartTime from im_user u where u.user_id = ? ", new BeanHandler<User>(User.class), userId);

    }

    public static boolean saveFriend(String myId, String friendId) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        Friend friend = runner.query("select f.a_user_id as aUserId ,f.b_user_id as bUserId , f.a_status as aStatus , f.b_status as bStatus from im_friend f where ( f.a_user_id = ? and f.b_user_id =? ) or ( f.a_user_id = ? and f.b_user_id =? ) ", new BeanHandler<Friend>(Friend.class), myId, friendId, friendId, myId);

        if (friend == null) {
            String sql = "insert into im_friend (a_user_id,b_user_id,a_status,b_status,create_time) values (?,?,?,?,?) ";
            runner.update(sql, myId, friendId, 1, 0, new Date());
            return true;
        } else {
            if (friend.getaUserId().equals(myId)) {
                runner.update("update im_friend set a_status = ?  where a_user_id = ? and b_user_id =? ", 1, myId, friendId);
            } else {
                runner.update("update im_friend set b_status = ?  where a_user_id = ? and b_user_id =? ", 1, friendId, myId);
            }
        }

        return false;
    }

    public static List<User> queryUserByKeywork(String keyword) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        BeanListHandler<User> resultHandler = new BeanListHandler<>(User.class);
        return runner.query("select u.user_id as userId , u.username as username , u.nickname as nickname, u.status as `status`  from  im_user u  where u.username like ? or  u.nickname like ? ", resultHandler, "%" + keyword + "%", "%" + keyword + "%");

    }

    public static List<Group> queryGroupByKeywork(String keyword) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        BeanListHandler<Group> resultHandler = new BeanListHandler<>(Group.class);
        return runner.query("select g.group_id as groupId , g.group_name as groupName  from im_group g where g.group_name like ? ", resultHandler, "%" + keyword + "%");
    }

    public static User saveUser(String userId , String username, String nickname, String password) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        int u = runner.update("insert into im_user (user_id , username,nickname ,password ,status ,create_time ) values (?,?,?,?,?,?)", userId, username, nickname, password,0, new Date());
        return u > 0 ?  getUser(userId) :  null ;
    }

    public static Group saveGroup(String userId, String token, String groupName) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        long groupId  = IdWorker.getId();
        int u = runner.update("insert into im_group (group_id , token ,creater  , group_name ,create_time ) values (?,?,?,?,?)", groupId, token, userId, groupName, new Date());
        saveGroupUser(userId,String.valueOf(groupId));
        return u > 0 ?   getGroup(String.valueOf(groupId)) : null ;
    }

    public static Group getGroupByToken(String token) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        return runner.query("select g.group_id as groupId ,g.token as token , g.group_name as groupName , g.create_time as createTime from im_group g where g.token  = ? " ,new BeanHandler<Group>(Group.class) ,token );
    }

    public static boolean saveGroupUser(String userId, String groupId) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        int u = runner.update("insert into im_group_user (group_id , user_id ,create_time ) values (?,?,?)", groupId, userId , new Date());
        return u > 0;
    }

    public static Group getGroup(String groupId) throws SQLException {
        QueryRunner runner = new QueryRunner(dataSource);
        return runner.query("select g.group_id as groupId ,g.token as token , g.group_name as groupName , g.create_time as createTime from im_group g where g.group_id  = ? " ,new BeanHandler<Group>(Group.class) ,groupId );

    }

    public static List<String> getAllFriendIncludeGroupFriend(String userId) {
        return null;
    }
}
