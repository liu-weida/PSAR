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
                    Thread.sleep(300); // 0.3s  后期得改成30
                    heartbeatRecv();
                    Thread.sleep(300);
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
                over();
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
        HeartbeatMessage heartbeatMessage = (HeartbeatMessage) channel.recv();
        if (heartbeatMessage == null) {
            throw new SocketException("No response received from server.");
        }
        System.out.println(heartbeatMessage.toString());
        System.out.println(heartbeatMessage.getServerPid() + " pid 123");
        serverPid = heartbeatMessage.getServerPid();
    }

    public void over() throws IOException, ClassNotFoundException {
        if (!continueRunning) return; // 如果已经处理过，直接返回
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
                // Windows命令
                command = "taskkill /F /PID " + pid;
            } else {
                // Unix/Linux命令
                command = "kill -9 " + pid;
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

//    public static void restartServer(int port, String serverName) {
//        try {
//            String javaHome = System.getProperty("java.home");
//            String javaBin = javaHome + "/bin/java";
//            String classpath = System.getProperty("java.class.path");
//            String mainClass = "test.StartServer"; // 这里替换成Server类的完全限定名
//
//            ProcessBuilder processBuilder = new ProcessBuilder(
//                    javaBin, "-cp", classpath, mainClass, String.valueOf(port), serverName);
//            processBuilder.redirectErrorStream(true); // 合并标准输出和错误输出
//
//            Process process = processBuilder.start(); // 启动新的JVM进程
//
//            // 输出新进程的输出信息
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println("Server Output: " + line);
//            }
//
//            int exitCode = process.waitFor(); // 等待新进程结束
//            System.out.println("Server exited with code " + exitCode);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }



    public static void main(String[] args) throws IOException {
        new MirrorInitiator("mirrorinitiator", 1010);
    }
}
