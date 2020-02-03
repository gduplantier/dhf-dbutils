package com.marklogic.util

import java.security.SecureRandom

class StringUtil {
    private StringUtil() {}

    static String randomString(int length = 16, String symbols = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ@_") {
        Random random = new SecureRandom()
        StringBuilder sb = new StringBuilder()
        for(int i = 0; i < length; i++) {
            sb.append(symbols.charAt(random.nextInt(symbols.length())))
        }
        sb.toString()
    }

    static String toCamelCase( String text ) {
		text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
		return text
	}
}
