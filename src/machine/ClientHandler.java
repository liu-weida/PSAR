package machine;

import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;

    public ClientHandler(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            Channel channel = new ChannelBasic(clientSocket);
            System.out.println("处理客户端请求");
            Message message = server.processor.process(channel, " ");

            System.out.println("heap： " + server.getHeap());

            channel.send(message);
        } catch (IOException e) {
            System.out.println("处理客户端请求时出错: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("关闭客户端连接时出错: " + e.getMessage());
            }
        }
    }
}
