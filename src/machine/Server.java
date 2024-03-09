package machine;

import utils.exception.ServerException;
import utils.message.Message;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Server implements Machine{
    private final int port;
    private final String serverId;
    private final ServerProcessor processor;
    private HashMap<String, List<String>> heap;//HashMap<variableId,List<clientId>>
    private final int heapMaxSize = 10;
    private int elementNum = 0;


    public Server(int port, String id, ServerProcessor processor){
        this.port = port;
        this.serverId = id;
        this.processor = processor;
        this.heap = new HashMap<>();
    }



    //如果不存在:插入，如果已经存在:无事发生，如果heap满了:报错
    public void insertData(String variableId, String clientId) {
        if (variableExistsHeap(variableId)) {
            if (!dataExistsHeap(clientId, variableId)) {
                heap.get(variableId).add(clientId);
            }
        } else {
            if(elementNum < heapMaxSize){
                List<String> newList = new ArrayList<>();
                newList.add(clientId);
                heap.put(variableId, newList);
                elementNum++;
            }else {
                throw new ServerException("Heap is full");
            }
        }
    }

    // 如果variableId不存在或clientId不在列表中，不执行任何操作
    public void deleteData(String variableId, String clientId) {
        if (variableExistsHeap(variableId)) {
            List<String> clientIds = heap.get(variableId);
            boolean removed = clientIds.remove(clientId);
            if (removed && clientIds.isEmpty()) {
                heap.remove(variableId);
                elementNum--;
            }
        }
    }


    public boolean variableExistsHeap(String variableId){
        return heap.containsKey(variableId);
    }

    public boolean dataExistsHeap(String clientId, String variableId){
        if (variableExistsHeap(variableId)){
            List<String> clientIds = heap.get(variableId);
            return clientIds.contains(clientId);
        }else{
            return false;
        }
    }

    public void start(){
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            int i = 0;
            while (! Thread.currentThread().isInterrupted()) {
                try (Socket client = ss.accept()) {
                    System.out.println("Debut de requête " + i);
                    //respond(processor.process(client));
                    processor.process(client);
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

    @Override
    public void request(String methodType, List<Object> args) {

    }

    @Override
    public void respond(Message message) {

    }
}
