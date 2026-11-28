package tetris.ai;

/**
 * 컴퓨터 상대의 난이도. 키를 누르는 간격만 다르고 판단 방식은 같다.
 * 요구사항(Req3 도전 과제)의 0.5~1초 간격은 보통·쉬움이 그대로 따르고, 어려움은 더 빠르게 둔다(팀 결정).
 */
public enum AiDifficulty {
    EASY("쉬움", 1000),
    NORMAL("보통", 500),
    HARD("어려움", 200);

    private final String label;
    private final long intervalMillis;

    AiDifficulty(String label, long intervalMillis) {
        this.label = label;
        this.intervalMillis = intervalMillis;
    }

    /** 키 하나를 누른 뒤 다음 키까지 기다리는 시간(ms). */
    public long intervalMillis() { return intervalMillis; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
