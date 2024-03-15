package utils.processor;

import machine.Server;
import utils.exception.ServerException;
import utils.message.ClientMessage;
import utils.channel.Channel;
import utils.message.Message;
import utils.message.ServerMessage;
import utils.message.MessageType;
import utils.message.OperationStatus;

import java.io.IOException;
import java.net.InetAddress;

public class ServerProcessor implements Processor{
    private Server server;
    private ServerMessage message;
    // private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ServerProcessor(){
        this.server = null;
    }

    public void setServer(Server server){
        this.server = server;
    }

    public Message process(Channel channel) throws ServerException, IOException, ClassNotFoundException {
        System.out.println("waiting for message on port : "+ channel.getRemotePort());
        ClientMessage clientMessage = (ClientMessage) channel.recv();
        String clientId = clientMessage.getClientId();
        String variableId = clientMessage.getVariableId();
        int clientPort = clientMessage.getClientPort();
        System.out.println("message recv from client : "+ clientId);
        System.out.println("client port : "+ clientPort);
        System.out.println("variableId: "+ variableId);

        InetAddress clientHost = channel.getRemoteHost();
        // Class<?> clazz = clientMessage.getClazz();
        try {
            switch (clientMessage.getCommand()) {
                case "dMalloc" -> handleDMalloc(variableId, clientId, channel);
                case "dAccessWrite" -> handleDAccessWrite(variableId, clientId, channel);
                case "dAccessRead" -> handleDAccessRead(variableId, clientId, channel, clientPort, clientHost);
                case "dRelease" -> handleDRelease(channel);
                case "dFree" -> handleDFree(variableId, channel);
                default -> message = new ServerMessage(MessageType.EXP,OperationStatus.COMMAND_ERROR);
            }
        }catch(IOException e){
                //传输错误
                //尝试使用镜像
                throw new ServerException("传输错误",e);
        }
        return message;

    }

    //返回成功信息//如果数据信息已经存在或添加数据信息失败，发送错误信息
    private void handleDMalloc(String variableId, String clientId, Channel channel) throws IOException {
        System.out.println("收到初始化数据信息");
        //检查服务器中是否有这个数据
        /*if(server.variableExistsHeap(variableId)){
            message = new ServerMessage(MessageType.DMA, OperationStatus.DATA_EXISTS);
        }else{
            try{//尝试在服务器堆中添加数据信息
                server.modifyHeap(variableId,clientId,0);
            }catch(ServerException e){
                ServerMessage message = new ServerMessage(MessageType.DMA, OperationStatus.INSERT_ERROR);
                channel.send(message);
                return;
            }
            message = new ServerMessage(MessageType.DMA, OperationStatus.SUCCESS);

        }*/
    }

    //检查是否有这个数据，如果存在并且未上锁，(如果还没有此客户的信息)尝试在服务器堆中添加数据信息。等待dRelease信息，接收到修改完毕消息后，设置成拥有最新消息客户(将数据放到双向链表头部代表此客户拥有最新数据信息)
    private void handleDAccessWrite(String variableId, String clientId, Channel channel) throws IOException {
        System.out.println("收到写入请求");
        // lock.writeLock().lock();
        //数据锁
        if(!server.variableExistsHeap(variableId)){
            message = new ServerMessage(MessageType.DAW, OperationStatus.SUCCESS);
            return;
        }
//        server.insertData(variableId, clientId);
//        message = new ServerMessage(MessageType.DAW,OperationStatus.SUCCESS);
//        ClientMessage clientMessageWrite= (ClientMessage)channel.recv();
//        if(!Objects.equals(clientMessageWrite.getCommand(), "dRelease")){
//            message = new ServerMessage(MessageType.DAW,OperationStatus.COMMAND_ERROR);
//            //解锁下一个notifyone
//        }else{
//            try{//更新数据到list首位
//                server.deleteData(clientId, variableId);
//                server.insertData(clientId, variableId);
//            }catch(ServerException e){
//                message = new ServerMessage(MessageType.DAW,OperationStatus.INSERT_ERROR);
//                //解锁下一个notifyone
//                return;
//            }
//            message = new ServerMessage(MessageType.DAW,OperationStatus.SUCCESS);
//
//            //解锁下一个notifyone
//            // lock.writeLock().unlock();
//        }
        message = new ServerMessage(MessageType.DAW,OperationStatus.SUCCESS);
    }

    //检查是否有这个数据，如果存在并且未上锁，如果此客户不是最新消息客户，回信最新客户的地址用来联系。等待dRelease信息，接收到读取完毕消息后，设置成拥有最新消息客户(放到双向链表头部代表此客户拥有最新数据信息,如果出现问题无所谓)
    private void handleDAccessRead(String variableId, String clientId, Channel channel,int clientPort,InetAddress clientHost) throws IOException {
        System.out.println("收到客户端阅读请求");
        //数据锁
        // lock.readLock().lock();
        if (!server.variableExistsHeap(variableId)) {
            message = new ServerMessage(MessageType.DAR,OperationStatus.DATA_NOT_EXISTS);

            //解锁下一个notifyone
            return;
        }
//        ServerMessage message = new ServerMessage(MessageType.DAR,OperationStatus.WAIT_FOR_DRE,clientPort,clientHost);
//        channel.send(message);
//        ClientMessage clientMessageRead = (ClientMessage) channel.recv();
//        if (!Objects.equals(clientMessageRead.getCommand(), "dRelease")) {
//            message = new ServerMessage(MessageType.DAR,OperationStatus.COMMAND_ERROR);
//
//            //解锁下一个notifyone
//        } else {
//            try {
//                server.deleteData(clientId, variableId);
//                server.insertData(clientId, variableId);
//            } catch (ServerException e) {
//                message = new ServerMessage(MessageType.DAR,OperationStatus.INSERT_ERROR);
//
//                //解锁下一个notifyone
//                return;
//            }
//            message = new ServerMessage(MessageType.DAR,OperationStatus.SUCCESS);
//
//            //解锁下一个notifyone
//            // lock.readLock().unlock();
//        }
        message = new ServerMessage(MessageType.DAR,OperationStatus.SUCCESS, clientPort, clientHost);
    }

    //在这里收到Drelease是错误的，直接报错
    private void handleDRelease(Channel channel) throws IOException {
        System.out.println("数据使用完毕信息");
        message = new ServerMessage(MessageType.DRE,OperationStatus.COMMAND_ERROR);
    }

    //检查服务器是否有这个数据，如果存在，通知所有存在数据信息的客户删除数据，收到回信后删除这个数据信息
    private void handleDFree(String variableId, Channel channel) throws IOException {
        System.out.println("收到客户端删除数据请求");
        //数据锁
        if (!server.variableExistsHeap(variableId)) {
            message = new ServerMessage(MessageType.DFR,OperationStatus.DATA_NOT_EXISTS);

        } else {//如果存在
            //server.deleteVariable(variableId);
            message = new ServerMessage(MessageType.DFR,OperationStatus.SUCCESS);

            //notifyall
        }
    }
}
