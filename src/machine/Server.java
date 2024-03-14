package machine;

import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.exception.ServerException;
import utils.message.Message;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Server implements Machine{
    private final int port;
    private final String serverId;
    private final ServerProcessor processor;
    private Channel channel;
    private HashMap<String, LinkedList<String>> heap;//HashMap<variableId,LinkedList<clientId>>，第一个值为最新数据拥有者
    private final int clientMaxMunber = 10;
    private int elementNum = 0;

    public Server(int port, String id){
        this.port = port;
        this.serverId = id;
        this.heap = new HashMap<>();
        this.processor = new ServerProcessor();
        processor.setServer(this);
    }

    /**
     * 向哈希表中插入一个新的clientId和variableId的映射。
     * 如果映射已经存在，则将clientId添加到LinkedList的头部。
     * 如果哈希表已满，则抛出异常。
     *
     * @param variableId 要添加到哈希表的variableId。
     * @param clientId 要添加到与variableId关联的LinkedList中的clientId。
     * @throws ServerException 如果哈希表已满。
     */
    public void insertData(String variableId, String clientId) throws ServerException {
        if (variableExistsHeap(variableId)) {
            if (!dataExistsHeap(clientId, variableId)) {
                LinkedList<String> clientIds = heap.get(variableId);
                if(clientIds.size()<clientMaxMunber){
                    clientIds.addFirst(clientId);
                }else{
                    throw new ServerException("too much client in one data");
                }
            }else {
                throw new ServerException("data exists");
            }
        } else {
            LinkedList<String> newList = new LinkedList<>();
            newList.add(clientId);
            heap.put(variableId, newList);
            elementNum++;
        }
    }


    // 如果variableId不存在或clientId不在列表中，不执行任何操作
    public void deleteData(String variableId, String clientId) {
        if (variableExistsHeap(variableId)) {
            LinkedList<String> clientIds = heap.get(variableId);
            boolean removed = clientIds.remove(clientId);
            if (removed && clientIds.isEmpty()) {
                heap.remove(variableId);
                elementNum--;
            }
        }
    }

    public void deleteVariable(String variableId){
        if (heap.containsKey(variableId)) {
            heap.remove(variableId);
            elementNum--;
        }
    }

    public boolean variableExistsHeap(String variableId){
        return heap.containsKey(variableId);
    }

    public boolean dataExistsHeap(String clientId, String variableId){
        if (variableExistsHeap(variableId)){
            LinkedList<String> clientIds = heap.get(variableId);
            return clientIds.contains(clientId);
        }else{
            return false;
        }
    }

    public void start() throws ServerException, ClassNotFoundException {
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            int i = 0;
            while (! Thread.currentThread().isInterrupted()) {
                try (Socket s = ss.accept()) {
                    Channel channel = new ChannelBasic(s);
                    System.out.println("Debut de requête " + i);
                    Message message = processor.process(channel);

                    System.out.println(heap);

                    channel.send(message);
                }
                System.out.println("Fin de requête " + i);
                System.out.println("**********************\n");
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public int getPort() {
        return port;
    }

    public HashMap<String, LinkedList<String>> getHeap(){
        return heap;
    }


    @Override
    public void request(String methodType, String args) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException, ClassNotFoundException, InstantiationException {

    }

    @Override
    public void respond() throws IOException {
        // channel.send(message);
    }
}
