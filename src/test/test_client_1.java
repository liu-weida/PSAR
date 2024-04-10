package test;

import machine.Client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class test_client_1 {
    private ArrayList<Client> _clients = new ArrayList<>();
    Scanner scanner = new Scanner(System.in);
    int firstPort = 6060; // 初始端口

    public void autoCreateClient(){
        // 仅创建一个客户端
        if (_clients.isEmpty()) {
            try {
                Client client = new Client(firstPort, "c1");
                _clients.add(client);
            } catch (Exception e){
                System.out.println("Fail to create client");
                e.printStackTrace();
            }
        }
    }

    public void testStart() {
        System.out.println("Creating initial client...");
        autoCreateClient(); // 自动创建初始客户端
        if (!_clients.isEmpty()) {
            testClient(_clients.get(0)); // 对当前唯一的客户端进行测试
        }
    }

    public void testClient(Client client) {
        boolean continueTesting = true;
        autoCreateDataForClient(client); // 为当前客户端自动创建数据
        while (continueTesting) {
            continueTesting = displayMainMenu(client);
        }
    }

    private boolean displayMainMenu(Client client) {
        System.out.println("Controlling Client: " + client.getId());
        System.out.println("1: Create data (int)");
        System.out.println("2: Perform request");
        System.out.println("3: Print all Data");
        System.out.println("4: End Test");
        System.out.println("Enter option number: ");
        int option = scanner.nextInt();
        scanner.nextLine(); // consume newline

        switch (option) {
            case 1:
                createData(client);
                break;
            case 2:
                return performRequest(client);
            case 3:
                printData(client);
                break;
            case 4:
                System.out.println("Test ended.");
                return false; // 结束测试
            default:
                System.out.println("Invalid option. Please try again.");
                break;
        }
        return true; // 继续显示菜单
    }

    private boolean performRequest(Client client) {
        int retryCount = 0;
        boolean requestSuccessful = false;

        while (!requestSuccessful && retryCount < 3) { // 最多重试3次
            try {
                // 展示数据选项并选择数据进行操作
                if (client.getLocalHeap().isEmpty()) {
                    System.out.println("No data available.");
                    return true; // 没有数据，返回主菜单
                }

                displayDataOptions(client); // 显示数据选项

                int dataIndex = getDataIndexFromUser(); // 获取用户选择的数据索引
                String dataName = getDataNameByIndex(client, dataIndex); // 根据索引获取数据名称

                displayAvailableRequests(); // 显示可用请求
                int requestOption = getRequestOptionFromUser(); // 获取用户选择的请求操作

                requestSuccessful = handleRequest(client, requestOption, dataName);
            } catch (IOException e) {
                System.out.println("Connection interrupted, attempting to reconnect...");
                client.reconnectToServer(); // 重连机制调用
                resetScanner(); // 重置Scanner
                System.out.println("Reconnected and scanner reset. Please try operation again.");
                return true; // 表示重连后需要回到主菜单继续测试
            } catch (Exception e) {
                System.out.println("Error during request: " + e.getMessage());
                e.printStackTrace();
                retryCount++;
            }
        }

        if (!requestSuccessful) {
            System.out.println("Failed to perform the operation after several attempts.");
        }
        return true; // 无论请求成功或失败，都回到主菜单
    }

    private void resetScanner() {
        scanner.close(); // 关闭旧的Scanner
        scanner = new Scanner(System.in); // 重新实例化Scanner
    }

    private void displayDataOptions(Client client) {
        System.out.println("Available data in the client heap:");
        int index = 1;
        for (String key : client.getLocalHeap().keySet()) {
            System.out.println(index++ + ": " + key);
        }
        System.out.println("Enter the index of the data name for the operation (index starts from 1): ");
    }

    private int getDataIndexFromUser() {
        int dataIndex = scanner.nextInt();
        scanner.nextLine();  // consume newline
        return dataIndex;
    }

    private String getDataNameByIndex(Client client, int dataIndex) {
        return (String) client.getLocalHeap().keySet().toArray()[dataIndex - 1];
    }

    private void displayAvailableRequests() {
        System.out.println("Available requests:");
        System.out.println("1. dMalloc");
        System.out.println("2. dAccessWrite");
        System.out.println("3. dAccessRead");
        System.out.println("4. dRelease");
        System.out.println("5. dFree");
        System.out.println("Enter request number: ");
    }

    private int getRequestOptionFromUser() {
        int requestOption = scanner.nextInt();
        scanner.nextLine();  // consume newline
        return requestOption;
    }


    private void autoCreateDataForClient(Client client) {
        String name = "cd1";
        int value = 100; // 给定一个初始值
        if (!client.heapHaveData(name)) {
            client.setObject(name, value);
            System.out.println("Data auto-creation successful for client " + client.getId() + ": " + name + " = " + value);
        } else {
            System.out.println("Data with the name \"" + name + "\" already exists in client " + client.getId() + ". Auto-creation aborted.");
        }
    }

    private void createData(Client client) {
        System.out.println("Enter a name for the data: ");
        String name = scanner.nextLine();
        System.out.println("Enter an integer value: ");
        int value = scanner.nextInt();
        scanner.nextLine();
        if (!client.heapHaveData(name)) {
            client.setObject(name, value);
            System.out.println("Data creation successful: " + name + " = " + value);
        } else {
            System.out.println("Data with the name \"" + name + "\" already exists. Creation aborted.");
        }
    }




    private boolean handleRequest(Client client, int requestOption, String dataName) throws Exception {
        switch (requestOption) {
            case 1:
                client.request("dMalloc", dataName);
                break;
            case 2:
                client.request("dAccessWrite", dataName);
                break;
            case 3:
                client.request("dAccessRead", dataName);
                break;
            case 4:
                client.request("dRelease", dataName);
                break;
            case 5:
                client.request("dFree", dataName);
                break;
            default:
                System.out.println("Invalid request option.");
                return false;
        }
        System.out.println("Request " + requestOption + " for " + dataName + " completed successfully.");
        return true;  // Return true if the request was handled successfully
    }


    public void printData(Client client){
        for (String s: client.getLocalHeap().keySet()){
            System.out.println(s + " = " + client.getLocalHeap().get(s));
        }
    }


    public static void main(String[] args) {
        test_client_1 test = new test_client_1();
        test.testStart();
    }
}
