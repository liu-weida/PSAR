package machine;

import annotations.CommandMethod;
import utils.channel.ChannelBasic;
import utils.message.ClientMessage;
import utils.channel.Channel;
import utils.message.Message;
import utils.message.ServerMessage;
import utils.processor.ClientProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.net.ServerSocket;
import java.util.HashMap;

public class Client implements Machine{
    private final int port;
    private final String clientId;
    private Channel channel;
    private HashMap<String, Object> localHeap = new HashMap<>();//数据储存在这里
    private ServerSocket serverSocket;

    // ServerSocket serverSocket;
    public Client(int port, String clientId, Channel channel) throws IOException {
        this.port = port;
        this.clientId = clientId;
        this.channel = channel;
        serverSocket = new ServerSocket(port);
        System.out.println("client create success: port = "+ port + " clientID = "+ clientId);
//        try{
//            this.serverSocket = new ServerSocket(port);
//        }catch (IOException e){
//            e.printStackTrace();
//        }
    }

    public String getId() {
        return clientId;
    }

    public Channel getChannel() {
        return channel;
    }

    public HashMap<String, Object> getLocalHeap(){
        return localHeap;
    }

    public Object getObject(String variableId){
        return localHeap.get(variableId);
    }

    public void setObject(String variableId,Object o){
        localHeap.put(variableId,o);
    }

    public void setChannel(Channel channel){
        this.channel = channel;
    }

    public boolean compareClassObject(String variableId,Class<?> clazz){
        return getObject(variableId).getClass() == clazz;
    }
    @Override
    public void request(String methodType, String args) throws InvocationTargetException, IllegalAccessException {
        for (Method method: getClass().getMethods()){
            if (method.getName().equals(methodType) && method.isAnnotationPresent(CommandMethod.class)){
                method.invoke(args);
                break;
            }
        }
    }

    @Override
    public void respond() throws IOException, ClassNotFoundException {
        //这里放收到服务器消息之后的处理
        ClientProcessor clientProcessor = new ClientProcessor();
        clientProcessor.process(channel);
    }


    public void connectToClient(InetAddress host, int port, String variableId) {
        try {
            // 创建一个新的Socket连接到指定的主机和端口
            Channel channel = new ChannelBasic(new Socket(host, port));

            // 发送消息
            ClientMessage SendMessage = new ClientMessage("wantValue", getId(), variableId, this.port);
            channel.send(SendMessage);

            // 接收响应
            ServerMessage receivedMessage = (ServerMessage) channel.recv();
            // 处理接收到的消息...
            System.out.println(receivedMessage);
            // 关闭连接
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void listenForClientMessages() {
        new Thread(() -> {
            try {
                while (! Thread.currentThread().isInterrupted()) {
                    // 接受一个连接
                    Channel channelClients = new ChannelBasic(serverSocket.accept());
                    ClientMessage receivedMessage = (ClientMessage) channelClients.recv();
                    if (receivedMessage.getCommand().equals("wantValue")){
                        channel.send(new ClientMessage("sendValue", clientId, receivedMessage.getVariableId(), this.port));
                    }
                                // 处理接收到的消息...
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }

    //向服务器发送消息，查看该数据是否存在，如果收到不存在消息，在自己的堆里加入这个数据
    @CommandMethod
    private void dMalloc(String id) throws IOException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ClientMessage message = new ClientMessage("dMalloc", getId(), id, port);
        channel.send(message);
    }

    //向服务器发送写入请求，(如果存在这个数据并且数据未上锁)收到确认消息，返回自己堆中该数据的地址位置，如果收到报错信息，返回null
    @CommandMethod
    private int dAccessWrite(String id) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dAccessWrite", getId(), id, port);
        channel.send(message);
//        ServerMessage serverMessage = (ServerMessage) channel.recv();
//        if (serverMessage.getSuccesses()){
//            System.out.println("ok");
//        } else {
//            System.out.println("not ok");
//        }
        return 1;
    }

    //向服务器发送读取请求，(如果存在这个数据并且数据未上锁)收到确认消息，根据返回的信息判断是否直接读取自己的数据，或向另一个客户端传输读取请求，如果读取出错，向服务器发送错误消息，读取成功修改自己的堆返回地址
    @CommandMethod
    private int dAccessRead(String variableId) throws  IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dAccessRead", getId(), variableId, port);
        channel.send(message);
//        ServerMessage serverMessage = (ServerMessage) channel.recv();
//        if(serverMessage.getSuccesses()){
//            System.out.println("ok");
//        } else {
//            System.out.println("not ok");
//        }
        return 1;
    }

    //回复修改确认消息，(将数据设置为不可修改?)
    @CommandMethod
    private void dRelease(String variableId) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dRelease", variableId, port);
        channel.send(message);
        ServerMessage serverMessage = (ServerMessage) channel.recv();
    }

    //发出删除信号消息，等待回信
    @CommandMethod
    private void dFree(String id) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dFree",getId(), id, port);
        channel.send(message);
        ServerMessage serverMessage = (ServerMessage) channel.recv();
    }

}
