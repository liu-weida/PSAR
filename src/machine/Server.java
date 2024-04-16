package machine;

import annotations.ModifyMethod;
import utils.tools.Pair;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.OperationStatus;
import utils.processor.ServerProcessor;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server extends Machine{
    private HashMap<String, LinkedList<Pair>> heap = new HashMap<>(); //HashMap<variableId,LinkedList<clientId>>，第一个值为最新数据拥有者
    private ConcurrentHashMap<String, Boolean> heapLock = new ConcurrentHashMap<>();//用作锁 <varibleId，true/false>
    //false被锁，true未被锁
    private static ConcurrentHashMap<Integer, ExecutorService> clientThreads = new ConcurrentHashMap<>();//这个用来维持线程与客户端的一对一
    private final AtomicBoolean companionThread = new AtomicBoolean(false);
    private ServerSocket serverSocketHeart;  //用于心跳
    private int heartPort;

    public Server(int port, String id) throws IOException {
        super(id, port);
        restoreFromBackup();     //为了测试！！！ 正式使用的时候记得取消注释
        heartPort = port+1;
        serverSocketHeart = new ServerSocket(heartPort);   //用于心跳
    }


    public void start() throws IOException {
        ExecutorService service = Executors.newFixedThreadPool(3);
        service.submit(this::startBackupThread);
        service.submit(() -> {
            handleClientConnections(super.getServerSocket());
        });
        service.submit(() -> {
            handleClientConnections(serverSocketHeart);
        });
        service.shutdown(); //关闭线程池的提交功能
        System.out.println("Server started on port " + super.getPort() + " and " + heartPort);
    }


    private void handleClientConnections(ServerSocket serverSocket) {  // 处理通用和心跳请求
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();  // 接收客户端连接
                createThreads(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("SocketException");
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.out.println("Server socket close error: " + e.getMessage());
            }
        }
    }


    private void createThreads(Socket clientSocket) {
        int clientPort = clientSocket.getPort();
        ExecutorService executor = clientThreads.computeIfAbsent(clientPort, k -> Executors.newSingleThreadExecutor());
        executor.execute(() -> {
            try {
                ServerProcessor processor = new ServerProcessor();
                processor.setServer(this);
                Channel channel = new ChannelBasic(clientSocket);
                while (!clientSocket.isClosed()) {
                    processor.process(channel, " ");
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

    private void startBackupThread(){
        if (companionThread.compareAndSet(false, true)) {   //每30秒执行一次
            System.out.println("陪伴线程启动！！！！");
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(this::backUp, 30, 30, TimeUnit.SECONDS);
        }
    }
    private void backUp() {
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

    private void restoreFromBackup() {
        Path backupDir = Paths.get("log");
        if (!Files.exists(backupDir)) {
            return;
        }

        try {
            // 使用DirectoryStream列出所有文件，并找到最新的文件
            ArrayList<Path> files = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir)) {
                for (Path file : stream) {
                    files.add(file);
                }
            }

            if (files.isEmpty()) {
                return;
            }

            // 对文件进行排序，找到最新的一个文件
            files.sort((f1, f2) -> Long.compare(f2.toFile().lastModified(), f1.toFile().lastModified()));
            Path latestFile = files.get(0);
            System.out.println("正在从最新的备份文件恢复：" + latestFile);

            // 从最新的文件中读取heap和heapLock的状态
            try (FileInputStream fileIn = new FileInputStream(latestFile.toFile());
                 ObjectInputStream in = new ObjectInputStream(fileIn)) {
                heap = (HashMap<String, LinkedList<Pair>>) in.readObject();
                heapLock = (ConcurrentHashMap<String, Boolean>) in.readObject();
                System.out.println("数据恢复成功。");
            }

        } catch (Exception e) {
            System.out.println("恢复数据时出错：" + e.getMessage());
        }
    }

    public boolean variableExistsHeap(String variableId){
        return heap.containsKey(variableId);
    }
    public HashMap<String, LinkedList<Pair>> getHeap(){
        return heap;
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
