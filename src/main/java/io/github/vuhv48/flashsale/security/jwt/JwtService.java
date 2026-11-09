package io.github.vuhv48.flashsale.security.jwt;

import io.github.vuhv48.flashsale.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

	private final JwtProperties properties;
	private final SecretKey signingKey;

	public JwtService(
			JwtProperties properties,
			@Value("${app.security.jwt.secret}") String rawSecret
	) {
		this.properties = properties;
		this.signingKey = deriveKey(rawSecret);
	}

	private static SecretKey deriveKey(String rawSecret) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawSecret.getBytes(StandardCharsets.UTF_8));
			return Keys.hmacShaKeyFor(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public String generateAccessToken(UserDetails user, UUID userId) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(properties.expirationSeconds());
		String authorities = user.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(","));
		return Jwts.builder()
				.subject(user.getUsername())
				.issuer(properties.issuer())
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.claim("uid", userId.toString())
				.claim("authorities", authorities)
				.signWith(signingKey)
				.compact();
	}

	public Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
