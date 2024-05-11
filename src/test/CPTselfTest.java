package test;

import machine.Client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CPTselfTest {
    private static List<Client> clientsList = new ArrayList<>();
    private static List<Runnable> jobsList = new ArrayList<>();

    public static void createClientList(int nbClient) throws IOException {
        for (int i = 1; i < nbClient+1; i++){
            String clientName = "Client" + i;
            int clientPort = 6060+i;
            Client client = new Client(clientPort,clientName);
            clientsList.add(client);
        }
    }

    public static void initiaJobList() {
        for (int i = 0; i < 30; i++) { // Dans l'hypothèse de 30 tâches
            jobsList.add(new Runnable() {
                @Override
                public void run() {
                    Client client = getClientForJob();
                    if (client != null) {
                        try {
                            clientRun(client);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            releaseClientAfterJob(client); // Relâchez l'objet Client après avoir terminé la tâche
                        }
                    }
                }
            });
        }
    }

    private static synchronized Client getClientForJob() {
        // Obtenir un client
        return clientsList.isEmpty() ? null : clientsList.remove(0);
    }

    private static synchronized void releaseClientAfterJob(Client client) {
        // Client libéré
        clientsList.add(client);
    }

    public synchronized static void clientRun(Client client) throws InvocationTargetException, IllegalAccessException, InterruptedException {
        synchronized (client){
            client.request("dAccessRead","c0");
            int ownCpt = getCpt(client);
            int ccc = ownCpt +1;
            updateCpt(client,ccc);
        }
        synchronized (client){
            client.request("dAccessWrite","c0");
            client.request("dRelease","c0");
            System.out.println("cpt: " + getCpt(client) );
        }
    }

    static int getCpt(Client client){
        HashMap<String,Object> hashMap = client.getLocalHeap();
        if (hashMap.containsKey("c0")) {
            return (Integer) hashMap.get("c0");
        } else {
            return -1;
        }
    }

    static void updateCpt(Client client, int newCpt) {
        HashMap<String, Object> hashMap = client.getLocalHeap();
        hashMap.put("c0", newCpt);
    }

    public static void initiaC0() throws IOException, InvocationTargetException, IllegalAccessException, InterruptedException {
        Client client0 = new Client(6060,"client0");
        String c0 = "c0";
        int numC0 = 0;
        client0.setObject(c0, numC0);
        client0.request("dMalloc",c0);
        client0.request("dAccessWrite",c0);
        client0.request("dRelease",c0);
    }

    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException, InterruptedException {
        initiaC0();
        createClientList(5);
        initiaJobList();
        long startTime = System.nanoTime();  // Heure de début du test
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (Runnable job : jobsList) {
            executorService.submit(job);
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.DAYS)) {
                executorService.shutdownNow(); // Tentative d'arrêt de toutes les tâches en cours
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow(); // Réessayer d'arrêter toutes les tâches en cours
            Thread.currentThread().interrupt(); // Préserver l'état des interruptions
        }
        long endTime = System.nanoTime();  // Temps de fin de test
        long executionTime = (endTime - startTime)/1000000;
    }
}
