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
import java.net.Socket;
import java.util.List;
import java.net.ServerSocket;
import java.util.HashMap;

public class Client implements Machine{
    private final int port;
    private final String clientId;
    private Channel channel;
    private HashMap<String, Object> localHeap;//数据储存在这里

    // ServerSocket serverSocket;
    public Client(int port, String clientId, Channel channel) {
        this.port = port;
        this.clientId = clientId;
        this.channel = channel;
//        try{
//            this.serverSocket = new ServerSocket(port);
//        }catch (IOException e){
//            e.printStackTrace();
//        }
        init_data();
    }

    public void init_data(){
        localHeap = new HashMap<>();
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

    public boolean compareClassObject(String variableId,Class<?> clazz){
        return getObject(variableId).getClass() == clazz;
    }
    @Override
    public void request(String methodType, List<Object> args) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException {
        for (Method method : getClass().getDeclaredMethods()) {
            // 检查方法是否有@CommandMethod注解
            if (method.isAnnotationPresent(CommandMethod.class)) {
                if (methodType.equals(method.getName())) {
                    if (method.getParameterTypes().length == args.size()) {
                        method.setAccessible(true);
                        try {
                            method.invoke(this, args.toArray());
                            return;
                        } catch (IllegalArgumentException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                }
            }
        }
        throw new NoSuchMethodException("Method " + methodType + " with " + args.size() + " parameters not found or not annotated with @CommandMethod.");
    }

    @Override
    public void respond() throws IOException, ClassNotFoundException {
        //这里放收到服务器消息之后的处理
        ClientProcessor clientProcessor = new ClientProcessor();
        clientProcessor.process(channel.getSocket());
    }

//
//    public void connectToClient(String host, int port,String variableId) {
//        try {
//            // 创建一个新的Socket连接到指定的主机和端口
//            Socket socket = new Socket(host, port);
//            Channel channel = new ChannelBasic(socket);
//
//            // 发送消息
//            ClientMessage SendMessage = new ClientMessage("wantValue", getId(), variableId);
//            channel.send(SendMessage);
//
//            // 接收响应
//            ServerMessage receivedMessage = (ServerMessage) channel.recv();
//            // 处理接收到的消息...
//
//            // 关闭连接
//            socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void listenForClientMessages() {
//                new Thread(() -> {
//                    try {
//                            while (true) {
//                                // 接受一个连接
//                                Socket clientSocket = serverSocket.accept();
//
//                                Channel channelClients = new ChannelBasic(clientSocket);
//
//                                try{
//                                    ClientMessage receivedMessage = (ClientMessage) channelClients.recv();
//                                }catch (ClassNotFoundException e){
//                                    e.printStackTrace();
//                                }
//
//                                // 处理接收到的消息...
//
//                                // 关闭这个连接
//                                clientSocket.close();
//                            }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }).start();
//    }

    //向服务器发送消息，查看该数据是否存在，如果收到不存在消息，在自己的堆里加入这个数据
    @CommandMethod
    private void dMalloc(String id) throws IOException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ClientMessage message = new ClientMessage("dMalloc", getId(), id);
        channel.send(message);
    }

    //向服务器发送写入请求，(如果存在这个数据并且数据未上锁)收到确认消息，返回自己堆中该数据的地址位置，如果收到报错信息，返回null
    @CommandMethod
    private int dAccessWrite(String id) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dAccessWrite", getId(), id);
        channel.send(message);
//        ServerMessage serverMessage = (ServerMessage) channel.recv();
//        if (serverMessage.getSuccesses()){
//            System.out.println("ok");
//        } else {
//            System.out.println("not ok");
//        }
        return 1;
    }

    public void setChannel(Channel channel){
        this.channel = channel;
    }

    //向服务器发送读取请求，(如果存在这个数据并且数据未上锁)收到确认消息，根据返回的信息判断是否直接读取自己的数据，或向另一个客户端传输读取请求，如果读取出错，向服务器发送错误消息，读取成功修改自己的堆返回地址
    @CommandMethod
    private int dAccessRead(String variableId) throws  IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dAccessRead", getId(), variableId);
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
        ClientMessage message = new ClientMessage("dRelease", variableId);
        channel.send(message);
        ServerMessage serverMessage = (ServerMessage) channel.recv();
    }

    //发出删除信号消息，等待回信
    @CommandMethod
    private void dFree(String id) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dFree",getId(), id);
        channel.send(message);
        ServerMessage serverMessage = (ServerMessage) channel.recv();
    }

}
