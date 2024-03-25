package test;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.channel.ChannelBasic;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ChannelBasicTest {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Socket serverSideSocket;
    private ChannelBasic clientChannel;
    private ChannelBasic serverChannel;

    @Before
    public void setUp() throws IOException {
        // 设置测试用的服务器和客户端socket连接
        int testPort = 8080;
        serverSocket = new ServerSocket(testPort);
        new Thread(() -> {
            try {
                serverSideSocket = serverSocket.accept();
                serverChannel = new ChannelBasic(serverSideSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        clientSocket = new Socket("localhost", testPort);
        clientChannel = new ChannelBasic(clientSocket);

        // 等待连接建立
        try {
            Thread.sleep(1000); // 等待足够的时间来确保socket连接已建立
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSendAndRecv() throws IOException, ClassNotFoundException {
        String testMessage = "Hello, Channel!";
        clientChannel.send(testMessage); // 客户端发送消息
        Object receivedMessage = serverChannel.recv(); // 服务器接收消息

        assertEquals(testMessage, receivedMessage);
    }

    @Test
    public void testGetRemoteHost() {
        assertEquals(clientSocket.getInetAddress(), clientChannel.getRemoteHost());
        assertEquals(serverSideSocket.getInetAddress(), serverChannel.getRemoteHost());
    }

    @Test
    public void testGetRemotePort() {
        assertEquals(clientSocket.getPort(), clientChannel.getRemotePort());
    }

    @Test
    public void testGetLocalHost() {
        assertEquals(clientSocket.getLocalAddress(), clientChannel.getLocalHost());
        assertEquals(serverSideSocket.getLocalAddress(), serverChannel.getLocalHost());
    }

    @Test
    public void testGetLocalPort() {
        assertTrue(clientChannel.getLocalPort() > 0);
        assertTrue(serverChannel.getLocalPort() > 0);
    }

    @After
    public void tearDown() throws IOException {
        clientSocket.close();
        serverSideSocket.close();
        serverSocket.close();
    }
}
