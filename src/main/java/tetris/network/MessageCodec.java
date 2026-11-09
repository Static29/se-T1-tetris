package tetris.network;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** TCP의 분할/병합 수신에 대응하는 길이 기반 UTF-8 프레임. Java 객체 역직렬화는 사용하지 않는다. */
public final class MessageCodec {
    public static final int MAX_BYTES = 65536;
    private MessageCodec() { }
    public static byte[] encode(NetworkMessage message) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var body = new DataOutputStream(bytes);
        body.writeInt(1);
        writeString(body, message.type());
        body.writeInt(message.fields().size());
        for (String field : message.fields()) writeString(body, field);
        if (bytes.size() > MAX_BYTES) throw new IOException("Frame too large");
        var frame = new ByteArrayOutputStream();
        var out = new DataOutputStream(frame);
        out.writeInt(bytes.size()); bytes.writeTo(out);
        return frame.toByteArray();
    }
    public static NetworkMessage read(InputStream input) throws IOException {
        var in = new DataInputStream(input);
        int size = in.readInt();
        if (size < 12 || size > MAX_BYTES) throw new IOException("Invalid frame size");
        byte[] bytes = new byte[size]; in.readFully(bytes);
        var body = new DataInputStream(new ByteArrayInputStream(bytes));
        if (body.readInt() != 1) throw new IOException("Unsupported version");
        String type = readString(body);
        int count = body.readInt();
        if (count < 0 || count > 16) throw new IOException("Invalid field count");
        var fields = new ArrayList<String>();
        for (int i = 0; i < count; i++) fields.add(readString(body));
        if (body.available() != 0) throw new IOException("Trailing data");
        try { return new NetworkMessage(type, fields); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid message", e); }
    }
    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("Field too large");
        out.writeInt(bytes.length); out.write(bytes);
    }
    private static String readString(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size < 0 || size > in.available()) throw new IOException("Invalid string size");
        byte[] bytes = new byte[size]; in.readFully(bytes);
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }
}
