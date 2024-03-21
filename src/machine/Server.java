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


public class Server extends Machine{
    private final ServerProcessor processor = new ServerProcessor();
    private HashMap<String, LinkedList<Pair>> heap = new HashMap<>(); //HashMap<variableId,LinkedList<clientId>>，第一个值为最新数据拥有者

    public Server(int port, String id) throws IOException {
        super(id, port);
        processor.setServer(this);
    }

    public boolean variableExistsHeap(String variableId){
        return heap.containsKey(variableId);
    }

    public void start() throws ClassNotFoundException, IOException {
        try {
            System.out.println("Server started on port " + super.getPort());
            int i = 0;
            while (! Thread.currentThread().isInterrupted()) {
                try (Socket s = super.getServerSocket().accept()) {
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
            if (super.getServerSocket() != null && !super.getServerSocket().isClosed()) {
                super.getServerSocket().close();
            }
        }
    }


    public HashMap<String, LinkedList<Pair>> getHeap(){
        return heap;
    }

    public void request(String methodType, String args) {

    }

    public void respond() throws IOException {
        // channel.send(message);
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
            Pair insertEl = new Pair(host, port);
            if (localListW.contains(insertEl)) {
                localListW.remove(insertEl);
            }
            localListW.addFirst(insertEl);
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
