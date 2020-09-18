package hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.net.SocketFactory;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author wangnan
 * @since 1.0.0, 2020/9/18
 **/
@Component
@EnableScheduling
public class DynamicListOfServer {

    private static final Logger logger = LoggerFactory.getLogger(DynamicListOfServer.class);

    /**
     * 可以动态刷新全量 socket 节点（如 apollo 分布式配置中心）
     */
    @Value("${connectStr:localhost:8090,localhost:9092,localhost:9999}")
    private String connectStr;

    private static final Set<SocketAddress> activeSocketAddresses = new CopyOnWriteArraySet<>();


    private Set<SocketAddress> convertInitConnectStr() {
        return Stream.of(connectStr.split(",")).map(connectStr -> new SocketAddress(connectStr.split(":")[0], Integer.parseInt(connectStr.split(":")[1]))).collect(Collectors.toSet());
    }

    public static String convertToConnectStr(Set<SocketAddress> socketAddresses) {
        if (socketAddresses.isEmpty()) {
            return "";
        }

        return socketAddresses.stream().map(socketAddress -> socketAddress.getHostName() + ":" + socketAddress.getPort()).collect(Collectors.joining(","));
    }

    public Set<SocketAddress> getAvailableSocketAddresses(boolean fresh) {
        if (activeSocketAddresses.isEmpty()) {
            synchronized (activeSocketAddresses) {
                if (activeSocketAddresses.isEmpty()) {
                    activeSocketAddresses.addAll(convertInitConnectStr());
                }
            }
        }

        if (fresh) {
            synchronized (activeSocketAddresses) {
                // 每次从全量数据中寻找活跃的节点
                Set<SocketAddress> addedSocketAddresses = new HashSet<>();
                Set<SocketAddress> removedSocketAddresses = new HashSet<>();
                for (SocketAddress socketAddress : convertInitConnectStr()) {
                    boolean active = isRemoteTcpAvailable(socketAddress.getHostName(), socketAddress.getPort());
                    if (!active) {
                        if (activeSocketAddresses.contains(socketAddress)) {
                            removedSocketAddresses.add(socketAddress);
                            logger.warn("socket connection: {} is unreachable, and will be removed from the available list", socketAddress);
                        }
                    } else {
                        if (!activeSocketAddresses.contains(socketAddress)) {
                            addedSocketAddresses.add(socketAddress);
                            logger.info("socket connection: {} is available now, and will be added to the available list", socketAddress);
                        }
                    }
                }
                if (!addedSocketAddresses.isEmpty()) {
                    activeSocketAddresses.addAll(addedSocketAddresses);
                    logger.warn("added socket list: {}", addedSocketAddresses);
                }
                if (!removedSocketAddresses.isEmpty()) {
                    // 如果没有存活的节点了，那就不删除了
                    if (removedSocketAddresses.size() == activeSocketAddresses.size()) {
                        logger.error("no active socket addresses any more, please check it.");
                        // TODO alarm..
                    } else {
                        activeSocketAddresses.removeAll(removedSocketAddresses);
                        logger.warn("removed socket list: {}", removedSocketAddresses);
                    }
                }
            }
        }

        return activeSocketAddresses;
    }

    public Set<SocketAddress> getAvailableSocketAddresses() {
        return getAvailableSocketAddresses(false);
    }

    //    @Scheduled(cron = "0/5 * * * * *")
    public void freshActiveServiceInstances() {
        if (logger.isDebugEnabled()) {
            logger.debug("refresh the active socket addresses [{}] if necessary ..", activeSocketAddresses);
        }
        getAvailableSocketAddresses(true);
    }

    private boolean isRemoteTcpAvailable(String host, int port) {
        try {
            SocketFactory.getDefault().createSocket(host, port)
                    .close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }


}

class SocketAddress {
    // The hostname of the Socket Address
    private final String hostname;
    // The IP address of the Socket Address
    private InetAddress addr;
    // The port number of the Socket Address
    private final int port;

    public SocketAddress(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public SocketAddress(String hostname, InetAddress addr, int port) {
        this.hostname = hostname;
        this.addr = addr;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getAddress() {
        return addr;
    }

    public String getHostName() {
        if (hostname != null)
            return hostname;
        if (addr != null)
            return addr.getHostName();
        return null;
    }

    public String getHostString() {
        if (hostname != null)
            return hostname;
        if (addr != null) {
            if (getHostName() != null)
                return getHostName();
            else
                return addr.getHostAddress();
        }
        return null;
    }

    private boolean isUnresolved() {
        return addr == null;
    }

    @Override
    public String toString() {
        if (isUnresolved()) {
            return hostname + ":" + port;
        } else {
            return addr.toString() + ":" + port;
        }
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SocketAddress))
            return false;
        SocketAddress that = (SocketAddress) obj;
        boolean sameIP;
        if (addr != null)
            sameIP = addr.equals(that.addr);
        else if (hostname != null)
            sameIP = (that.addr == null) &&
                    hostname.equalsIgnoreCase(that.hostname);
        else
            sameIP = (that.addr == null) && (that.hostname == null);
        return sameIP && (port == that.port);
    }

    @Override
    public final int hashCode() {
        if (addr != null)
            return addr.hashCode() + port;
        if (hostname != null)
            return hostname.toLowerCase().hashCode() + port;
        return port;
    }

}