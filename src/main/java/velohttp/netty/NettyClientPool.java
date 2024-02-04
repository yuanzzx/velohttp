package velohttp.netty;

import velohttp.config.RequestConfig;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NettyClientPool {

    /**
     * 连接池大小
     */
    private final Integer pollSize = (int)Short.MAX_VALUE;

    /**
     * 一个连接最大请求次数
     */
    private int maxRequestTimes = Integer.MAX_VALUE;

    /**
     * 一个连接最大请求次数
     */
    private int defaultConnectTimeout = Integer.MAX_VALUE;

    // 不可能会有这么多请求
    private final ArrayBlockingQueue<NettyClientBootstrap> nettyClientQueue = new ArrayBlockingQueue<>(pollSize);

    private final ConcurrentHashMap<NettyClientBootstrap, AtomicInteger> useCounter = new ConcurrentHashMap<>();

    private String host;

    private int port;


    public static NettyClientPool create() {
        return new NettyClientPool();
    }

    public NettyClientPool host(String host) {
        this.host = host;
        return this;
    }

    public NettyClientPool port(Integer port) {
        this.port = port;
        return this;
    }

    public NettyClientPool maxRequestTimes(Integer maxRequestTimes) {
        this.maxRequestTimes = maxRequestTimes;
        return this;
    }

    public NettyClientPool build() {
        return this;
    }

    public NettyClientBootstrap getNettyClientBootstrap(RequestConfig requestConfig) {
        int connectTimeout = requestConfig != null ? requestConfig.getConnectTimeout() : defaultConnectTimeout;
        NettyClientBootstrap nettyClientBootstrap = null;
        do {
            // 如果是第一次进入, 会有并发问题，但是没关系，多创建几个影响不大
            if (nettyClientQueue.size() == 0) {
                nettyClientBootstrap = buildNettyClientBootstrap(requestConfig);
            }

            // 非第一次进入
            if (nettyClientBootstrap == null) {
                try {
                    // 花费1/10的时间等待
                    nettyClientBootstrap = nettyClientQueue.poll(connectTimeout / 10, TimeUnit.MILLISECONDS);
                    // 判断是否有效
                    if (nettyClientBootstrap != null && nettyClientBootstrap.isClose()) {
                        // 从计数器中删除
                        useCounter.remove(nettyClientBootstrap);
                        // 无效则不使用
                        nettyClientBootstrap = null;
                    }
                } catch (InterruptedException e) {
                }

                if (nettyClientBootstrap == null) {
                    nettyClientBootstrap = buildNettyClientBootstrap(requestConfig);
                }
            }

            // 如果超限则不加入
            if (useCounter.get(nettyClientBootstrap).incrementAndGet() > maxRequestTimes) {
                // 从计数器中删除
                useCounter.remove(nettyClientBootstrap);
                // 重新进入循环
                nettyClientBootstrap = null;
            } else {
                // 重新加入集合，循环使用
                nettyClientQueue.offer(nettyClientBootstrap);
            }
        } while(nettyClientBootstrap == null);

        return nettyClientBootstrap;
    }

    public NettyClientBootstrap buildNettyClientBootstrap(RequestConfig requestConfig) {
        NettyClientBootstrap nettyClientBootstrap = NettyClientBootstrap.create().host(host).port(port);
        nettyClientBootstrap.connectTimeout(requestConfig != null ? requestConfig.getConnectTimeout() : defaultConnectTimeout);
        nettyClientBootstrap.build();
        if (nettyClientBootstrap.isOpen()) {
            useCounter.put(nettyClientBootstrap, new AtomicInteger(0));
        }
        return nettyClientBootstrap;
    }
}
