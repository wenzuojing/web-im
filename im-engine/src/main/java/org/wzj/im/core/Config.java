package org.wzj.im.core;

import org.wzj.im.common.Constant;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * app config
 * <p>
 * Created by wens on 15-11-11.
 */
public class Config {

    private String bindAddress;

    private int bossThreads;

    private int workerThreads;

    private int maxPayload;

    private String dbUrl;

    private String dbUser;

    private String dbPassword;

    private String redisHost ;

    private int redisPort ;

    public Config() {
        this(Constant.CLASS_PATH_PREFIX + "config.properties");
    }

    public Config(String configPath) {

        if (configPath.startsWith(Constant.CLASS_PATH_PREFIX)) {
            configPath = Thread.currentThread().getContextClassLoader().getResource(configPath.substring(Constant.CLASS_PATH_PREFIX.length())).getPath();
        }

        InputStream confInputStream = null;

        try {
            confInputStream = new FileInputStream(configPath);
        } catch (FileNotFoundException e) {
            System.out.println("ERROR:can not find configure :" + configPath);
            System.exit(-1);
        }

        Properties prop = new Properties();
        try {
            prop.load(confInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                confInputStream.close();
            } catch (IOException e) {
                //
            }
        }

        this.bindAddress = prop.getProperty("bind.address", "0.0.0.0:8080");
        this.bossThreads = Integer.parseInt(prop.getProperty("boss.threads", "0"));
        this.workerThreads = Integer.parseInt(prop.getProperty("worker.threads", "0"));
        this.maxPayload = Integer.parseInt(prop.getProperty("max.payload", "10240"));

        this.dbUrl = prop.getProperty("jdbc.url");
        this.dbUser = prop.getProperty("jdbc.username");
        this.dbPassword = prop.getProperty("jdbc.password");
        this.redisHost = prop.getProperty("redis.host");
        this.redisPort = Integer.parseInt(prop.getProperty("redis.port"));

    }


    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public int getMaxPayload() {
        return maxPayload;
    }

    public void setMaxPayload(int maxPayload) {
        this.maxPayload = maxPayload;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(int redisPort) {
        this.redisPort = redisPort;
    }
}
