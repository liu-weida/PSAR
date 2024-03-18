package machine;

import annotations.CommandMethod;
import annotations.ModifyMethod;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.exception.ServerException;
import utils.message.Message;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;

public class Server implements Machine{
    private final int port;
    private final String serverId;
    private final ServerProcessor processor;
    ServerSocket ss;
    private Channel channel;
    private HashMap<String, LinkedList<String>> heap;//HashMap<variableId,LinkedList<clientId>>，第一个值为最新数据拥有者

    public Server(int port, String id) {
        this.port = port;
        this.serverId = id;
        this.heap = new HashMap<>();
        this.processor = new ServerProcessor();
        processor.setServer(this);
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

    public void start() throws ServerException, ClassNotFoundException, IOException {
        try {
            ss = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            int i = 0;
            while (! Thread.currentThread().isInterrupted()) {
                try (Socket s = ss.accept()) {
                    Channel channel = new ChannelBasic(s);
                    System.out.println("Debut de requête " + i);
                    Message message = processor.process(channel);

                    System.out.println(heap);

                    channel.send(message);
                }catch (SocketException e){
                    System.out.println("SocketException");
                }
                System.out.println("Fin de requête " + i);
                System.out.println("**********************************************************************************\n");
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (ss != null && !ss.isClosed()) {
                ss.close();
            }
        }
    }

    public void close() throws IOException {
        if (ss != null && !ss.isClosed()) {
            ss.close();
        }
        System.out.println("Server stopped.");
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

    @Override
    public boolean modifyHeap(String methodType, String key, String value){
        for (Method method: getClass().getMethods()){
            if (method.getName().equals(methodType) && method.isAnnotationPresent(ModifyMethod.class)){
                boolean b;
                try{
                    b = (boolean)method.invoke(this,key,value);
                }catch (InvocationTargetException | IllegalAccessException e){
                    return false;
                }
                return b;
            }
        }
        return false;
    }

    @ModifyMethod
    public boolean modifyHeapDMalloc(String variableId,String clientId){
        if (!heap.containsKey(variableId)) {
            LinkedList<String> newList = new LinkedList<>();
            newList.add(clientId);
            heap.put(variableId, newList);
            return true;
        }
        return false;
    }
    @ModifyMethod
    public boolean modifyHeapDAccessWrite(String variableId,String clientId){
        if(heap.containsKey(variableId)){
            LinkedList<String> localListW = heap.get(variableId);
            localListW.clear();
            localListW.add(clientId);
            return true;
        }
        return false;
    }
    @ModifyMethod
    public boolean modifyHeapDAccessRead(String variableId,String clientId){
        if(heap.containsKey(variableId)){
            LinkedList<String> localListR = heap.get(variableId);
            if(!localListR.contains(clientId))localListR.add(clientId);
            return true;
        }
        return false;
    }
    @ModifyMethod
    public boolean modifyHeapDFree(String variableId,String clientId){
        if(heap.containsKey(variableId)){
            heap.remove(variableId);
            return true;
        }
        return false;
    }
}
