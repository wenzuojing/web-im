package org.wzj.im.core.test;

import junit.framework.Assert;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wzj.im.common.*;
import org.wzj.im.core.Config;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Created by wens on 16/9/15.
 */
public class DBUtilsTest {


    @BeforeClass
    public static void init() throws SQLException {
        DBUtils.initDB(new Config());
    }


    @Test
    public void test_saveUser() throws SQLException {
        User user = DBUtils.saveUser( String.valueOf(IdWorker.getId()) , RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(3), "123456");
        Assert.assertNotNull(user );
    }

    @Test
    public void test_queryUserByUsername() throws SQLException {
        String username = RandomStringUtils.randomAlphabetic(10);
        User user   = DBUtils.saveUser( String.valueOf(IdWorker.getId()),  username , RandomStringUtils.randomAlphabetic(3), "123456");
        Assert.assertNotNull(user);
        User user2 = DBUtils.queryUserByUsername(username);
        Assert.assertNotNull(user2);
    }

    @Test
    public void test_saveFriend() throws SQLException {
        String username1 = RandomStringUtils.randomAlphabetic(10);
        String username2 = RandomStringUtils.randomAlphabetic(10);
        String nickname1 = RandomStringUtils.randomAlphabetic(3);
        String nickname2 = RandomStringUtils.randomAlphabetic(3);
        User user1 = DBUtils.saveUser(  String.valueOf(IdWorker.getId()), username1 ,nickname1 , "123456");
        User user2 = DBUtils.saveUser(  String.valueOf(IdWorker.getId()) , username2 ,nickname2 , "123456");

        boolean b = DBUtils.saveFriend(user1.getUserId(), user2.getUserId());

        Assert.assertTrue(b);
        b = DBUtils.saveFriend(user1.getUserId(), user2.getUserId());
        Assert.assertFalse(b);
        b = DBUtils.saveFriend(user2.getUserId(), user1.getUserId());
        Assert.assertFalse(b);

    }

    @Test
    public void test_queryUserByKeywork() throws SQLException {
        String username = RandomStringUtils.randomAlphabetic(10);
        String nickname = RandomStringUtils.randomAlphabetic(3);
        User user  = DBUtils.saveUser( String.valueOf(IdWorker.getId()) ,  username ,nickname , "123456");
        Assert.assertNotNull(user );
        List<User> users = DBUtils.queryUserByKeywork(nickname);
        Assert.assertTrue(users.size() > 0 );
    }

    @Test
    public void test_saveGroup() throws SQLException {
        String token = RandomStringUtils.randomAlphabetic(10);
        String groupName = RandomStringUtils.randomAlphabetic(10);
        Group group = DBUtils.saveGroup("1", token, groupName);
        Assert.assertNotNull( group );
        boolean error = false ;
        try{
            DBUtils.saveGroup("1" , token, groupName);
        }catch (SQLException e){
            error = true ;
        }
        Assert.assertTrue(error);
    }

    @Test
    public void test_saveGroupUser_queryJoinGroupBy() throws SQLException {
        String username = RandomStringUtils.randomAlphabetic(10);
        User user  = DBUtils.saveUser(String.valueOf(IdWorker.getId()) ,  username , RandomStringUtils.randomAlphabetic(3), "123456");

        String token = RandomStringUtils.randomAlphabetic(10);
        String groupName = RandomStringUtils.randomAlphabetic(10);
        Group group  = DBUtils.saveGroup("1", token, groupName);

        boolean b  = DBUtils.saveGroupUser(user.getUserId(), group.getGroupId());
        Assert.assertTrue(b);

        List<Group> groups = DBUtils.queryJoinGroupBy(user.getUserId());

        Assert.assertTrue(groups.size() > 0 );
    }

    @Test
    public void test_saveMessage() throws SQLException {

        String username1 = RandomStringUtils.randomAlphabetic(10);
        String username2 = RandomStringUtils.randomAlphabetic(10);
        String nickname1 = RandomStringUtils.randomAlphabetic(3);
        String nickname2 = RandomStringUtils.randomAlphabetic(3);
        User user1  = DBUtils.saveUser(String.valueOf(IdWorker.getId()) ,  username1 ,nickname1 , "123456");
        User user2 = DBUtils.saveUser( String.valueOf(IdWorker.getId()) , username2 ,nickname2 , "123456");
        ImMessage imMessage = new ImMessage();
        imMessage.setContent("hi");
        imMessage.setCreateTime(new Date());
        imMessage.setSender(user1.getUserId());
        imMessage.setSenderName(user1.getNickname());
        imMessage.setMsgId(String.valueOf( IdWorker.getId()));
        imMessage.setMsgType(1);
        imMessage.setStatus(1);
        imMessage.setTarget(user2.getUserId());
        imMessage.setTargetName(user2.getNickname());
        imMessage.setType(1);

        DBUtils.saveMessage(imMessage);
    }

    @Test
    public void test_saveGroupMessage_queryGroupHistoryMessage() throws SQLException {

        String username1 = RandomStringUtils.randomAlphabetic(10);
        String username2 = RandomStringUtils.randomAlphabetic(10);
        String nickname1 = RandomStringUtils.randomAlphabetic(3);
        String nickname2 = RandomStringUtils.randomAlphabetic(3);
        User user1  = DBUtils.saveUser(String.valueOf(IdWorker.getId()) ,  username1 ,nickname1 , "123456");
        User user2 = DBUtils.saveUser( String.valueOf(IdWorker.getId()) , username2 ,nickname2 , "123456");


        String token = RandomStringUtils.randomAlphabetic(10);
        String groupName = RandomStringUtils.randomAlphabetic(10);
        Group group = DBUtils.getGroupByToken(token);

        GroupMessage groupMessage = new GroupMessage();
        groupMessage.setContent("hi");
        groupMessage.setCreateTime(new Date());
        groupMessage.setSender(user1.getUserId());
        groupMessage.setSenderName(user1.getNickname());
        groupMessage.setMsgId(String.valueOf( IdWorker.getId()));
        groupMessage.setMsgType(1);
        groupMessage.setStatus(1);
        groupMessage.setGroupId(group.getGroupId());
        groupMessage.setGroupName(group.getGroupName());
        groupMessage.setType(1);

        DBUtils.saveGroupMessage(groupMessage);


        List<GroupMessage> messages = DBUtils.queryGroupHistoryMessage(group.getGroupId());

        Assert.assertEquals(1, messages.size() );

    }





}
