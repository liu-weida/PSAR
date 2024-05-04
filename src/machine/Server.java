package machine;

import annotations.ModifyMethod;
import rmi.ServerErrorSet;
import rmi.ForcedServerShutdown;
import utils.channel.ChannelWithBuffer;
import utils.enums.ServerState;
import utils.tools.Pair;
import utils.channel.Channel;
import utils.enums.OperationStatus;
import utils.processor.ServerProcessor;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server extends Machine implements ForcedServerShutdown, ServerErrorSet {
    // Enregistre le propriétaire de chaque donnée: HashMap<varibleID,linkedList<Pair<host,port>>>
    private HashMap<String, LinkedList<Pair>> heap = new HashMap<>();
    // Verrou de données pour chaque donnée : false = verrouillé, true = non verrouillé
    private ConcurrentHashMap<String, Boolean> heapLock = new ConcurrentHashMap<>();
    // Maintient une correspondance un-à-deux entre les threads et les clients : ConcurrentHashMap<localPort,ExecutorService>
    private static ConcurrentHashMap<Integer, ExecutorService> clientThreads = new ConcurrentHashMap<>();
    // Utilisé pour déclencher le thread compagnon(Pour une sauvegarde régulière des données)
    private final AtomicBoolean companionThread = new AtomicBoolean(false);
    // ServerSocket pour HeartBeat
    private ServerSocket serverSocketHeart;
    // Port pour HeartBeat
    private int heartPort;

    // Constructeur du serveur
    public Server(int port, String id) throws IOException {
        super(id, port);
        restoreFromBackup();     // Charger les données de sauvegarde
        heartPort = port+1;
        serverSocketHeart = new ServerSocket(heartPort);   // ServerSocket pour HeartBeat
        bufferDisplay();
        registerRmiServer();  //RMI
    }

    // Démarrage du serveur
    public void start() throws IOException {
        ExecutorService service = Executors.newFixedThreadPool(3);
        service.submit(this::startBackupThread); // Sauvegarde périodique des données du serveur
        //Pour les connexions demande-réponse
        service.submit(() -> {
            handleClientConnections(super.getServerSocket());
        });
        //Pour les connexions Heartbeat
        service.submit(() -> {
            handleClientConnections(serverSocketHeart);
        });
        service.shutdown(); // Fermer la possibilité de soumettre de nouvelles tâches au pool
        System.out.println("Server started on port " + super.getPort() + " and " + heartPort);
    }

    // Gère les connexions client
    private void handleClientConnections(ServerSocket serverSocket) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();  // Accepter une connexion client
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
                System.out.println("Erreur lors de la fermeture du socket serveur : " + e.getMessage());
            }
        }
    }

    // Créez deux threads pour chaque client connecté (un thread pour la demande-réponse et un thread pour la connexion Heartbeat).
    private void createThreads(Socket clientSocket) {
        int clientPort = clientSocket.getPort();
        ExecutorService executor = clientThreads.computeIfAbsent(clientPort, k -> Executors.newSingleThreadExecutor());
        executor.execute(() -> {
            try {
                ServerProcessor processor = new ServerProcessor();
                processor.setServer(this);
                Channel channel = new ChannelWithBuffer(clientSocket);
                while (!clientSocket.isClosed()) {
                    processor.process(channel, " ", null);
                }
            } catch (Exception e) {
                System.out.println("Erreur lors du traitement de la requête client : " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Erreur lors de la fermeture de la connexion client : " + e.getMessage());
                }
            }
        });
    }

    // Lance périodiquement le thread de sauvegarde des données(30s)
    private void startBackupThread(){
        if (companionThread.compareAndSet(false, true)) {
            System.out.println("Thread compagnon démarré !");
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(this::backUp, 30, 30, TimeUnit.SECONDS);
        }
    }

    // Sauvegarde des données
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

            try (Stream<Path> files = Files.list(backupDir)) {
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

    // Chargement des données depuis la sauvegarde
    private void restoreFromBackup() {
        Path backupDir = Paths.get("log");
        if (!Files.exists(backupDir)) {
            return;
        }

        try {
            ArrayList<Path> files = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir)) {
                for (Path file : stream) {
                    files.add(file);
                }
            }

            if (files.isEmpty()) {
                return;
            }
            
            files.sort((f1, f2) -> Long.compare(f2.toFile().lastModified(), f1.toFile().lastModified()));
            Path latestFile = files.get(0);
            System.out.println("Restauration depuis le dernier fichier de sauvegarde : " + latestFile);

            try (FileInputStream fileIn = new FileInputStream(latestFile.toFile());
                 ObjectInputStream in = new ObjectInputStream(fileIn)) {
                heap = (HashMap<String, LinkedList<Pair>>) in.readObject();
                heapLock = (ConcurrentHashMap<String, Boolean>) in.readObject();
                System.out.println("Restauration des données réussie.");
            }

        } catch (Exception e) {
            System.out.println("Erreur lors de la restauration des données : " + e.getMessage());
        }
    }

    // Affichage périodique du nombre de messages en tampon et des messages verrouillés
    private void bufferDisplay() {
        ScheduledExecutorService singletonExecutorService = Executors.newSingleThreadScheduledExecutor();
        Runnable task = () -> {
            ChannelWithBuffer.printMessageCounts();
            ChannelWithBuffer.printLockedMessageCounts();
        };
        singletonExecutorService.scheduleAtFixedRate(task, 1, 1, TimeUnit.SECONDS);
    }

    // Vérifie si une variable existe dans le tas
    public boolean variableExistsHeap(String variableId){
        return heap.containsKey(variableId);
    }

    // Getter pour la table des données
    public HashMap<String, LinkedList<Pair>> getHeap(){
        return heap;
    }

    // Traitement des signaux dmalloc sur la table des données
    @ModifyMethod
    public OperationStatus modifyHeapDMalloc(String variableId){
        LinkedList<Pair> newList = new LinkedList<>();
        heap.put(variableId, newList);
        heapLock.put(variableId, true);
        return OperationStatus.SUCCESS;
    }

    // Traitement des signaux daccesswrite sur la table des données
    @ModifyMethod
    public OperationStatus modifyHeapDAccessWrite(String variableId, InetAddress host, int port){
        if (heapLock.get(variableId)) {
            heapLock.put(variableId, false);
            System.out.println("Verrouillage activé !");
        } else {
            System.out.println("Verrou déjà activé !");
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

    // Traitement des signaux daccessread sur la table des données
    @ModifyMethod
    public Pair modifyHeapDAccessRead(String variableId){
        if(heapLock.get(variableId)){
            return new Pair(OperationStatus.SUCCESS,heap.get(variableId).get(0));
        } else {
            System.out.println("Verrou déjà activé !");
            return new Pair(OperationStatus.LOCKED,null);
        }
    }

    // Traitement des signaux dmrelease sur la table des données
    @ModifyMethod
    public OperationStatus modifyHeapDRelease(String variableId){
        System.out.println("Entrée dans modifyHeapDRelease");

        if(!heapLock.get(variableId)){
            heapLock.put(variableId, true);
            System.out.println("Verrou désactivé !");
            return OperationStatus.SUCCESS;
        }

        return OperationStatus.ERROR;
    }

    // Traitement des signaux dfree sur la table des données
    @ModifyMethod
    public OperationStatus modifyHeapDFree(String variableId){
        if(!heapLock.get(variableId)){
            return OperationStatus.LOCKED;
        } else {
            heap.remove(variableId);
            heapLock.remove(variableId);
            return OperationStatus.SUCCESS;
        }
    }

    // Fermeture forcée du serveur
    @Override
    public void forcedserverShutdown() throws RemoteException {
        backUp();
        try {
            System.out.println("Fermeture immédiate du serveur...");

            if (serverSocketHeart != null && !serverSocketHeart.isClosed()) {
                serverSocketHeart.close();
                System.out.println("Socket du service de vie fermé immédiatement.");
            }

            ServerSocket mainServerSocket = super.getServerSocket();
            if (mainServerSocket != null && !mainServerSocket.isClosed()) {
                mainServerSocket.close();
                System.out.println("Socket du service principal fermé immédiatement.");
            }

            clientThreads.forEach((port, executor) -> {
                immediateShutdown(executor);
            });

            System.out.println("Tous les threads clients ont été fermés immédiatement.");

            System.exit(0);

        } catch (IOException e) {
            System.out.println("Erreur lors de la fermeture du serveur : " + e.getMessage());
        }
    }

    // Arrêt immédiat de tous les tâches en cours dans un pool
    private void immediateShutdown(ExecutorService pool) {
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    // Enregistrement du service RMI permettant aux clients d'invoquer des méthodes locales à distance
    private void registerRmiServer() {
        try {
            Server obj = this;
            Remote stub = (Remote) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("Registre RMI créé.");
            } catch (RemoteException e) {
                System.out.println("Le registre existe déjà.");
                registry = LocateRegistry.getRegistry(1099);
            }

            registry.rebind("RemoteShutdownService", stub);
            System.out.println("Service de fermeture à distance enregistré dans le registre.");
            registry.rebind("ServerErrorService", stub);
            System.out.println("Service d'erreur serveur enregistré dans le registre.");
        } catch (Exception e) {
            System.err.println("Exception serveur RMI : " + e.toString());
            e.printStackTrace();
        }
    }

    // Test de la gestion des erreurs serveur
    @Override
    public void setServerError(ServerState serverState) throws RemoteException {
        ServerProcessor.setNormalorNot(serverState);
    }
}
