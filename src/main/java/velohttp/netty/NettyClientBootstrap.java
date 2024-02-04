package velohttp.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import velohttp.future.DefaultFuture;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class NettyClientBootstrap {

    private static final Logger log = LoggerFactory.getLogger(NettyClientBootstrap.class.getName());


    public final String ID = UUID.randomUUID().toString();
    private String host;
    private int port;
    private int connectTimeout;
    private volatile boolean isOpen = false;
    private final AtomicLong requestIndexRecord = new AtomicLong();
    private final AtomicLong responseIndexRecord = new AtomicLong();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final ConcurrentHashMap<Long, DefaultFuture<Object>> resultMap = new ConcurrentHashMap<>();

    private Channel channel = null;

    public static NettyClientBootstrap create() {
        return new NettyClientBootstrap();
    }

    public NettyClientBootstrap host(String host) {
        this.host = host;
        return this;
    }

    public NettyClientBootstrap port(Integer port) {
        this.port = port;
        return this;
    }

    public NettyClientBootstrap connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isClose() {
        return !isOpen;
    }

    private NettyClientBootstrap() {}

    /**
     * 使用唯一id，增加比较速度
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        return Objects.equals(ID, ((NettyClientBootstrap)obj).ID);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public NettyClientBootstrap build() {
        EventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();

        try {
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            channel.pipeline().addLast(new HttpClientCodec());
                            channel.pipeline().addLast(new HttpObjectAggregator(65536));
                            channel.pipeline().addLast(new HttpContentDecompressor());
                            channel.pipeline().addLast(new HttpClientHandler());
                        }
                    });
            this.channel = bootstrap.connect(host, port).sync().channel();
            channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                group.shutdownGracefully();
                log.info("{} 链路关闭", channelFuture.channel().toString());
            });
            if (!countDownLatch.await(connectTimeout, TimeUnit.MILLISECONDS)) {
                channel.close();
            }
        } catch (InterruptedException e) {
            log.error("connect timeout", e);
            channel.close();
        } catch (Exception e) {
            log.error("build error", e);
            channel.close();
        }
        return this;
    }

    public Object execute(FullHttpRequest request) throws InterruptedException, ExecutionException {
        Future<Object> future = asyncExecute(request, 10000);
        return future.get();
    }

    public Object execute(FullHttpRequest request, int timeout) throws InterruptedException, ExecutionException, TimeoutException {
        Future<Object> future = asyncExecute(request, timeout);
        return future.get(timeout, TimeUnit.MILLISECONDS);
    }

    public Future<Object> asyncExecute(FullHttpRequest request) throws InterruptedException, ExecutionException {
        return asyncExecute(request, 10000);
    }

    public Future<Object> asyncExecute(FullHttpRequest request, int timeout) throws InterruptedException {
        // 请求序号
        return doExecute(request, timeout);
    }

    /**
     * 发送请求
     */
    private DefaultFuture<Object> doExecute(FullHttpRequest request, int timeout) throws InterruptedException {
        if (!isOpen) {
            throw new NullPointerException("channel is null");
        }
        synchronized (this) {
            // 在HTTP管道化中，客户端可以在等待第一个响应之前发送多个请求。但是，响应必须按照请求被发送的顺序返回。
            // 尽管这增加了复杂性，但它允许更有效的网络利用。
            long index = requestIndexRecord.incrementAndGet();
//            log.debug("send request:{}", ID);
            channel.writeAndFlush(request).await(timeout, TimeUnit.MILLISECONDS);

            DefaultFuture<Object> defaultFuture = new DefaultFuture<>();
            resultMap.put(index, defaultFuture);
            return defaultFuture;
        }
    }

    private class HttpClientHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("channelActive");
            isOpen = Boolean.TRUE;
            countDownLatch.countDown();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // 响应必然有序
            long index = responseIndexRecord.incrementAndGet();
//            if(msg instanceof FullHttpResponse){
            DefaultFuture<Object> future = resultMap.remove(index);
            if (future == null) {
                log.error("ID:{}, respIndex:{}", ID, index);
            } else {
                future.setResult(msg);
            }
//            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            log.info("channelInactive");
            isOpen = Boolean.FALSE;
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            super.channelUnregistered(ctx);
            log.info("channelUnregistered");
            isOpen = Boolean.FALSE;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            log.info("exceptionCaught");
            isOpen = Boolean.FALSE;
        }

    }



}
