package tetris.network;

/** 시간 단위 ms. 기본값은 팀 합의 전의 변경 가능한 초기 설정이다. */
public record ConnectionOptions(int connectTimeoutMs, int heartbeatMs, int lagMs, int disconnectMs) {
    public static final ConnectionOptions DEFAULT = new ConnectionOptions(5000, 500, 1000, 5000);
    public ConnectionOptions {
        if (connectTimeoutMs <= 0 || heartbeatMs <= 0 || lagMs <= heartbeatMs || disconnectMs <= lagMs)
            throw new IllegalArgumentException("Require 0 < heartbeat < lag < disconnect");
    }
}
