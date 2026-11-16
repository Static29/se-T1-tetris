package tetris.network;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleRoomsTest {
    @Test void twoDuelsRouteOnlyWithinTheirOwnRoom() {
        try (var rooms = new BattleRooms()) {
            var a = connect(rooms, "a");
            var b = connect(rooms, "b");
            var c = connect(rooms, "c");
            var d = connect(rooms, "d");
            create(rooms, "a", 2);
            create(rooms, "c", 2);
            send(rooms, "b", "JOIN_ROOM", field(a, "ROOM_JOINED", 0));
            send(rooms, "d", "JOIN_ROOM", field(c, "ROOM_JOINED", 0));
            ready(rooms, "a", "b", "c", "d");
            String first = field(a, "ROOM_START", 0);
            String second = field(c, "ROOM_START", 0);
            assertNotEquals(first, second);

            send(rooms, "a", "ATTACK", first, "0", "1111001111/1111001111");
            assertEquals("a", field(b, "ATTACK", 1));
            assertFalse(has(c, "ATTACK"));
            assertFalse(has(d, "ATTACK"));
            rooms.disconnect("a");
            assertEquals("b", field(b, "RESULT", 1));
            assertFalse(has(c, "RESULT"));
            send(rooms, "c", "STATE", second, "0", "frame");
            assertEquals("frame", field(d, "STATE", 3));
        }
    }

    @Test void fourPlayerMatchSurvivesHostDisconnectAndSkipsFullAndEliminatedTargets() {
        try (var rooms = new BattleRooms()) {
            var host = connect(rooms, "host");
            var b = connect(rooms, "b");
            var c = connect(rooms, "c");
            var d = connect(rooms, "d");
            create(rooms, "host", 4);
            String room = field(host, "ROOM_JOINED", 0);
            for (String id : List.of("b", "c")) send(rooms, id, "JOIN_ROOM", room);
            ready(rooms, "host", "b", "c");
            assertFalse(has(b, "ROOM_START"));
            send(rooms, "d", "JOIN_ROOM", room);
            ready(rooms, "d");
            String match = field(b, "ROOM_START", 0);
            rooms.disconnect("host");
            assertFalse(has(b, "RESULT"));
            send(rooms, "c", "GAME_OVER", match);
            send(rooms, "d", "QUEUE_SPACE", match, "false");
            String rows = "1111001111/1111001111";
            send(rooms, "b", "ATTACK", match, "0", rows);
            assertFalse(has(d, "ATTACK"));
            send(rooms, "d", "QUEUE_SPACE", match, "true");
            send(rooms, "b", "ATTACK", match, "0", rows);
            assertFalse(has(d, "ATTACK"));
            send(rooms, "b", "ATTACK", match, "1", rows);
            assertEquals("1", field(d, "ATTACK", 2));
            assertFalse(has(c, "ATTACK"));
            send(rooms, "b", "STATE", match, "0", "frame");
            assertEquals("frame", field(c, "STATE", 3));
            send(rooms, "d", "GAME_OVER", match);
            assertEquals("b", field(c, "RESULT", 1));
        }
    }

    private static List<NetworkMessage> connect(BattleRooms rooms, String id) {
        var messages = new ArrayList<NetworkMessage>();
        rooms.connect(id, messages::add);
        return messages;
    }
    private static void create(BattleRooms rooms, String id, int size) {
        send(rooms, id, "CREATE_ROOM", Integer.toString(size), "DUEL", "0", "true", "NORMAL");
    }
    private static void ready(BattleRooms rooms, String... ids) {
        for (String id : ids) send(rooms, id, "READY", "true");
    }
    private static void send(BattleRooms rooms, String id, String type, String... fields) {
        rooms.receive(id, NetworkMessage.of(type, fields));
    }
    private static boolean has(List<NetworkMessage> messages, String type) {
        return messages.stream().anyMatch(message -> message.type().equals(type));
    }
    private static String field(List<NetworkMessage> messages, String type, int index) {
        return messages.stream().filter(message -> message.type().equals(type))
                .findFirst().orElseThrow().fields().get(index);
    }
}
