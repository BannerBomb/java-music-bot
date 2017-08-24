package ovh.not.javamusicbot;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

public class Cluster implements Runnable {
    // this is the same buffer sized used in bando
    private static final int INPUT_BUFFER_SIZE = 1024;

    private final Gson gson = new Gson();
    private final Config config;

    private boolean running = true;

    // reconnection states
    private boolean reconnecting = false;
    private final long initialReconnectPause = 2000;
    private long reconnectPause = initialReconnectPause;

    Cluster(Config config) {
        this.config = config;
    }

    @Override
    public void run() {
        Socket socket = null;

        while (running) {
            try {
                socket = new Socket(config.bandoAddress, config.bandoPort);

                System.out.printf("Connection to bando on %s:%d established!\n", config.bandoAddress, config.bandoPort);
                if (reconnecting) {
                    reconnecting = false;
                    reconnectPause = initialReconnectPause;
                }

                // open output stream
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // send identify packet (opcode 0)
                String json = gson.toJson(new Message(0, new IdentifyMessage(config.bandoKey, 0, 3)));
                out.write(json);
                out.flush();

                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()), INPUT_BUFFER_SIZE)) {
                    String content;
                    while ((content = in.readLine()) != null) {
                        Message message = gson.fromJson(content, Message.class);

                        switch (Opcode.fromId(message.op)) {
                            case AUTHENTICATED:
                                // todo logging
                                System.out.println("Connected to bando!");
                                break;
                            case AUTHENTICATION_REJECTED:
                                // todo logging
                                System.out.println("Invalid RPC key! Running cluster without RPC...");
                                return; // exit the method
                            case UNKNOWN_OPCODE:
                            default:
                                System.out.printf("Received message with unknown opcode %d\n", message.op);
                        }
                    }
                } finally {
                    // close the output stream. the input stream should be auto closed
                    out.close();
                }
            } catch (IOException e) {
                if (e instanceof ConnectException && e.getMessage().equals("Connection refused: connect")) {
                    // ignored
                } else if (e instanceof SocketException && e.getMessage().equals("Connection reset")) {
                    // ignored
                }

                // todo logging and more checks

                else {
                    e.printStackTrace();
                }
            } finally {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // todo error handling
                        e.printStackTrace();

                        // break out of the reconnect loop
                        running = false;
                    }
                }
            }

            // if still running wait before reconnecting
            if (running) {
                reconnecting = true;

                // todo logging
                System.out.println(String.format("Attempting to reconnect in %dms...", reconnectPause));

                try {
                    Thread.sleep(reconnectPause);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // set the next pause to be twice as long as the last
                reconnectPause += reconnectPause;
            }
        }
    }

    enum Opcode {
        IDENTIFY(0),
        AUTHENTICATED(1),
        AUTHENTICATION_REJECTED(2),
        SUMMONS(3),
        STATUS_REQUEST(6),
        STATUS_RESPONSE(7),
        STATUS_ANSWER(8),
        RESTART_ALL_SHARDS(50),
        RESTART_SUCCESS(51),
        RESTART_FAILURE(52),
        ROLLING_RESTART(53),
        INVALID_REQUEST(99),
        UNKNOWN_OPCODE(-1);

        int id;

        Opcode(int id) {
            this.id = id;
        }

        static Opcode fromId(int id) {
            for (Opcode opcode : Opcode.values()) {
                if (opcode.id == id) {
                    return opcode;
                }
            }

            return Opcode.UNKNOWN_OPCODE;
        }
    }

    class Message {
        int op;
        Object data;

        Message(int op, Object data) {
            this.op = op;
            this.data = data;
        }
    }

    class IdentifyMessage {
        String key;
        int min, max;

        IdentifyMessage(String key, int min, int max) {
            this.key = key;
            this.min = min;
            this.max = max;
        }
    }
}
