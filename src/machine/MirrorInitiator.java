package machine;

import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.HeartbeatMessage;
import utils.message.MessageType;
import utils.message.OperationStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class MirrorInitiator extends Machine {

    private Channel channel;
    private boolean continueRunning = true;
    private boolean isOverCalled = false; // 避免over方法重复调用

    private long serverPid;

    public MirrorInitiator(String id, int port) throws IOException {
        super(id, port);
        this.channel = new ChannelBasic(new Socket("localhost", 8080));
        startHeartbeat();
    }

    private void startHeartbeat() {
        new Thread(() -> {
            while (continueRunning) {
                try {
                    heartbeatSend();
                    heartbeatRecv();
                    Thread.sleep(300);// 0.3s  后期得改成30
                } catch (Exception e) {
                    handleError(e);
                }
            }
        }).start();
    }

    private void handleError(Exception e) {
        if (!isOverCalled) {
            System.out.println("An error occurred: " + e.getMessage());
            try {
                reconnect();
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("阿巴阿巴");
            }
            isOverCalled = true; // 标记已调用
        }
    }

    private void heartbeatSend() throws IOException, ClassNotFoundException {
        HeartbeatMessage hbm = new HeartbeatMessage(MessageType.HBM, OperationStatus.HEART);
        channel.send(hbm);
    }

    private void heartbeatRecv() throws IOException, ClassNotFoundException {
        HeartbeatMessage heartbeatMessage = null;

        try {
            heartbeatMessage = (HeartbeatMessage) channel.recvWithTimeout(5000);  //timeout为5秒
        }catch (SocketTimeoutException e){
            handleError(e);
        }

        if (heartbeatMessage == null) {
            throw new SocketException("No response received from server.");
        }
        System.out.println(heartbeatMessage.toString());
        System.out.println(heartbeatMessage.getServerPid() + " pid 123");
        serverPid = heartbeatMessage.getServerPid();
    }

    public void reconnect() throws IOException, ClassNotFoundException {
        if (!continueRunning) {    // 如果已经处理过，直接返回
            return;
        }

        System.out.println("心跳连接失效/socket连接断开！！！");
        continueRunning = true;
        killServer((int)serverPid);

        System.out.println("启动镜像服务器");
        restartServer(8080,"server");

    }

    public static void killServer(int pid) {    //关闭服务器
        String os = System.getProperty("os.name").toLowerCase();
        String command;

        try {
            if (os.contains("win")) {
                command = "taskkill /F /PID " + pid;   // Windows
            } else {
                command = "kill -9 " + pid;    // Unix/Linux
            }

            Process proc = Runtime.getRuntime().exec(command);

            // 读取命令的输出信息
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            proc.waitFor(); // 等待命令执行完成

            int exitVal = proc.exitValue();
            if (exitVal == 0) {
                System.out.println("Process (PID: " + pid + ") terminated successfully.");
            } else {
                System.out.println("Process (PID: " + pid + ") could not be terminated.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void restartServer(int port, String serverName) throws IOException, ClassNotFoundException {
        Server server = new Server(port, serverName);
        server.start();
    }


}
