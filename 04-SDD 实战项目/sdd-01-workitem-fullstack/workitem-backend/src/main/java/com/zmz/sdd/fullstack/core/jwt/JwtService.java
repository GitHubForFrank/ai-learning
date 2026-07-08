package com.zmz.sdd.fullstack.core.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §1.6 + conventions/security-conventions.md §13.3]
 * HS256 + 1h
 */
@Slf4j
@Service
public class JwtService {

    @Value("${auth.jwt.secret}")
    private String secret;

    @Value("${auth.jwt.expires-seconds}")
    private long expiresSeconds;

    /**
     * Issue a token. claim "sub" = appUserId, "username" = username.
     */
    public String issue(long appUserId, String username) {
        try {
            JWSSigner signer = new MACSigner(secret.getBytes());
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(appUserId))
                    .claim("username", username)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(expiresSeconds)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    /**
     * Parse + verify. Returns claims if valid; empty otherwise.
     */
    public Optional<JWTClaimsSet> verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secret.getBytes());
            if (!jwt.verify(verifier)) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.before(new Date())) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (ParseException | JOSEException e) {
            log.debug("JWT verify failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public long getExpiresSeconds() {
        return expiresSeconds;
    }
}
