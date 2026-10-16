param([string]$JdkHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $JdkHome) { throw 'Pass -JdkHome with a Java 21+ JDK directory.' }
$checkDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ('tetris-srs-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $checkDirectory | Out-Null
$checkSource = @'
package tetris.block;
import java.util.*;
import java.util.function.Predicate;

public class SrsCheck {
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }
    static void same(int[][] a, int[][] b, String name) {
        check(Arrays.deepEquals(a, b), name);
    }
    static Set<String> occupied(int[][] cells) {
        Set<String> result = new HashSet<>();
        for (int[] c : cells) result.add(c[0] + "," + c[1]);
        return result;
    }
    static int[][] shape(String... rows) {
        List<int[]> cells = new ArrayList<>();
        for (int y=0; y<rows.length; y++) for (int x=0; x<rows[y].length(); x++)
            if (rows[y].charAt(x)=='#') cells.add(new int[]{x,y});
        return cells.toArray(int[][]::new);
    }
    // Test-only Board substitute; production Board is deliberately untouched.
    static boolean tryRotation(Block b, Predicate<int[][]> fits) {
        for (int n=0; n<b.getRotationOffsets().length; n++) {
            if (fits.test(b.previewRotation(n))) { b.rotate(n); return true; }
        }
        return false;
    }
    static boolean fits(int[][] cells) {
        for (int[] c:cells) if(c[0]<0 || c[0]>=10 || c[1]<0 || c[1]>=20) return false;
        return true;
    }
    public static void main(String[] args) {
        // Independent, screen-coordinate expected spawn->R shapes for every rotatable type.
        Map<TetrominoType,int[][]> right = Map.of(
            TetrominoType.I,shape("..#.","..#.","..#.","..#."),
            TetrominoType.J,shape(".##",".#.",".#."),
            TetrominoType.L,shape(".#.",".#.",".##"),
            TetrominoType.S,shape(".#.",".##","..#"),
            TetrominoType.Z,shape("..#",".##",".#."),
            TetrominoType.T,shape(".#.",".##",".#."));
        // Reference tables use y-up; compare to API after explicitly inverting y.
        int[][][] jUp={
            {{0,0},{-1,0},{-1,1},{0,-2},{-1,-2}},
            {{0,0},{1,0},{1,-1},{0,2},{1,2}},
            {{0,0},{1,0},{1,1},{0,-2},{1,-2}},
            {{0,0},{-1,0},{-1,-1},{0,2},{-1,2}}};
        int[][][] iUp={
            {{0,0},{-2,0},{1,0},{-2,-1},{1,2}},
            {{0,0},{-1,0},{2,0},{-1,2},{2,-1}},
            {{0,0},{2,0},{-1,0},{2,1},{-1,-2}},
            {{0,0},{1,0},{-2,0},{1,-2},{-2,1}}};
        for (var type:TetrominoType.values()) {
            var b=new Block(type,3,5);
            var original=b.getCells();
            var enumOriginal=type.getCells();
            if(type==TetrominoType.I) same(shape("....","####","....","...."), original,"I spawn row");
            for(int state=0;state<4;state++) {
                int[][] before=b.getCells();
                int[][] offsets=b.getRotationOffsets();
                if(type==TetrominoType.O) check(offsets.length==1,"O no kicks");
                else for(int n=0;n<5;n++) {
                    int[] reference=(type==TetrominoType.I?iUp:jUp)[state][n];
                    check(offsets[n][0]==reference[0] && offsets[n][1]==-reference[1],"kick table "+type+state+n);
                    var candidate=new Block(type,3,5);
                    for(int turn=0;turn<state;turn++) candidate.rotate();
                    var preview=candidate.previewRotation(n);
                    candidate.rotate(n);
                    same(preview,candidate.getBoardCells(),"candidate commit");
                    check(candidate.getX()==3+offsets[n][0] && candidate.getY()==5+offsets[n][1],"kick displacement");
                }
                b.previewRotation(); same(before,b.getCells(),"preview immutable");
                offsets[0][0]=999; check(b.getRotationOffsets()[0][0]==0,"table copy");
                b.rotate();
                if(state==0 && type!=TetrominoType.O)
                    check(occupied(b.getCells()).equals(occupied(right.get(type))),"CW shape "+type);
                check(b.getRotationState()==(type==TetrominoType.O?0:(state+1)%4),"state");
                check(b.getX()==3 && b.getY()==5,"fixed origin");
            }
            same(original,b.getCells(),"four turns "+type);
            same(enumOriginal,type.getCells(),"enum immutable");
            try { b.rotate(99); throw new AssertionError("invalid candidate accepted"); }
            catch(IllegalArgumentException expected) { same(original,b.getCells(),"invalid no mutation"); }
        }
        // All possible attached cells follow the same physical mino, including kick displacement.
        for(var type:TetrominoType.values()) for(var item:ItemType.values()) {
            if(item==ItemType.WEIGHT) continue;
            for(int cell=0;cell<4;cell++) {
                var b=Block.withItem(type,3,5,item,cell);
                for(int turn=0;turn<4;turn++) {
                    int[] old=b.getItemCells()[0];
                    int size=b.getFrameSize(); b.rotate();
                    int[] mark=b.getItemCells()[0];
                    check(type==TetrominoType.O ? Arrays.equals(old,mark)
                        : mark[0]==size-1-old[1] && mark[1]==old[0],"item pivot");
                    check(b.getCellSymbol(mark[0],mark[1])==item.getSymbol(),"item label");
                }
                b.move(-2,3);
                var mark=b.getItemCells()[0]; var absolute=b.getBoardItemCells()[0];
                check(absolute[0]==mark[0]+b.getX() && absolute[1]==mark[1]+b.getY(),"item absolute");
            }
        }
        var weight=Block.weight(3,5); var weightCells=weight.getCells();
        weight.rotate(); same(weightCells,weight.getCells(),"weight unchanged");
        check(weight.width()==4 && weight.height()==2 && weight.getRotationOffsets().length==1,"weight shape");
        var wall=new Block(TetrominoType.T,-1,5); wall.rotate();
        check(!fits(wall.previewRotation()),"wall basic blocked");
        check(tryRotation(wall,SrsCheck::fits) && wall.getX()==0,"wall kick succeeds");
        var floor=new Block(TetrominoType.T,3,18);
        check(!fits(floor.previewRotation()),"floor basic blocked");
        check(tryRotation(floor,SrsCheck::fits) && floor.getY()==17,"floor kick succeeds");
        var blocked=new Block(TetrominoType.I,3,5); var before=blocked.getCells();
        check(!tryRotation(blocked,c->false),"all blocked rejected");
        same(before,blocked.getCells(),"blocked shape unchanged");
        check(blocked.getRotationState()==0 && blocked.getX()==3 && blocked.getY()==5,"blocked state unchanged");
        var f=new ItemBlockFactory(new Random(42));
        try {f.create(false,TetrominoType.T,0,0); throw new AssertionError("mode guard");}
        catch(IllegalStateException expected) { }
        System.out.println("PASS: SRS clockwise shapes, all 8 kick-table rows, all candidates, 4-turn restoration, items, wall/floor kicks, blocked rotation, mode guard.");
    }
}
'@
$sourcePath = Join-Path $checkDirectory 'SrsCheck.java'
[System.IO.File]::WriteAllText($sourcePath, $checkSource, [System.Text.UTF8Encoding]::new($false))
$sources = @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.java' | ForEach-Object FullName)
& (Join-Path $JdkHome 'bin/javac.exe') --release 21 -encoding UTF-8 -d $checkDirectory @sources $sourcePath
if ($LASTEXITCODE -ne 0) { throw 'SRS compilation failed.' }
& (Join-Path $JdkHome 'bin/java.exe') -cp $checkDirectory tetris.block.SrsCheck
if ($LASTEXITCODE -ne 0) { throw 'SRS verification failed.' }
