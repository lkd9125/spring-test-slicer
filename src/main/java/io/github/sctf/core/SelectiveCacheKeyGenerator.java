package io.github.sctf.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.stream.Collectors;

public class SelectiveCacheKeyGenerator {

    /**
     * 의존성 그래프(클래스 셋)를 기반으로 고유한 SHA-256 캐시 키를 생성
     * @param scannedClasses 의존성 collections
     * @return hash value
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
