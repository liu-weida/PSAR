package test;

import machine.Client;
import machine.Server;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.exception.ServerException;

import javax.swing.text.html.StyleSheet;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Test {
    private Server _server;
    private ArrayList<Client> _clients = new ArrayList<Client>();
    Scanner scanner = new Scanner(System.in);
    public void createServer(){
        System.out.println("try to build server");
        if (_server == null) {
            try {
                _server = new Server(8080, "Server");
                new Thread(() -> {
                    try {
                        _server.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                System.out.println("Server started successfully on port 8080");
            } catch (Exception e) {
                System.out.println("Fail to create server");
                e.printStackTrace();
            }
        } else {
            System.out.println("Server already built");
        }
    }
    public void createClient(){
        System.out.println("Enter client port: ");
        int port = scanner.nextInt();
        scanner.nextLine();
        System.out.println("Enter client ID: ");
        String clientId = scanner.nextLine();
        System.out.println("clientId = " + clientId);

        for (Client c : _clients) {
            if (c.getPort() == port || c.getId().equals(clientId)) {
                System.out.println("Fail to create client: client exists");
                return;
            }
        }

        try {
            Client client = new Client(port, clientId);
            _clients.add(client);
            System.out.println("Client created successfully: port = " + port + ", clientID = " + clientId);
        } catch (Exception e) {
            System.out.println("Fail to create client");
            e.printStackTrace();
        }
    }
    public void selectClient() {
        System.out.println("Your clients (total = " + _clients.size() + "): ");
        for (int i = 0; i < _clients.size(); i++) {
            System.out.println(i + ": " + _clients.get(i).getId());
        }
        System.out.println("Select client by index: ");
        int index = scanner.nextInt();
        scanner.nextLine(); // consume newline
        if (index >= 0 && index < _clients.size()) {
            testClient(_clients.get(index));
        } else {
            System.out.println("Invalid client index.");
        }
    }
    public void testStart() {
        createServer();
        while (true) {
            System.out.println("Enter operation number:");
            System.out.println("1. Create client");
            System.out.println("2. Select client");
            System.out.println("3. End test");
            int operation = scanner.nextInt();
            scanner.nextLine();
            switch (operation) {
                case 1:
                    createClient();
                    break;
                case 2:
                    selectClient();
                    break;
                case 3:
                    return;
                default:
                    System.out.println("Invalid operation. Please try again.");
                    break;
            }
        }
    }
    public void testClient(Client client) {
        while (true) {
            System.out.println("Controlling Client: " + client.getId());
            System.out.println("1. Create data (int)");
            System.out.println("2. Perform request");
            System.out.println("3. End control client");
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
        if (!client.heapHaveData(name)) {
            client.setObject(name, value);
            System.out.println("Data creation successful: " + name + " = " + value);
        } else {
            System.out.println("Data with the name \"" + name + "\" already exists. Creation aborted.");
        }
    }

    private void performRequest(Client client) {
        System.out.println("Available requests:");
        System.out.println("1. dMalloc");
        System.out.println("2. dAccessWrite");
        System.out.println("3. dAccessRead");
        System.out.println("4. dRelease");
        System.out.println("5. dFree");
        System.out.println("Enter request number: ");
        int requestOption = scanner.nextInt();
        scanner.nextLine(); // consume newline

        System.out.println("Enter data name for the operation: ");
        String dataName = scanner.nextLine();

        try {
            switch(requestOption) {
                case 1:
                    if (!client.heapHaveData(dataName)) {
                        client.request("dMalloc", dataName);
                        System.out.println("dMalloc request sent for " + dataName);
                    } else {
                        System.out.println("Data already exists.");
                    }
                    break;
                case 2:
                    if (client.heapHaveData(dataName)) {
                        client.request("dAccessWrite", dataName);
                        System.out.println("dAccessWrite request sent for " + dataName);
                    } else {
                        System.out.println("Data does not exist.");
                    }
                    break;
                case 3:
                    if (client.heapHaveData(dataName)) {
                        client.request("dAccessRead", dataName);
                        System.out.println("dAccessRead request sent for " + dataName);
                    } else {
                        System.out.println("Data does not exist.");
                    }
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


    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
       Test test = new Test();
       test.testStart();
    }

}
