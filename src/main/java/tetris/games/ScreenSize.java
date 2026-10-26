package tetris.games;

public enum ScreenSize {
    DEFAULT("기본 화면"), SMALL("작은 화면"), FULLSCREEN("전체 화면");
    private final String label;
    ScreenSize(String label) { this.label = label; }
    public String getLabel() { return label; }
}
