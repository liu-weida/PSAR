package machine;

import annotations.ModifyMethod;
import utils.tools.Pair;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.Message;
import utils.message.OperationStatus;
import utils.processor.ServerProcessor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Server extends Machine{
    final ServerProcessor processor = new ServerProcessor();
    private HashMap<String, LinkedList<Pair>> heap = new HashMap<>(); //HashMap<variableId,LinkedList<clientId>>，第一个值为最新数据拥有者
    private ConcurrentHashMap<String, Boolean> heapLock = new ConcurrentHashMap<>();//用作锁 <varibleId，true/false>
                                                                                            //false被锁，true未被锁
    private static ConcurrentHashMap<Integer, ExecutorService> clientThreads = new ConcurrentHashMap<>();//这个用来维持线程与客户端的一对一
    private final AtomicBoolean companionThread = new AtomicBoolean(false);

    public Server(int port, String id) throws IOException {
        super(id, port);
        processor.setServer(this);
    }

    public boolean variableExistsHeap(String variableId){
        return heap.containsKey(variableId);
    }

    public void start() throws ClassNotFoundException, IOException {  //多线程部分
        try {
            System.out.println("Server started on port " + super.getPort());
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = super.getServerSocket().accept(); // 接收客户端连接，一个client全程只使用同一个socket


                if (companionThread.compareAndSet(false, true)) {   //每30秒执行一次，拿来做心跳也不错
                    startCompanionThread(); // 启动备份守护线程
                }

                int clientPort = clientSocket.getPort();
                ExecutorService executor = clientThreads.computeIfAbsent(clientPort, k -> Executors.newSingleThreadExecutor());
                executor.execute(() -> {
                    try {
                        Channel channel = new ChannelBasic(clientSocket);
                        while (!clientSocket.isClosed()) {
                            System.out.println("处理客户端请求");
                            Message message = processor.process(channel, " ");
                            System.out.println("heap： " + getHeap());
                            channel.send(message);
                        }
                        } catch (Exception e) {
                            System.out.println("处理客户端请求时出错: " + e.getMessage());
                        } finally {
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                System.out.println("关闭客户端连接时出错: " + e.getMessage());
                            }
                        }

                });
            }
        } catch (SocketException e) {
            System.out.println("SocketException");
        } finally {
            if (super.getServerSocket() != null && !super.getServerSocket().isClosed()) {
                super.getServerSocket().close();
            }
        }
    }

    public void startCompanionThread() {
        System.out.println("陪伴线程启动！！！！");
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this:: runCompanionTask, 30, 30, TimeUnit.SECONDS);
    }

    private void runCompanionTask() {
        try {
            Path backupDir = Paths.get("log");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            String fileName = "log_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyyHHmmss")) + ".ser";
            Path filePath = backupDir.resolve(fileName);

            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
                 ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
                out.writeObject(heap);
                out.writeObject(heapLock);
            }

            try (Stream<Path> files = Files.list(backupDir)) { // 检查并删除多余的备份文件
                List<Path> sortedFiles = files
                        .sorted(Comparator.comparingLong(file -> file.toFile().lastModified()))
                        .collect(Collectors.toList());

                while (sortedFiles.size() > 10) {
                    Path fileToDelete = sortedFiles.get(0);
                    Files.delete(fileToDelete);
                    sortedFiles.remove(fileToDelete);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
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
