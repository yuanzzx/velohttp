package velohttp.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class HeaderGroup implements Serializable {

    private final Header[] EMPTY = new Header[] {};

    private final LinkedList<Header> headers;

    public HeaderGroup() {
        this.headers = new LinkedList<>();
    }

    public void clear() {
        headers.clear();
    }

    public void addHeader(Header header) {
        if (header == null) {
            return;
        }
        headers.add(header);
    }

    public void removeHeader(Header header) {
        if (header == null) {
            return;
        }
        headers.remove(header);
    }

    public void updateHeader(Header header) {
        if (header == null) {
            return;
        }

        for (int i = 0; i < this.headers.size(); i++) {
            final Header current = this.headers.get(i);
            if (current.getName().equalsIgnoreCase(header.getName())) {
                this.headers.set(i, header);
                return;
            }
        }
        this.headers.add(header);
    }

    public void setHeaders(Header[] headers) {
        clear();
        if (headers == null) {
            return;
        }
        Collections.addAll(this.headers, headers);
    }


    public Header getFirstHeader(final String name) {
        for (int i = 0; i < this.headers.size(); i++) {
            final Header header = this.headers.get(i);
            if (header.getName().equalsIgnoreCase(name)) {
                return header;
            }
        }
        return null;
    }

    public Header getLastHeader(final String name) {
        // start at the end of the list and work backwards
        for (int i = headers.size() - 1; i >= 0; i--) {
            final Header header = headers.get(i);
            if (header.getName().equalsIgnoreCase(name)) {
                return header;
            }
        }
        return null;
    }

    public List<Header> getAllHeaders() {
        return headers;
    }

    public Header[] getAllHeaderArray() {
        return headers.toArray(new Header[headers.size()]);
    }

    public Header[] getHeaders(final String name) {
        List<Header> headersFound = null;
        for (int i = 0; i < this.headers.size(); i++) {
            final Header header = this.headers.get(i);
            if (header.getName().equalsIgnoreCase(name)) {
                if (headersFound == null) {
                    headersFound = new ArrayList<Header>();
                }
                headersFound.add(header);
            }
        }
        return headersFound != null ? headersFound.toArray(new Header[headersFound.size()]) : EMPTY;
    }

}