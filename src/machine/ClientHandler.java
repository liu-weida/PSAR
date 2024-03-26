package machine;

import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;
    private int clientPort; // 假设我们可以从连接或通信协议中获取到客户端ID


    // 使用客户端ID作为键来跟踪对应的线程（或线程池）
    private static ConcurrentHashMap<Integer, ExecutorService> clientExecutors = new ConcurrentHashMap<>();

    public ClientHandler(Socket clientSocket, Server server, int clientPort) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.clientPort = clientPort; // 客户端ID通过构造函数传入或以其它方式获取
    }

    @Override
    public void run() {

        System.out.println(clientExecutors.toString());
        // 根据客户端ID获取或创建ExecutorService
        ExecutorService executor = clientExecutors.computeIfAbsent(clientPort, k -> Executors.newSingleThreadExecutor());
        System.out.println(clientExecutors.toString() + "123");
        System.out.println("testsssssssssssssssss");

        executor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 在这个线程（或线程池）中处理客户端请求...
                    System.out.println("处理来自客户端 " + clientPort + " 的请求");
                    System.out.println(Thread.currentThread().getName());

                    Channel channel = new ChannelBasic(clientSocket);
                    System.out.println(channel.toString() + "  channel");

                    System.out.println("处理客户端请求");
                    Message message = server.processor.process(channel, " ");
                    System.out.println("?????");
                    System.out.println("heap： " + server.getHeap());

                    channel.send(message);
                    // 处理完成后，不立即关闭线程池，以便复用
                } catch (Exception e) {
                    System.out.println("处理客户端请求时出错: " + e.getMessage());
                } finally {
                    try {
                        clientSocket.close();
                        // 注意：我们不在这里移除和关闭线程池，以支持线程复用
                    } catch (IOException e) {
                        System.out.println("关闭客户端连接时出错: " + e.getMessage());
                    }
                }
            }
        });
    }

    // 可能还需要一个方法来在适当的时候清理不再使用的线程池（比如客户端长时间未连接）
}
