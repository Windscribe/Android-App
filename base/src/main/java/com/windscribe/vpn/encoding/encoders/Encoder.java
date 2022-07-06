/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.encoding.encoders;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Encode and decode byte arrays (typically from binary to 7-bit ASCII
 * encodings).
 */
public interface Encoder {

    int decode(String data, OutputStream out) throws IOException;

    int encode(byte[] data, int off, int length, OutputStream out) throws IOException;
}
