package test;

import machine.Client;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class AutoTest {
    private ArrayList<Client> _clients = new ArrayList<Client>();
    Scanner scanner = new Scanner(System.in);

    public void autoCreateClient(){
        int firstPort = 6060;
        for (int i = 1; i <= 10; i++){
            try {
                Client client = new Client(firstPort + i - 1, String.valueOf(i));
                _clients.add(client);
            } catch (Exception e){
                System.out.println("Fail to create client");
                e.printStackTrace();
            }
        }
    }

    public void testStart() {
        autoCreateClient(); // Création automatique de clients
        for (int i = 0; i < _clients.size(); i++) {
            autoCreateDataForClient(_clients.get(i), i + 1);
        }
        System.out.println("Auto creation and data assignment completed. ");
        //pour C1 et C2 auto process dMalloc, dAccessWrite, dRelease
        if (_clients.size() >= 2) { // Assurez-vous qu'il y a au moins deux clients
            for (int i = 0 ; i<10; i++){
                mwrTest(_clients.get(i),i);
                readTest(_clients.get(i),i);
            }
        }

        while (true) {
            System.out.println("Enter operation number:");
            System.out.println("1: Perform request");
            System.out.println("2: End Test");
            int operation = scanner.nextInt();
            scanner.nextLine(); // consume newline
            if (operation == 1) {
                System.out.println("Select a client by index for performing requests (index from 1 to "+_clients.size()+"):");
                int index = scanner.nextInt();
                scanner.nextLine(); // consume newline
                if (index > 0 && index <= _clients.size()) {
                    testClient(_clients.get(index - 1)); // L'indexation est ajustée pour commencer à 1
                } else {
                    System.out.println("Invalid client index.");
                }
            } else if (operation == 2) {
                System.out.println("Test ended.");
                return;
            } else {
                System.out.println("Invalid operation. Please try again.");
            }
        }
    }

    private void mwrTest(Client client,int i) {
        try {
            client.request("dMalloc", "c"+(i+1) );
            System.out.println("dMalloc request auto-performed for c"+ (i+1) +" .");
            client.request("dAccessWrite", "c"+(i+1));
            System.out.println("dAccessWrite request auto-performed for c"+(i+1) +" .");
            client.request("dRelease", "c"+(i+1));
            System.out.println("dRelease request auto-performed for c"+(i+1) +" .");
        } catch (Exception e) {
            System.out.println("Failed to auto-perform operations for " + client.getId());
            e.printStackTrace();
        }
    }

    private void readTest(Client client,int i) {
        if (i != 0) {
            try {
                client.request("dAccessRead","c"+(i-1));
                System.out.println("c"+(i+1) + " send read request to c" + (i-1)+" .");
            } catch (Exception e) {
                System.out.println("Failed to auto-perform operations for " + client.getId());
                e.printStackTrace();
            }
        }else {
            int size = _clients.size();
            try {
                client.request("dAccessRead","c"+(size-1));
                System.out.println("c1" + " send read request to c" + (size-1) +" .");
            } catch (Exception e) {
                System.out.println("Failed to auto-perform operations for " + client.getId());
                e.printStackTrace();
            }
        }
    }


    private void autoCreateDataForClient(Client client, int index) {
        String name = "c" + index;
        int value = index;
        if (!client.heapHaveData(name)) {
            client.setObject(name, value);
            //System.out.println("Data auto-creation successful for client " + client.getId() + ": " + name + " = " + value);
        } else {
            System.out.println("Data with the name \"" + name + "\" already exists in client " + client.getId() + ". Auto-creation aborted.");
        }
    }

    public void printData(Client client){
        for (String s: client.getLocalHeap().keySet()){
            System.out.println(s + " = " + client.getLocalHeap().get(s));
        }
    }

    public void testClient(Client client) {
        while (true) {
            System.out.println("Controlling Client: " + client.getId());
            System.out.println("1: Create data (int)");
            System.out.println("2: Perform request");
            System.out.println("3: Print all Data");
            System.out.println("4: End control client");
            System.out.println("Enter option number: ");
            int option = scanner.nextInt();
            scanner.nextLine(); // consume newline
            switch (option) {
                case 1:
                    createData(client);
                    break;
                case 2:
                    performRequest(client);
                    break;
                case 3:
                    printData(client);
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }
        }
    }

    private void createData(Client client) {
        System.out.println("Enter a name for the data: ");
        String name = scanner.nextLine();
        System.out.println("Enter an integer value: ");
        int value = scanner.nextInt();
        scanner.nextLine();
        if (! client.heapHaveData(name)) {
            client.setObject(name, value);
            System.out.println("Data creation successful: " + name + " = " + value);
        } else {
            System.out.println("Data with the name \"" + name + "\" already exists. Creation aborted.");
        }
    }

    private void performRequest(Client client) {
        System.out.println("Available data in the client heap:");
        HashMap<String, Object> localHeap = client.getLocalHeap(); // Obtenir le tas de données dans le client
        if (localHeap.isEmpty()) {
            System.out.println("No data available.");
        } else {
            int index = 1; // 索引从1开始
            for (String key : localHeap.keySet()) {
                System.out.println(index++ + ": " + key); // Imprime en utilisant un index commençant par 1
            }
        }
        System.out.println("Enter the index of the data name for the operation (index starts from 1): ");
        int dataIndex = scanner.nextInt();
        scanner.nextLine(); // consume newline
        String dataName = (String) localHeap.keySet().toArray()[dataIndex - 1];
        System.out.println("Selected data name for operation: " + dataName);
        System.out.println("Available requests:");
        System.out.println("1. dMalloc");
        System.out.println("2. dAccessWrite");
        System.out.println("3. dAccessRead");
        System.out.println("4. dRelease");
        System.out.println("5. dFree");
        System.out.println("Enter request number: ");
        int requestOption = scanner.nextInt();
        scanner.nextLine(); // consume newline
        try {
            switch(requestOption) {
                case 1:
                    client.request("dMalloc", dataName);
                    System.out.println("dMalloc request sent for " + dataName);
                    break;
                case 2:
                    client.request("dAccessWrite", dataName);
                    System.out.println("dAccessWrite request sent for " + dataName);
                    break;
                case 3:
                    String dataName2 = scanner.nextLine();
                    client.request("dAccessRead", dataName2);
                    System.out.println("dAccessWrite request sent for " + dataName2);
                    break;
                case 4:
                    if (client.heapHaveData(dataName)) {
                        client.request("dRelease", dataName);
                        System.out.println("dRelease request sent for " + dataName);
                    } else {
                        System.out.println("Data does not exist.");
                    }
                    break;
                case 5:
                    if (client.heapHaveData(dataName)) {
                        client.request("dFree", dataName);
                        System.out.println("dFree request sent for " + dataName);
                    } else {
                        System.out.println("Data does not exist.");
                    }
                    break;
                default:
                    System.out.println("Invalid request option.");
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            System.out.println("Failed to perform the requested operation.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        AutoTest test = new AutoTest();
        test.testStart();
    }
}
