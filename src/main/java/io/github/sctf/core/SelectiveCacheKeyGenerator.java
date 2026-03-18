package io.github.sctf.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 의존성 그래프의 클래스 집합으로부터 고유한 캐시 키를 생성하는 생성기.
 *
 * <p>Spring Test의 컨텍스트 캐싱 메커니즘에서 동일한 의존성 구성을 가진 테스트들이
 * 같은 ApplicationContext를 재사용할 수 있도록, 클래스 이름을 정렬 후 SHA-256 해시로 변환한다.</p>
 *
 * @see SelectiveContextCustomizer#equals(Object)
 * @see SelectiveContextCustomizer#hashCode()
 */
public class SelectiveCacheKeyGenerator {

    /**
     * 스캔된 클래스 집합을 기반으로 SHA-256 캐시 키를 생성한다.
     *
     * <p>클래스 이름을 알파벳순으로 정렬한 뒤 콤마로 연결하여 SHA-256 해시를 생성한다.
     * 정렬을 통해 집합의 순서에 관계없이 동일한 구성이면 항상 같은 키가 생성됨을 보장한다.</p>
     *
     * @param scannedClasses 의존성 그래프에서 탐색된 클래스 집합
     * @return SHA-256 해시값의 16진수 문자열
     * @throws RuntimeException SHA-256 알고리즘을 사용할 수 없는 경우
     */
    public String generateKey(Set<Class<?>> scannedClasses) {
        return generateKey(scannedClasses, true, "", false);
    }

    /**
     * 슬라이스 구성(클래스 집합 + 플래그) 전체를 캐시 키에 반영한다.
     * 동일 클래스 집합이라도 {@code withDatabase}, {@code basePackage}, {@code stubSecurityInfrastructure}가
     * 다르면 서로 다른 ApplicationContext를 쓰도록 한다.
     */
    public String generateKey(Set<Class<?>> scannedClasses, boolean withDatabase, String basePackage,
            boolean stubSecurityInfrastructure) {
        String joinedNames = scannedClasses.stream()
                .map(Class::getName)
                .sorted()
                .collect(Collectors.joining(","));
        String payload = joinedNames
                + "\0withDatabase=" + withDatabase
                + "\0basePackage=" + (basePackage != null ? basePackage : "")
                + "\0stubSecurity=" + stubSecurityInfrastructure;
        return sha256Hex(payload);
    }

    private static String sha256Hex(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시 알고리즘을 초기화할 수 없습니다.", e);
        }
    }

}
