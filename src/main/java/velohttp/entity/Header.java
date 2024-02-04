package velohttp.entity;

import java.io.Serializable;
import java.util.Objects;

public class Header implements Serializable {

    private final String name;
    private final String value;

    public Header(final String name, final String value) {
//        this.name = Args.notNull(name, "Name");
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Header header = (Header) o;
        return Objects.equals(name, header.name) && Objects.equals(value, header.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
