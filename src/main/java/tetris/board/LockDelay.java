package tetris.board;

/**
 * 블록이 바닥이나 다른 블록 위에 닿은 뒤 고정되기까지의 여유 시간(lock delay).
 * 자동 하강·소프트 드롭으로 닿았을 때 바로 고정하지 않고 delayMillis 동안 이동·회전할 기회를 준다.
 * 여유 중에 이동·회전에 성공하면 처음부터 다시 세며, 무한히 버티지 못하도록 블록 하나당 maxResets번까지만 다시 센다.
 * 하드 드롭은 이 클래스를 거치지 않고 즉시 고정한다. 시간은 호출하는 쪽이 밀리초로 넘긴다(테스트에서 시간을 직접 지정).
 */
public final class LockDelay {
    public static final long DEFAULT_DELAY_MILLIS = 500;
    public static final int DEFAULT_MAX_RESETS = 15;

    private final long delayMillis;
    private final int maxResets;
    /** 지금 바닥에 닿아 고정을 기다리는 중인지. */
    private boolean waiting;
    /** 기다리기 시작한(또는 마지막으로 다시 센) 시각. waiting일 때만 의미가 있다. */
    private long startedAtMillis;
    private int resetCount;

    public LockDelay() {
        this(DEFAULT_DELAY_MILLIS, DEFAULT_MAX_RESETS);
    }

    public LockDelay(long delayMillis, int maxResets) {
        if (delayMillis < 0 || maxResets < 0) {
            throw new IllegalArgumentException("고정 여유 설정이 올바르지 않습니다: delayMillis=" + delayMillis
                    + ", maxResets=" + maxResets);
        }
        this.delayMillis = delayMillis;
        this.maxResets = maxResets;
    }

    /** 아래로 내려갈 수 없게 되었을 때 호출한다. 이미 기다리는 중이면 시간을 다시 세지 않는다. */
    public void onGrounded(long nowMillis) {
        if (!waiting) {
            waiting = true;
            startedAtMillis = nowMillis;
        }
    }

    /** 이동으로 발밑이 비어 다시 떨어질 수 있게 되었을 때 호출한다. 다시 닿으면 처음부터 센다. */
    public void onAirborne() {
        waiting = false;
    }

    /** 이동·회전에 성공했을 때 호출한다. 기다리는 중이고 리셋 횟수가 남아 있으면 처음부터 다시 센다. */
    public void onMoveOrRotate(long nowMillis) {
        if (waiting && resetCount < maxResets) {
            startedAtMillis = nowMillis;
            resetCount++;
        }
    }

    /** 여유 시간이 다 지나 지금 고정해야 하면 true. */
    public boolean isExpired(long nowMillis) {
        return waiting && nowMillis - startedAtMillis >= delayMillis;
    }

    public boolean isWaiting() { return waiting; }
    public int getResetCount() { return resetCount; }

    /** 새 블록이 생성되면 호출한다. 기다림과 리셋 횟수를 모두 초기화한다. */
    public void clear() {
        waiting = false;
        resetCount = 0;
    }
}
