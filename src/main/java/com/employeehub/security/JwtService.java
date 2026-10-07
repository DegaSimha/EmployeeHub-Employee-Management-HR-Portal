package com.employeehub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final byte[] secret;
    private final long expirationMs;
    private final ObjectMapper mapper;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs, ObjectMapper mapper) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) throw new IllegalArgumentException("JWT secret must contain at least 32 bytes.");
        this.expirationMs = expirationMs;
        this.mapper = mapper;
    }
    public String createToken(String email, String role) {
        try {
            String header = encode(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = encode(mapper.writeValueAsBytes(Map.of("sub", email, "role", role,
                    "exp", Instant.now().plusMillis(expirationMs).getEpochSecond())));
            String content = header + "." + payload;
            return content + "." + ENCODER.encodeToString(sign(content));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create authentication token.", ex);
        }
    }
    public Optional<Claims> verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !java.security.MessageDigest.isEqual(
                    sign(parts[0] + "." + parts[1]), DECODER.decode(parts[2]))) return Optional.empty();
            Map<?, ?> payload = mapper.readValue(DECODER.decode(parts[1]), Map.class);
            String email = (String) payload.get("sub");
            String role = (String) payload.get("role");
            long expiry = ((Number) payload.get("exp")).longValue();
            if (email == null || role == null || expiry <= Instant.now().getEpochSecond()) return Optional.empty();
            return Optional.of(new Claims(email, role));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }
    private String encode(byte[] value) { return ENCODER.encodeToString(value); }
    public record Claims(String email, String role) {}
}
