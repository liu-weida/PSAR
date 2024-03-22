package machine;

import annotations.CommandMethod;
import annotations.ModifyMethod;
import utils.Pair;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.exception.ServerException;
import utils.message.Message;
import utils.message.OperationStatus;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;


public class Server extends Machine{
    private final ServerProcessor processor = new ServerProcessor();
    private HashMap<String, LinkedList<Pair>> heap = new HashMap<>(); //HashMap<variableId,LinkedList<clientId>>，第一个值为最新数据拥有者


    private ConcurrentHashMap<String, Boolean> heapLock = new ConcurrentHashMap<>();//用作锁 <varibleId，true/false>
                                                                                    //false被锁，true未被锁


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

                    System.out.println("heap： " + heap);

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
    public OperationStatus modifyHeapDMalloc(String variableId){

            LinkedList<Pair> newList = new LinkedList<>();
            heap.put(variableId, newList);
            heapLock.put(variableId,true);  //true -> 未被锁定
            return OperationStatus.SUCCESS;

    }

    @ModifyMethod
    public OperationStatus modifyHeapDAccessWrite(String variableId,InetAddress host, int port){

            if (heapLock.get(variableId)){    //检测是否被锁
                heapLock.put(variableId,false);  //如果没被锁则加锁
                System.out.println("lock锁定！");
            }else {
                System.out.println("lock已被锁！");
                return OperationStatus.LOCKED;
            }

            LinkedList<Pair> localListW = heap.get(variableId);
            Pair insertEl = new Pair(host, port);
            if (localListW.contains(insertEl)) {
                localListW.remove(insertEl);
            }
            localListW.addFirst(insertEl);

            return OperationStatus.SUCCESS;

    }

    @ModifyMethod
    public Pair modifyHeapDAccessRead(String variableId){

        if(heapLock.get(variableId)){
            return new Pair(OperationStatus.SUCCESS,heap.get(variableId).get(0));
        }else {
            System.out.println("lock已被锁！");
            return new Pair(OperationStatus.LOCKED,null);
        }

    }

    @ModifyMethod
    public OperationStatus modifyHeapDRelease(String variableId){
        System.out.println("已进入modifyHeapDRelease");

        if(!heapLock.get(variableId)){
            heapLock.put(variableId,true);
            System.out.println("lock已解锁！");
            return OperationStatus.SUCCESS;
        }

        return OperationStatus.COMMAND_ERROR;
    }

    @ModifyMethod
    public OperationStatus modifyHeapDFree(String variableId){

        if(!heapLock.get(variableId)){
            return OperationStatus.LOCKED;
        }else {
            heap.remove(variableId);
            heapLock.remove(variableId);
            return OperationStatus.SUCCESS;
        }

    }


}
