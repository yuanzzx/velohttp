package velohttp.entity;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import velohttp.config.RequestConfig;

import java.io.Serializable;
import java.net.URI;

/**
 * @Author: yuanzhanzhenxing
 */
public class HttpUriRequest implements Serializable {

    /**
     * 请求地址
     */
    private URI uri;

    /**
     * 请求版本
     */
    private HttpVersion httpVersion = HttpVersion.HTTP_1_1;

    /**
     * 请求方法
     */
    private HttpMethod method;

    /**
     * 请求内容
     */
    private StringEntity entity;

    /**
     * 请求头
     */
    private HeaderGroup headerGroup;

    /**
     * 请求配置
     */
    private RequestConfig requestConfig;

    public HttpUriRequest(String uri) {
        setUri(URI.create(uri));
        this.headerGroup = new HeaderGroup();
        setHeader(HttpHeaderNames.HOST.toString(), this.uri.getHost());
    }

    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    public StringEntity getEntity() {
        return entity;
    }

    public void setEntity(StringEntity entity) {
        this.entity = entity;
        setHeader(HttpHeaderNames.CONTENT_LENGTH.toString(), String.valueOf(entity.getContentLength()));
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI url) {
        this.uri = url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public HeaderGroup getHeaderGroup() {
        return headerGroup;
    }

    public void setHeaderGroup(HeaderGroup headerGroup) {
        this.headerGroup = headerGroup;
    }

    public void setHeader(String name, String value) {
        this.headerGroup.updateHeader(new Header(name, value));
    }

    public RequestConfig getRequestConfig() {
        return requestConfig;
    }

    public void setRequestConfig(RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
    }

    public static class HttpPostRequest extends HttpUriRequest {

        public HttpPostRequest(String uri) {
            super(uri);
            super.method = HttpMethod.POST;
        }
    }

    public static class HttpGetRequest extends HttpUriRequest {

        public HttpGetRequest(String uri) {
            super(uri);
            super.method = HttpMethod.GET;
        }
    }
}
