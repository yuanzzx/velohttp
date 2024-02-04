package velohttp.entity;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;

import java.io.Serializable;

public class HttpUriResponse implements Serializable {

    private FullHttpResponse response;

    public HttpUriResponse(FullHttpResponse response) {
        this.response = response;
    }


    public String getEntity() {
        return response.content().toString(CharsetUtil.UTF_8);
    }
}
