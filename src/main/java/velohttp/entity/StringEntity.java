package velohttp.entity;



import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public class StringEntity {
    private final byte[] content;

    private final Charset charset;

    public StringEntity(final String string) {
        this(string, CharsetUtil.UTF_8);
    }

    public StringEntity(final String string, final Charset charset) {
        if (charset == null) {
            throw new NullPointerException("charset cannot be null");
        }
        this.charset = charset;
        this.content = string.getBytes(charset);
    }

    public byte[] getBinaryContent() {
        return this.content;
    }

    public String getContent() {
        return new String(this.content, charset);
    }

    public long getContentLength() {
        return this.content.length;
    }
}
