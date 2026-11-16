package tetris.network;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.text.Normalizer;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** File owned by a single server process. Each update replaces the complete account snapshot. */
final class AccountStore {
    private static final int ITERATIONS = 600_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Path file;
    private final Properties accounts = new Properties();
    private final byte[] dummySalt = new byte[16];

    AccountStore(Path file) throws IOException {
        this.file = file.toAbsolutePath();
        RANDOM.nextBytes(dummySalt);
        if (Files.notExists(this.file)) return;
        try (var input = Files.newInputStream(this.file)) {
            accounts.load(input);
        }
        for (String id : accounts.stringPropertyNames()) {
            if (!id.equals(loginId(id))) throw new IOException("Invalid account ID in store");
            read(id);
        }
    }

    static String loginId(String input) {
        if (!input.matches("[A-Za-z0-9_]{4,20}")) throw new IllegalArgumentException("INVALID_LOGIN_ID");
        return input.toLowerCase(Locale.ROOT);
    }

    synchronized void register(String id, String display, String password) throws IOException {
        String name = Normalizer.normalize(display.strip(), Normalizer.Form.NFKC);
        if (!name.matches("[\\p{L}\\p{N}]{2,12}")) throw new IllegalArgumentException("INVALID_DISPLAY_NAME");
        if (accounts.containsKey(id)) throw new IllegalArgumentException("LOGIN_ID_TAKEN");
        if (accounts.size() >= 256) throw new IllegalArgumentException("ACCOUNT_LIMIT");
        for (String existing : accounts.stringPropertyNames()) {
            if (name.equalsIgnoreCase(read(existing).name())) throw new IllegalArgumentException("DISPLAY_NAME_TAKEN");
        }
        update(id, name, password);
    }

    synchronized String authenticate(String id, String password) {
        Account account = read(id);
        if (password.isEmpty() || password.length() > 128) throw new IllegalArgumentException("INVALID_CREDENTIALS");
        byte[] candidate = hash(password, account == null ? dummySalt : account.salt());
        if (account == null || !MessageDigest.isEqual(account.hash(), candidate)) {
            throw new IllegalArgumentException("INVALID_CREDENTIALS");
        }
        return account.name();
    }

    synchronized void change(String id, String current, String replacement) throws IOException {
        update(id, authenticate(id, current), replacement);
    }

    private Account read(String id) {
        String value = accounts.getProperty(id);
        if (value == null) return null;
        String[] fields = value.split(":", -1);
        if (fields.length != 4 || !fields[0].equals("1")) throw new IllegalArgumentException("INVALID_ACCOUNT_STORE");
        byte[] salt = Base64.getDecoder().decode(fields[2]);
        byte[] hash = Base64.getDecoder().decode(fields[3]);
        if (salt.length != 16 || hash.length != 32) throw new IllegalArgumentException("INVALID_ACCOUNT_STORE");
        return new Account(fields[1], salt, hash);
    }

    private void update(String id, String name, String password) throws IOException {
        if (password.length() < 8 || password.length() > 128 || password.isBlank()) {
            throw new IllegalArgumentException("INVALID_PASSWORD_LENGTH");
        }
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        var encoder = Base64.getEncoder();
        String value = "1:" + name + ":" + encoder.encodeToString(salt) + ":" + encoder.encodeToString(hash(password, salt));
        var updated = new Properties();
        updated.putAll(accounts);
        updated.setProperty(id, value);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "tetris-accounts-", ".tmp");
        try {
            try (var output = Files.newOutputStream(temporary)) {
                updated.store(output, "Tetris accounts");
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            accounts.setProperty(id, value);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static byte[] hash(String password, byte[] salt) {
        char[] chars = password.toCharArray();
        var spec = new PBEKeySpec(chars, salt, ITERATIONS, 256);
        Arrays.fill(chars, '\0');
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("PBKDF2 unavailable", error);
        } finally {
            spec.clearPassword();
        }
    }
    private record Account(String name, byte[] salt, byte[] hash) {}
}
