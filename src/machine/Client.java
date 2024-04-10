package machine;

import annotations.CommandMethod;
import utils.channel.ChannelBasic;
import utils.message.*;
import utils.channel.Channel;
import utils.processor.ClientProcessor;
import utils.tools.CountdownTimer;
import utils.tools.Pair;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client extends Machine{
    private HashMap<String, Object> localHeap = new HashMap<>();
    private ClientProcessor processor = new ClientProcessor();
    private Channel channel;
    private final int serverPort = 8080; // 服务器端口
    private final String serverHost = "localhost"; // 服务器地址

    int localPort = -1;

    public Client(int port, String clientId) throws IOException {
        super(clientId, port);
        System.out.println("构造函数开始执行");
        this.channel = createChannel();
        processor.setCLient(this);
        listenForClientMessages();
        heartBeat();
        System.out.println("构造函数执行完毕");
    }


    private void heartBeat() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {

            InetAddress localHost = channel.getLocalHost();
            int localPort = super.getPort();

            HeartbeatMessage heartbeatMessage = new HeartbeatMessage(HeartbeatMessage.Source.CLIENT,OperationStatus.HEART,localHost,localPort);

            try {
                channel.send(heartbeatMessage);
                System.out.println("客户端心跳已发送");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }, 20, 20, TimeUnit.SECONDS);
    }

    private Channel createChannel() throws IOException {  //第一次端口随机选择，第二次端口选择第一次的
        Socket socket = new Socket();
        socket.setReuseAddress(true);
        if (localPort == -1) {
            socket.connect(new InetSocketAddress(serverHost, serverPort));
            localPort = socket.getLocalPort();
        } else {
                socket.bind(new InetSocketAddress((InetAddress)null, this.localPort));
                socket.connect(new InetSocketAddress(serverHost, serverPort));
        }
        return new ChannelBasic(socket);
    }

    public void reconnectToServer() {
        try {
            System.out.println("检测到连接中断，尝试重连");
            channel.close();
            CountdownTimer timer = new CountdownTimer(5);  // 创建一个5秒的倒计时
            //timer.start();
            this.channel = createChannel(); // 重新建立连接
            System.out.println("重连成功！");
        } catch (IOException e) {
            System.out.println("无法连接到服务器: " + e.getMessage());
        }
    }

    public boolean heapHaveData(String variableId){
        return localHeap.containsKey(variableId);
    }

    public HashMap<String, Object> getLocalHeap(){
        return localHeap;
    }

    public void setObject(String variableId, Object o){
        localHeap.put(variableId, o);
    }

    public boolean modifyHeap(String key, Object value) {
        localHeap.put(key, value);
        return true;
    }

    public void request(String methodType, String args) throws InvocationTargetException, IllegalAccessException {
        for (Method method: getClass().getDeclaredMethods()){
            if (method.getName().equals(methodType) && method.isAnnotationPresent(CommandMethod.class)){
                method.invoke(this, args);
                break;
            }
        }
    }

    public void respond() throws IOException, ClassNotFoundException {

    }

//    @Override
//    // public boolean modifyHeap(String methodType, String key, String value){
//        return false;
//    }


    public Channel connectToClient(InetAddress host, int port) throws IOException {
        return new ChannelBasic(new Socket(host, port));
    }

    public void listenForClientMessages() {
        new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {

                    System.out.println("lfcm已启动");

                    Channel localChannel = new ChannelBasic(super.getServerSocket().accept());

                    System.out.println("read开始~");

                    SendDataMessage recv = (SendDataMessage) localChannel.recv();

                    System.out.println("lfcm收到的消息：" + recv.toString());

                    Object result = localHeap.get(recv.getVariableId());

                    localChannel.send(new SendDataMessage(recv.getVariableId(), result));
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }


    //向服务器发送消息，查看该数据是否存在，如果收到不存在消息，在自己的堆里加入这个数据
    @CommandMethod
    private void dMalloc(String id) throws IOException, SecurityException, IllegalArgumentException, ClassNotFoundException {
        ClientMessage message = new ClientMessage("dMalloc", getId(), id, super.getPort());
        // Channel channel = new ChannelBasic(new Socket("localhost", 8080));

        try {
            channel.send(message);
        }catch (IOException e){
            reconnectToServer();
        }

        processor.process(channel, id);
    }

    //向服务器发送写入请求，(如果存在这个数据并且数据未上锁)收到确认消息，返回自己堆中该数据的地址位置，如果收到报错信息，返回null
    @CommandMethod
    private int dAccessWrite(String id) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dAccessWrite", getId(), id, super.getPort());
//        Channel channel = new ChannelBasic(new Socket("localhost", 8080));
        try {
            channel.send(message);
        }catch (IOException e){
            reconnectToServer();
        }
        processor.process(channel, id);
        return 1;
    }

    //向服务器发送读取请求，(如果存在这个数据并且数据未上锁)收到确认消息，根据返回的信息判断是否直接读取自己的数据，或向另一个客户端传输读取请求，如果读取出错，向服务器发送错误消息，读取成功修改自己的堆返回地址
    @CommandMethod
    private int dAccessRead(String id) throws  IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dAccessRead", getId(), id, super.getPort());
        //Channel channel = new ChannelBasic(new Socket("localhost", 8080));
        try {
            channel.send(message);
        }catch (IOException e){
            reconnectToServer();
        }
        System.out.println("read message 发送： "+message.toString());
        processor.process(channel, id);
        return 1;
    }

    //回复修改确认消息，(将数据设置为不可修改?)
    @CommandMethod
    private void dRelease(String variableId) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dRelease", variableId, super.getPort());
        // Channel channel = new ChannelBasic(new Socket("localhost",8080));
        try {
            channel.send(message);
        }catch (IOException e){
            reconnectToServer();
        }
        processor.process(channel, variableId);

    }

    //发出删除信号消息，等待回信
    @CommandMethod
    private void dFree(String id) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dFree",getId(), id, super.getPort());
        // Channel channel = new ChannelBasic(new Socket("localhost", 8080));
        try {
            channel.send(message);
        }catch (IOException e){
            reconnectToServer();
        }
        processor.process(channel, id);
    }


}