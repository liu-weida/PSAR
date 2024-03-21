package machine;

import annotations.CommandMethod;
import utils.channel.ChannelBasic;
import utils.message.ClientMessage;
import utils.channel.Channel;
import utils.message.SendDataMessage;
import utils.processor.ClientProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

public class Client extends Machine{
    private HashMap<String, Object> localHeap = new HashMap<>();//数据储存在这里


    private ClientProcessor processor = new ClientProcessor();

    public Client(int port, String clientId) throws IOException {
        super(clientId, port);
        processor.setCLient(this);
        listenForClientMessages();
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
                while (! Thread.currentThread().isInterrupted()) {
                    // 接受一个连接
                    Channel localChannel = new ChannelBasic(super.getServerSocket().accept());
                    SendDataMessage recv = (SendDataMessage) localChannel.recv();

                    localChannel.send(new SendDataMessage(recv.getVariableId(), localHeap.get("test")));
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
        Channel channel = new ChannelBasic(new Socket("localhost", 8080));
        channel.send(message);
        //processor.process(channel, id);
    }

    //向服务器发送写入请求，(如果存在这个数据并且数据未上锁)收到确认消息，返回自己堆中该数据的地址位置，如果收到报错信息，返回null
    @CommandMethod
        private int dAccessWrite(String id) throws IOException, ClassNotFoundException{
            ClientMessage message = new ClientMessage("dAccessWrite", getId(), id, super.getPort());
            Channel channel = new ChannelBasic(new Socket("localhost", 8080));
            channel.send(message);
            processor.process(channel, id);
            return 1;
    }

    //向服务器发送读取请求，(如果存在这个数据并且数据未上锁)收到确认消息，根据返回的信息判断是否直接读取自己的数据，或向另一个客户端传输读取请求，如果读取出错，向服务器发送错误消息，读取成功修改自己的堆返回地址
    @CommandMethod
    private int dAccessRead(String id) throws  IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dAccessRead", getId(), id, super.getPort());
        Channel channel = new ChannelBasic(new Socket("localhost", 8080));
        channel.send(message);
        System.out.println("read message 发送： "+message.toString());
        processor.process(channel, id);
        return 1;
    }

    //回复修改确认消息，(将数据设置为不可修改?)
    @CommandMethod
    private void dRelease(String variableId) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dRelease", variableId, super.getPort());
        Channel channel = new ChannelBasic(new Socket("localhost",8080));
        channel.send(message);
        // ServerMessage serverMessage = (ServerMessage) channel.recv();
    }

    //发出删除信号消息，等待回信
    @CommandMethod
    private void dFree(String id) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dFree",getId(), id, super.getPort());
        Channel channel = new ChannelBasic(new Socket("localhost", 8080));
        channel.send(message);
        // ServerMessage serverMessage = (ServerMessage) channel.recv();
    }

}
