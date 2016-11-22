package org.wzj.im.core;


import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.wzj.im.common.DBUtils;
import org.wzj.im.common.Group;
import org.wzj.im.common.IdWorker;
import org.wzj.im.common.User;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by wens on 16-9-22.
 */
@Controller
@EnableAutoConfiguration
public class HttpApiController  extends SpringBootServletInitializer implements EmbeddedServletContainerCustomizer{


    @RequestMapping("/api/group/create")
    @ResponseBody
    public ReturnResult createGroup(@RequestParam(value = "userId" ,required = false , defaultValue = "") String userId , @RequestParam("groupName")String groupName ) {
        if(StringUtils.isEmpty(userId)){
            userId = "1" ;
        }
        try {
            Group group  = DBUtils.saveGroup(userId, UUID.randomUUID().toString(), groupName);
            return ReturnResult.success(group);
        } catch (SQLException e) {
            return ReturnResult.fail("Create group fail") ;
        }
    }

    @RequestMapping("/api/group/join")
    @ResponseBody
    public ReturnResult joinGroup( @RequestParam(value = "userId") String userId , @RequestParam("groupId")String groupId) {
        try {
            DBUtils.saveGroupUser(userId , groupId ) ;
            return ReturnResult.success("OK");
        } catch (SQLException e) {
            return ReturnResult.fail("Join group fail") ;
        }
    }

    @RequestMapping("/api/group/online/count")
    @ResponseBody
    public ReturnResult userEnroll( @RequestParam("groupId")String groupId) {
        try {
            return ReturnResult.success(DBUtils.groupOnlineCount(groupId));
        } catch (SQLException e) {
            return ReturnResult.fail("group online count fail") ;
        }
    }

    @RequestMapping("/api/user/enroll")
    @ResponseBody
    public ReturnResult userEnroll( @RequestParam("username")String username,@RequestParam("nickname")String nickname ,@RequestParam("password")String password) {
        try {
            User user  = DBUtils.saveUser(String.valueOf(IdWorker.getId()),username,nickname,password);
            return ReturnResult.success(user);
        } catch (SQLException e) {
            return ReturnResult.fail("Enroll fail") ;
        }
    }



    public static void main(String[] args) {
        SpringApplication.run(HttpApiController.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(HttpApiController.class);
    }

    @Override
    public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {
        configurableEmbeddedServletContainer.setPort(9696);
    }
}
