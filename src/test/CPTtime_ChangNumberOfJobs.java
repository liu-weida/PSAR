package test;

import machine.Client;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CPTtime_ChangNumberOfJobs {
    private List<Client> clientsList = new ArrayList<>();
    private List<Runnable> jobsList = new ArrayList<>();
    private int nb = 1;

    public void createClientList(int nbClient) throws IOException {
        int start = nb;
        for (int i = start; i < start + nbClient; i++) {
            String clientName = "Client" + i;
            int clientPort = 16060 + i;
            Client client = new Client(clientPort, clientName);
            clientsList.add(client);
        }
        nb += nbClient;
    }

    public void initiaJobList(int nb) {
        for (int i = 0; i < nb; i++) { 
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
                            releaseClientAfterJob(client); 
                        }
                    }
                }
            });
        }
    }

    private Client getClientForJob() {
        return clientsList.isEmpty() ? null : clientsList.remove(0);
    }

    private void releaseClientAfterJob(Client client) {
        clientsList.add(client);
    }


    public void clientRun(Client client) throws InvocationTargetException, IllegalAccessException, InterruptedException {
        synchronized (Client.class){
            client.request("dAccessRead","c0");
            int ownCpt = getCpt(client);
            int ccc = ownCpt +1;
            updateCpt(client,ccc);
        }
        synchronized (Client.class){
            client.request("dAccessWrite","c0");
            client.request("dRelease","c0");
            System.out.println("cpt: " + getCpt(client) );
        }
    }

    int getCpt(Client client){
        HashMap<String,Object> hashMap = client.getLocalHeap();
        if (hashMap.containsKey("c0")) {
            return (Integer) hashMap.get("c0");
        } else {
            return -1;
        }
    }

    void updateCpt(Client client, int newCpt) {
        HashMap<String, Object> hashMap = client.getLocalHeap();
        hashMap.put("c0", newCpt);
    }

    public void initiaC0() throws IOException, InvocationTargetException, IllegalAccessException, InterruptedException {
        Client client0 = new Client(6060,"client0");
        String c0 = "c0";
        int numC0 = 0;
        client0.setObject(c0, numC0);
        client0.request("dMalloc",c0);
        client0.request("dAccessWrite",c0);
        client0.request("dRelease",c0);
    }

    public long test(int nb) throws IOException, InterruptedException, InvocationTargetException, IllegalAccessException {
        createClientList(5);  
        initiaJobList(nb);         
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        long startTime = System.nanoTime();  
        for (Runnable job : jobsList) {
            executorService.submit(job);
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.DAYS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow(); 
            Thread.currentThread().interrupt(); 
        }
        long endTime = System.nanoTime();  
        long executionTime = (endTime - startTime)/1000;
        clientsList.clear();
        jobsList.clear();
        return executionTime;
    }

    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException, InterruptedException {
        CPTtime_ChangNumberOfJobs calculsCPT = new CPTtime_ChangNumberOfJobs();
        calculsCPT.initiaC0();
        FileWriter fileWriter = new FileWriter("Times_Change_NUM_of_Jobs");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        int[] is = {1,50,100,150,200,250,300,350,400,450,500,550,600,650,700,750,800,850,900,950,1000};
        for(int i: is) {
            long executionTime = calculsCPT.test(i);
            printWriter.println( executionTime);  
            //System.out.println("Test " + 5 + " completed in " + executionTime + " ms.");
        }
        printWriter.close();  
        fileWriter.close();  
    }
}
