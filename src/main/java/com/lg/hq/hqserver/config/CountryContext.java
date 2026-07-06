package com.lg.hq.hqserver.config;

/**
 * 현재 스레드에서 사용할 국가 코드를 관리하는 ThreadLocal 홀더.
 * 멀티스레드 환경에서 각 스레드가 독립적으로 국가를 가짐.
 */
public class CountryContext {

    private static final ThreadLocal<String> CURRENT_COUNTRY = new ThreadLocal<>();

    public static void set(String countryCode) {
        CURRENT_COUNTRY.set(countryCode);
    }

    public static String get() {
        return CURRENT_COUNTRY.get();
    }

    public static void clear() {
        CURRENT_COUNTRY.remove();  // 메모리 누수 방지
    }
}