package velohttp;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import velohttp.entity.Header;
import velohttp.entity.HttpUriRequest;
import velohttp.entity.HttpUriResponse;
import velohttp.netty.NettyClientBootstrap;
import velohttp.netty.NettyClientPool;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 目前待完成：
 * 1.连接超时处理
 * 2.数据传输超时处理
 * 3.失败重试机制
 */
public class VelohttpClient {

    private static final Logger log = LoggerFactory.getLogger(VelohttpClient.class.getName());

    private final ConcurrentHashMap<String, NettyClientPool> nettyClientPoolMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> lock = new ConcurrentHashMap<>();

    public HttpUriResponse execute(HttpUriRequest request) {
        String queryUrl = this.urlEncoder(request.getUri());
        FullHttpRequest httpRequest = new DefaultFullHttpRequest(request.getHttpVersion(), request.getMethod(), queryUrl);
        if (request.getEntity() != null) {
            httpRequest = httpRequest.replace(Unpooled.copiedBuffer(request.getEntity().getBinaryContent()));
        }
        for (Header header : request.getHeaderGroup().getAllHeaders()) {
            httpRequest.headers().add(header.getName(), header.getValue());
        }

        NettyClientBootstrap nettyClientBootstrap = getNettyClientBootstrap(request);
        try {
            Object resp = nettyClientBootstrap.execute(httpRequest, request.getRequestConfig().getSocketTimeout());
            return new HttpUriResponse((FullHttpResponse)resp);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("nettyClient execute error", e);
            throw new RuntimeException(e);
        }
    }

    private NettyClientBootstrap getNettyClientBootstrap(HttpUriRequest request) {
        String domain = request.getUri().getAuthority();
        // 是否存在
        while (!nettyClientPoolMap.containsKey(domain)) {
            // 获取锁
            String key = UUID.randomUUID().toString();
            lock.putIfAbsent(domain, key);
            if (Objects.equals(key, lock.get(domain))) {
                NettyClientPool nettyClientPool = NettyClientPool.create()
                        .host(request.getUri().getHost()).port(request.getUri().getPort());
                nettyClientPoolMap.put(domain, nettyClientPool);
                lock.remove(domain, key);
            }
            Thread.yield();
        }

        return nettyClientPoolMap.get(domain).getNettyClientBootstrap(request.getRequestConfig());
    }

    private String urlEncoder(URI uri) {
        String queryUrl = uri.getRawPath();
        if (uri.getQuery() != null) {
            StringBuilder builder = new StringBuilder();
            for (String split : uri.getQuery().split("&")) {
                String[] split1 = split.split("=");
                try {
                    builder.append(URLEncoder.encode(split1[0], "UTF-8")).append("=")
                            .append(URLEncoder.encode(split1[1], "UTF-8")).append("&");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
            builder.setLength(builder.length() -1);
            queryUrl = queryUrl + "?" + builder.toString();
        }
        return queryUrl;
    }

}
