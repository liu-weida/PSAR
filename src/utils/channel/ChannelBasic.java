package utils.channel;

import utils.message.ClientMessage;
import utils.message.Message;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import java.util.logging.Logger;
import java.util.logging.Level;


public class ChannelBasic implements Channel {

    //这个channelBasic实现了一个表面上的buffer
    //这个buffer实际上只起到记录功能，他可以保存所有尚未被读取的消息（但是实际上此时消息还是在TCP的缓存区中而不是buffer中）
    //消息在被服务器处理之后，从buffer中去除
    //如果消息被锁拦住，则将消息放入lockedMessageMap，等待drelease之后再去掉

    //lockedMessageMap同样只起到记录作用
    //被拦住的消息会在三秒后由客户端重新发送
    //大致流程 客户端 --（请求）-->服务器 --（被拦）--（加上locked表示）--> 客户端 ---（三秒后重发）-->服务器 --（如果还是被拦则循环这个过程） -->
    //我尽力了hhhhh

    //buffer的计数和被锁住的map的计数在server中


    protected Queue<Message> ownQueue; //每个channelBasic自己的消息队列
    private static final List<Queue<Message>> sharedMessageQueues = Collections.synchronizedList(new ArrayList<>()); //所有channelBasic实例的ownQueue会被汇集到这里
    private static final ConcurrentHashMap<String, List<Message>> messageMap = new ConcurrentHashMap<>(); //从sharedMessageQueues中读取variableid和message，然后汇聚成哈希表
    private static final ConcurrentHashMap<String, List<Message>> lockedMessageMap = new ConcurrentHashMap<>(); //用来保存所有被锁拦住的进程
    private static final ExecutorService sharedExecutor = Executors.newSingleThreadExecutor(); // 单个后台线程处理所有消息
    protected final Socket socket;
    protected ObjectOutputStream oos;
    protected ObjectInputStream ois;
    private static final Logger LOGGER = Logger.getLogger(ChannelBasic.class.getName());//log 不用管


    public ChannelBasic(Socket socket) throws IOException {
        this.socket = socket;
        ownQueue = new ConcurrentLinkedQueue<>();
        sharedMessageQueues.add(ownQueue);
        startSharedProcessing(); // 确保处理线程是运行的
    }

    // 这个send和原版其实一样
    public void send(Message message) throws IOException {
        if (oos == null) {
            oos = new ObjectOutputStream(socket.getOutputStream());
        }
        oos.writeObject(message);
        oos.flush();
    }
    // 这个recv其实也和原版一样
    public Message recv() throws IOException, ClassNotFoundException {
        if (ois == null) {
            ois = new ObjectInputStream(socket.getInputStream());
        }
        return (Message) ois.readObject();
    }

    // 这个用来处理需要经过消息队列的消息
    public void sendC2S(Message message) throws IOException {
        ownQueueOffer(message);
        send(message);
    }

    public void ownQueueOffer(Message message){
        ownQueue.offer(message);
    }
    public void ownQueuePOP(){
        ownQueue.poll();
    }


    public static void addMessageToLockedMap(Message message) {
        ClientMessage clientMessage = (ClientMessage)message;
        String variableId = clientMessage.getVariableId();

        List<Message> messages = lockedMessageMap.computeIfAbsent(variableId, k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (messages) {
            if (!messages.contains(message)) {
                messages.add(message);
            }
        }
    }

    public static void removeMessagesByVariableId(String variableId) {
        List<Message> removed = lockedMessageMap.remove(variableId);
//        if (removed != null) {}
    }


    public static void printMessageCounts() {

        for (Map.Entry<String, List<Message>> entry : messageMap.entrySet()) {
            String variableId = entry.getKey();
            int count = getMessagesNum(variableId);
            if (count != 0) {
                System.out.print("Current message counts: ");
                System.out.println("Variable ID: " + variableId + " - Messages: " + count);
            }
        }
    }

    public static void printLockedMessageCounts() {

        for (Map.Entry<String, List<Message>> entry : lockedMessageMap.entrySet()) {
            String variableId = entry.getKey();
            int count = getLockedMessagesNum(variableId);
            if (count != 0) {
                System.out.print("Current locked message counts: ");
                System.out.println("Variable ID: " + variableId + " - Locked Messages: " + count);
            }
        }
    }



    private static int getMessagesNum(String variableId) {
        List<Message> messages = messageMap.get(variableId);
        if (messages == null) {
            return 0;
        }
        return messages.size();
    }


    private static int getLockedMessagesNum(String variableId) {
        List<Message> messages = lockedMessageMap.get(variableId);
        if (messages == null) {
            return 0;
        }
        return messages.size();
    }

    @Override
    public Object recvWithTimeout(int timeout) throws IOException, ClassNotFoundException {  //这是个带超时的recv
        if (timeout > 0) {
            socket.setSoTimeout(timeout);
        }
        try {
            if (ois == null) {
                ois = new ObjectInputStream(socket.getInputStream());
            }
            return ois.readObject();
        } catch (SocketTimeoutException e) {
            throw new IOException("接收超时: " + e.getMessage(), e); //超时之后会抛出SocketTimeoutException
        }
    }

    @Override
    public InetAddress getRemoteHost() {
        return socket.getInetAddress();
    }

    @Override
    public int getRemotePort() {
        return socket.getPort();
    }

    @Override
    public InetAddress getLocalHost() {
        return socket.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public Socket getSocket() {
        return socket;
    }

    // 启动单例处理线程
    private static void startSharedProcessing() {
        System.out.println("线程已开启");
        sharedExecutor.submit(() -> {
            while (true) {
                Iterator<Queue<Message>> it = sharedMessageQueues.iterator();
                while (it.hasNext()) {
                    Queue<Message> queue = it.next();
                    Message message = queue.poll();
                    if (message != null && message instanceof ClientMessage) {
                        processClientMessage((ClientMessage) message);
                    }
                }
//                try {
//                    Thread.sleep(100); // 防止过度循环
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                }
            }
        });
    }

    // 处理客户端消息
    private static void processClientMessage(ClientMessage message) {
        String variableId = message.getVariableId();
        messageMap.computeIfAbsent(variableId, k -> Collections.synchronizedList(new ArrayList<>())).add(message);
    }

    // 提供一个查询特定 variableId 消息数量的方法
    public static int getMessageCountByVariableId(String variableId) {
        List<Message> messages = messageMap.get(variableId);
        return messages != null ? messages.size() : 0;
    }

    public void close() throws IOException {
        try {
            if (ois != null) {
                ois.close();
            }
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } finally {
                socket.close();
            }
        }
    }
}



