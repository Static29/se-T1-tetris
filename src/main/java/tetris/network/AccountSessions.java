package tetris.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Owns authenticated connections. Credential work runs outside the room lock. */
final class AccountSessions {
    private final AccountStore store;
    private final BattleRooms rooms;
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, String> loggedIn = new HashMap<>();
    private final Semaphore credentialWork = new Semaphore(1);

    AccountSessions(AccountStore store, BattleRooms rooms) {
        this.store = store;
        this.rooms = rooms;
    }

    synchronized void connect(String id, Consumer<NetworkMessage> output) {
        sessions.put(id, new Session(output));
        output.accept(NetworkMessage.of("AUTH_REQUIRED"));
    }

    void receive(String id, NetworkMessage message) {
        Session session;
        synchronized (this) {
            session = sessions.get(id);
            if (session == null) return;
        }
        var fields = message.fields();
        try {
            switch (message.type()) {
                case "REGISTER", "LOGIN", "CHANGE_PASSWORD" -> {
                    long now = System.nanoTime();
                    if (session.attempted && now - session.lastAttempt < TimeUnit.SECONDS.toNanos(1)) {
                        throw new IllegalArgumentException("AUTH_RETRY_LATER");
                    }
                    session.attempted = true;
                    session.lastAttempt = now;
                    if (!credentialWork.tryAcquire()) throw new IllegalArgumentException("AUTH_RETRY_LATER");
                    try {
                        switch (message.type()) {
                            case "REGISTER" -> {
                                require(session.login == null && fields.size() == 3, "INVALID_REGISTER");
                                store.register(AccountStore.loginId(fields.get(0)), fields.get(1), fields.get(2));
                                session.send("REGISTERED");
                            }
                            case "LOGIN" -> {
                                require(session.login == null && fields.size() == 2, "INVALID_LOGIN");
                                String login = AccountStore.loginId(fields.get(0));
                                String name = store.authenticate(login, fields.get(1));
                                login(id, session, login, name);
                            }
                            case "CHANGE_PASSWORD" -> {
                                require(session.login != null && fields.size() == 2, "INVALID_CHANGE_PASSWORD");
                                store.change(session.login, fields.get(0), fields.get(1));
                                session.send("PASSWORD_CHANGED");
                            }
                        }
                    } finally {
                        credentialWork.release();
                    }
                }
                default -> {
                    require(session.login != null, "LOGIN_REQUIRED");
                    rooms.receive(id, message);
                }
            }
        } catch (IllegalArgumentException error) {
            session.send("ERROR", error.getMessage());
        } catch (IOException error) {
            session.send("ERROR", "ACCOUNT_STORAGE_ERROR");
        }
    }

    private synchronized void login(String id, Session session, String login, String name) {
        if (sessions.get(id) != session) return;
        require(!loggedIn.containsKey(login), "ALREADY_LOGGED_IN");
        session.login = login;
        loggedIn.put(login, id);
        session.send("LOGGED_IN", id, name);
        rooms.connect(id, session.output, name);
    }

    synchronized void disconnect(String id) {
        Session session = sessions.remove(id);
        if (session != null && session.login != null) {
            loggedIn.remove(session.login, id);
            rooms.disconnect(id);
        }
    }

    private static void require(boolean condition, String error) {
        if (!condition) throw new IllegalArgumentException(error);
    }

    private static final class Session {
        final Consumer<NetworkMessage> output;
        volatile String login;
        boolean attempted;
        long lastAttempt;

        Session(Consumer<NetworkMessage> output) { this.output = output; }

        void send(String type, String... fields) {
            output.accept(NetworkMessage.of(type, fields));
        }
    }
}
