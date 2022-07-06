/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.encoding.io.pem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("all")
public class PemObject
        implements PemObjectGenerator {

    private static final List EMPTY_LIST = Collections.unmodifiableList(new ArrayList());

    private byte[] content;

    private List headers;

    private String type;

    /**
     * Generic constructor for object without headers.
     *
     * @param type    pem object type.
     * @param content the binary content of the object.
     */
    public PemObject(String type, byte[] content) {
        this(type, EMPTY_LIST, content);
    }

    /**
     * Generic constructor for object with headers.
     *
     * @param type    pem object type.
     * @param headers a list of PemHeader objects.
     * @param content the binary content of the object.
     */
    public PemObject(String type, List headers, byte[] content) {
        this.type = type;
        this.headers = Collections.unmodifiableList(headers);
        this.content = content;
    }

    public PemObject generate() {
        return this;
    }

    public byte[] getContent() {
        return content;
    }

    public List getHeaders() {
        return headers;
    }

    public String getType() {
        return type;
    }
}
