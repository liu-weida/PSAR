package test;

import machine.Client;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class autotest {
    private ArrayList<Client> _clients = new ArrayList<Client>();
    Scanner scanner = new Scanner(System.in);

    public void autoCreateClient(){
        int firstPort = 6060;
        for (int i = 1; i <= 10; i++){ // 索引从1开始
            try {
                Client client = new Client(firstPort + i - 1, String.valueOf(i));
                _clients.add(client);
                //System.out.println("Client created successfully: port = " + (firstPort + i - 1) + ", clientID = " + i);
            } catch (Exception e){
                System.out.println("Fail to create client");
                e.printStackTrace();
            }
        }
    }

    public void testStart() {
        autoCreateClient(); // 自动创建客户端
        for (int i = 0; i < _clients.size(); i++) {
            autoCreateDataForClient(_clients.get(i), i + 1); // 索引从1开始为每个客户端自动创建数据
        }
        System.out.println("Auto creation and data assignment completed. You can now perform requests.");

        // 提供用户进行操作的选项
        while (true) {
            System.out.println("Enter operation number:");
            System.out.println("1: Perform request");
            System.out.println("2: End Test");
            int operation = scanner.nextInt();
            scanner.nextLine(); // consume newline
            if (operation == 1) {
                System.out.println("Select a client by index for performing requests (index starts from 1):");
                int index = scanner.nextInt();
                scanner.nextLine(); // consume newline
                if (index > 0 && index <= _clients.size()) {
                    testClient(_clients.get(index - 1)); // 索引调整为从1开始
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
        HashMap<String, Object> localHeap = client.getLocalHeap(); // 获取Client中的数据堆
        if (localHeap.isEmpty()) {
            System.out.println("No data available.");
        } else {
            int index = 1; // 索引从1开始
            for (String key : localHeap.keySet()) {
                System.out.println(index++ + ": " + key); // 使用从1开始的索引打印
            }
        }

        System.out.println("Enter the index of the data name for the operation (index starts from 1): ");
        int dataIndex = scanner.nextInt();
        scanner.nextLine(); // consume newline

        // 用户输入的是从1开始的索引，所以要将其转换为数组（或集合）的索引（从0开始）
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
//                    if (client.heapHaveData(dataName)) {
//                        client.request("dAccessWrite", dataName);
//                        System.out.println("dAccessWrite request sent for " + dataName);
//                    } else {
//                        System.out.println("Data does not exist");
//                        System.out.println("Please set the object first");
//                    }
                    client.request("dAccessWrite", dataName);
                    System.out.println("dAccessWrite request sent for " + dataName);
                    break;
                case 3:
//                    if (client.heapHaveData(dataName)) {
//                        client.request("dAccessRead", dataName);
//                        System.out.println("dAccessRead request sent for " + dataName);
//                    } else {
//                        System.out.println("Data does not exist.");
//                    }

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

//    private void testStop(){
//        try {
//            if (_server != null) {
//                _server.close(); // 关闭服务器
//            }
//        } catch (IOException e) {
//            System.out.println("Error occurred while stopping the server.");
//            e.printStackTrace();
//        }
//        System.out.println("Test ended.");
//    }

    public static void main(String[] args) {
        autotest test = new autotest();
        test.testStart();
        //System.out.println(1);
        //Thread.currentThread().interrupt();
    }

}
