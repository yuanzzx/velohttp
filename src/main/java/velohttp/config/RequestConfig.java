package velohttp.config;

public class RequestConfig {

    private int connectTimeout;

    private int socketTimeout;

    public static RequestConfig custom() {
        return new RequestConfig();
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public RequestConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public RequestConfig setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }
}
