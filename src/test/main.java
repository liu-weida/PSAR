//package test;
//
//import machine.Client;
//import utils.tools.Pair;
//
//import java.io.IOException;
//import java.lang.reflect.InvocationTargetException;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//
//public class main {
//
//    private int nbClient;
//    private int nbJob;
//    private int cpt;
//    private List<Client> clientsList;
//    private Pair job;
//    private List<Pair> jobsList;
//
//    public void createClientList(int nbClient) throws IOException {
//        for (int i = 0; i < nbClient; i++){
//            String clientName = "Client" + i;
//            int clientPort = 6060+i;
//            Client client = new Client(clientPort,clientName);
//            clientsList.add(client);
//        }
//    }
//
//    public void initiaClient() throws InvocationTargetException, IllegalAccessException {
//        for (Client c : clientsList){
//            String name = c.getId();
//            int value = 100;
//            c.setObject(name, value);
//            c.request("dMalloc",name);
//            c.request("dAcessWrite",name);
//            c.request("dRealse",name);
//        }
//    }
//
//    public void initiaJobList() throws InvocationTargetException, IllegalAccessException {
//        for (int i = 0; i < nbJob; i++) {
//            Random random = new Random();
//            int randomIndexOwnClient = random.nextInt(clientsList.size());
//            int randomIndexOtherClient = random.nextInt(clientsList.size());
//            Client ownClient = clientsList.get(randomIndexOwnClient);
//            Client otherClient = clientsList.get(randomIndexOtherClient);
//
//            List<String> ownVariableIDList = ownClient.getAllStringsFromLocalHeap();
//            List<String> otherVariableIDList = otherClient.getAllStringsFromLocalHeap();
//
//
//            int randomIndexOwnClintID = random.nextInt(ownVariableIDList.size());
//            int randomIndexOtherClintID = random.nextInt(otherVariableIDList.size());
//
//            String ownVariableID = ownVariableIDList.get(randomIndexOwnClintID);
//            String otherVariableID = otherVariableIDList.get(randomIndexOtherClintID);
//
//            int randomIndex = random.nextInt(2);
//
//            if (randomIndex == 0) {
//                Pair pair1 = new Pair("dAcessWrite",ownVariableID);
//                Pair pair2 = new Pair("dRealse",ownVariableID);
//                Pair pair = new Pair(pair1,pair2);
//                jobsList.add(pair);
//            }else {
//                ownClient.request("dAcessRead",otherVariableID);
//                Pair pair = new Pair("dAcessRead",otherVariableID);
//                jobsList.add(pair);
//            }
//        }
//    }
//
//    void dododo(){
//        Random random = new Random();
//        int randomIndexOwnClient = random.nextInt(clientsList.size());
//        Client ownClient = clientsList.get(randomIndexOwnClient);
//
//        int randomIndexJob = random.nextInt(jobsList.size());
//
//
//
//        int randomIndexOtherClient = random.nextInt(clientsList.size());
//        Client otherClient = clientsList.get(randomIndexOtherClient);
//    }
//
//    void clientRun(Client client){
//
//    }
//}
