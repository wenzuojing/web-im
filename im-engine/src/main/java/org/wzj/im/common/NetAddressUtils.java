package org.wzj.im.common;

/**
 * Created by wens on 15-11-11.
 */
public class NetAddressUtils {

    public static AddressAndPort resolve(String src, int defaultPort) {

        String[] strings = src.split(":");

        String addr = strings[0];

        int port = defaultPort;

        if (strings.length == 2) {
            port = Integer.parseInt(strings[1]);
        }

        return new AddressAndPort(addr, port);

    }
}
