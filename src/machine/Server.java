package machine;

import utils.exception.ServerException;
import utils.message.Message;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class Server implements Machine{
    private final int port;
    private final String id;
    private final ServerProcessor processor;

    public Server(int port, String id, ServerProcessor processor){
        this.port = port;
        this.id = id;
        this.processor = processor;
    }

    public void start(){
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            int i = 0;
            while (! Thread.currentThread().isInterrupted()) {
                try (Socket client = ss.accept()) {
                    System.out.println("Debut de requête " + i);
                    processor.process(client);
                }
                System.out.println("Fin de requête " + i);
                System.out.println("**********************\n");
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public int getPort() {
        return port;
    }

    @Override
    public void request(String methodType, List<Object> args) {

    }

    @Override
    public void respond() {

    }
}
