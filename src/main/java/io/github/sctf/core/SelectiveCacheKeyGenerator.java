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
        // 1. 순서 보장을 위해 이름순으로 정렬
        String joinedNames = scannedClasses.stream()
                .map(Class::getName)
                .sorted() // 💡 핵심: 정렬하지 않으면 실행할 때마다 해시가 달라져서 캐시가 다 깨집니다!
                .collect(Collectors.joining(","));

        // 2. 정렬된 거대한 문자열을 SHA-256 알고리즘으로 압축
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(joinedNames.getBytes(StandardCharsets.UTF_8));

            // 3. sha 256 암호화
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
