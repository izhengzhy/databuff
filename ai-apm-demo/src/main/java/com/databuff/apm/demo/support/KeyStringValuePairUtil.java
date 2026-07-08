package com.databuff.apm.demo.support;

import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;

final class KeyStringValuePairUtil {

    private KeyStringValuePairUtil() {
    }

    static KeyStringValuePair of(String key, String value) {
        return KeyStringValuePair.newBuilder().setKey(key).setValue(value).build();
    }
}
