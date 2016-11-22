package org.wzj.im.core;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wens on 16-11-18.
 */
public class BlacklistService {

    private final static String BLACKLIST_PRE = "BLACKLIST_PRE:%s" ;

    private JedisPool jedisPool ;


    public BlacklistService(JedisPool jedisPool ){
        this.jedisPool = jedisPool ;
    }


    public void addGroupBlackList(String groupId , String userId ) {
        Jedis jedis = jedisPool.getResource();
        try{
            jedis.hset(String.format(BLACKLIST_PRE, groupId ) , userId , String.valueOf(System.currentTimeMillis()));
        }finally {
            if(jedis != null ){
                jedis.close();
            }
        }
    }

    public void delGroupBlackList(String groupId , String userId ) {

        Jedis jedis = jedisPool.getResource();
        try{
            jedis.hdel(String.format(BLACKLIST_PRE, groupId ) , userId );
        }finally {
            if(jedis != null ){
                jedis.close();
            }
        }

    }

    public boolean inGroupBlackList(String groupId , String userId ) {

        Jedis jedis = jedisPool.getResource();
        try{
            String t  = jedis.hget(String.format(BLACKLIST_PRE, groupId ) , userId );
            return t == null ? false : true  ;
        }finally {
            if(jedis != null ){
                jedis.close();
            }
        }
    }

    public Map<String,Boolean> getGroupBlackList(String groupId){
        Jedis jedis = jedisPool.getResource();
        try{
            Map<String, String> map = jedis.hgetAll(String.format(BLACKLIST_PRE, groupId));
            Map<String,Boolean> ret  = new HashMap<>();
            for(String key : map.keySet() ){
                ret.put(key,Boolean.TRUE);
            }
            return ret ;
        }finally {
            if(jedis != null ){
                jedis.close();
            }
        }
    }
}
