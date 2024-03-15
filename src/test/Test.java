package test;

import machine.Client;
import machine.Server;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.exception.ServerException;

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
        if(!(_server == null)){
            try{
                _server = new Server(8080, "Server");
                _server.start();
            } catch (ServerException |ClassNotFoundException e) {
                System.out.println("fail to create server");
            }
        }else{
            System.out.println("server already build");
        }
    }
    public void createClient(){
        System.out.println("client port : ");
        int port = scanner.nextInt();
        System.out.println("client ID : ");
        String clientId = scanner.nextLine();
        for(Client c : _clients){
            if(c.getPort() == port || c.getId() == clientId){
                System.out.println("fail to create client : client exists");
                return;
            }
        }
        try{
            Client client1 = new Client(port,clientId);
            _clients.add(client1);
        }catch(IOException e){
            System.out.println("fail to create client : IOException");
            return;
        }
        System.out.println("client create success: port = "+ port + " clientID = "+ clientId);
    }
    public void testStart(){
        createServer();
        while(_server!=null){
            System.out.println("operator insert number");
            System.out.println("create client : 0");
            System.out.println("select client : 1");
            System.out.println("test end : 2");
            int index = scanner.nextInt();
            switch (index){
                case 0:createClient();
                case 1:
                    System.out.println("your clients (total = " +_clients.size() + " ): ");
                    for(Client c : _clients){
                        System.out.println("your clients : " + c.getId());
                    }
                    System.out.println("select client : insert clientId");
                    String clientId = scanner.nextLine();
                    boolean find = false;
                    for(Client c : _clients){
                        if(c.getId() == clientId){
                            testClient(c);
                            find = true;
                            break;
                        }
                    }
                    if(!find){
                        System.out.println("error insert");
                    }
                case 2:
                    //_server.close();
                    _server = null;
                    _clients.clear();
                default:
                    System.out.println("error insert");
            }
        }

    }
    public void testClient(Client client){
        while(true){
            System.out.println("Controlling Client : "+ client.getId());
            System.out.println("operator insert number");
            System.out.println("create data(int) : 0");
            System.out.println("try request : 1");
            System.out.println("end control client : 2");
            int index = scanner.nextInt();
            switch (index){
                case 0:
                    System.out.println("insert a int");

                    if (scanner.hasNextInt()) {
                case 1:
                case 2:break;
                default:
                    System.out.println("error insert");
            }
        }
    }
    public static void sleepForTenSeconds() {
        try {
            // 睡眠10秒
            Thread.sleep(10000); // 10000毫秒 = 10秒
        } catch (InterruptedException e) {
            // 当线程的睡眠状态被中断时，会抛出InterruptedException
            Thread.currentThread().interrupt(); // 保守的中断处理
            System.out.println("Thread was interrupted, Failed to complete operation");
        }
    }
   public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
        //Channel channel = new ChannelBasic(new Socket("localhost", 8080));
        //Client client1 = new Client( 7070,"client1");
        //sleepForTenSeconds();
        int x = 10;
        client1.setObject("valueX",x);
        client1.request("dMalloc","valueX");

   /* client1.setObject("compter", 123);

        List<Object> req1 = new ArrayList<>();
        req1.add("compter");
        client1.request("dMalloc", req1);
        client1.respond();

        client1.setChannel(new ChannelBasic(new Socket("localhost", 8080)));
        client1.request("dAccessWrite", req1);
        client1.respond();

        Client client2 = new Client(6060,"client2", new ChannelBasic(new Socket("localhost", 8080)));
        client2.request("dAccessRead", req1);
        client2.respond();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // client1.setChannel(new ChannelBasic(new Socket("localhost", 6060)));
        Channel c = new ChannelBasic(new Socket("localhost", 6060));
        c.send(1);


        */
    }

}
