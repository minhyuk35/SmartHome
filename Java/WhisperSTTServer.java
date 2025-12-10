import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class WhisperSTTServer {

    private int port;
    private WhisperListener listener;

    public WhisperSTTServer(int port) {
        this.port = port;
    }

    public interface WhisperListener {
        void onTranscription(String text);
    }

    public void addWhisperListener(WhisperListener l) {
        this.listener = l;
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[JAVA] Whisper STT 서버 시작, 포트: " + port);

                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("[JAVA] Whisper 클라이언트 연결: " + client.getInetAddress());

                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream())
                    );

                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("[JAVA] Whisper 수신: " + line);
                        if (listener != null) {
                            listener.onTranscription(line);
                        }
                    }

                    client.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
