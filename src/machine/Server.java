package machine;

import annotations.CommandMethod;
import annotations.ModifyMethod;
import utils.Pair;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.exception.ServerException;
import utils.message.Message;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
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
    private HashMap<String, LinkedList<Pair>> heap;//HashMap<variableId,LinkedList<clientId>>，第一个值为最新数据拥有者

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

    public void start() throws ClassNotFoundException, IOException {
        try {
            ss = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            int i = 0;
            while (! Thread.currentThread().isInterrupted()) {
                try (Socket s = ss.accept()) {
                    Channel channel = new ChannelBasic(s);
                    System.out.println("Debut de requête " + i);
                    Message message = processor.process(channel, " ");

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

    public int getPort() {
        return port;
    }

    public HashMap<String, LinkedList<Pair>> getHeap(){
        return heap;
    }

    @Override
    public void request(String methodType, String args) {

    }

    @Override
    public void respond() throws IOException {
        // channel.send(message);
    }

    @Override
    public boolean modifyHeap(String key, Object value) {
        return false;
    }

    @ModifyMethod
    public boolean modifyHeapDMalloc(String variableId){
        if (! heap.containsKey(variableId)) {
            LinkedList<Pair> newList = new LinkedList<>();
            heap.put(variableId, newList);
            return true;
        }
        return false;
    }

    @ModifyMethod
    public boolean modifyHeapDAccessWrite(String variableId,InetAddress host, int port){
        if(heap.containsKey(variableId)){
            LinkedList<Pair> localListW = heap.get(variableId);
            localListW.clear();
            localListW.add(new Pair(host, port));
            return true;
        }
        return false;
    }

    @ModifyMethod
    public Pair modifyHeapDAccessRead(String variableId){
        if(heap.containsKey(variableId)){
            return heap.get(variableId).get(0);
        }
        return null;
    }
    @ModifyMethod
    public boolean modifyHeapDFree(String variableId){
        if(heap.containsKey(variableId)){
            heap.remove(variableId);
            return true;
        }
        return false;
    }
}
