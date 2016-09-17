package org.wzj.im.common;

/**
 * Created by wens on 15-11-11.
 */
public class AddressAndPort {

    private String address;

    private int port;

    public AddressAndPort(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return this.address + ":" + this.port;
    }


}
