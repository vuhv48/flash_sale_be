package io.github.vuhv48.flashsale.auth;

public record RegisterOutcome(AccessRefreshResult tokens, String message) {
}
