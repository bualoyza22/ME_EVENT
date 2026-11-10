package com.bakban9.web.pub;

import jakarta.servlet.http.HttpServletRequest;

final class ClientIp {

    private ClientIp() {}

    /** Behind a proxy set FORWARD_HEADERS_STRATEGY=native so getRemoteAddr() is the visitor IP. */
    static String of(HttpServletRequest req) {
        return req.getRemoteAddr();
    }
}
