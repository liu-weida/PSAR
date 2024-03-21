package utils.processor;

import machine.Server;
import utils.Pair;
import utils.exception.ServerException;
import utils.message.*;
import utils.channel.Channel;

import java.io.IOException;
import java.net.InetAddress;

public class ServerProcessor implements Processor{
    private Server server;
    // private ServerMessage message;
    // private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ServerProcessor(){
        this.server = null;
    }

    public void setServer(Server server){
        this.server = server;
    }

    public Message process(Channel channel, String id) throws IOException, ClassNotFoundException {
        Message message;
        System.out.println("waiting for message on port : "+ channel.getRemotePort());
        ClientMessage clientMessage = (ClientMessage) channel.recv();
        String clientId = clientMessage.getClientId();
        String variableId = clientMessage.getVariableId();
        int clientPort = clientMessage.getClientPort();
        System.out.println("message recv from client : " + clientId);
        System.out.println("client port : " + clientPort);
        System.out.println("variableId: " + variableId);

        switch (clientMessage.getCommand()) {
            case "dMalloc" -> message = handleDMalloc(variableId);
            case "dAccessWrite" -> message = handleDAccessWrite(variableId, channel.getRemoteHost(), clientPort);
            case "dAccessRead" -> message = handleDAccessRead(variableId);
            case "dRelease" -> message = handleDRelease(channel,variableId);
            case "dFree" -> message = handleDFree(variableId, channel);
            default -> message = new ServerMessage(MessageType.EXP,OperationStatus.COMMAND_ERROR);
        }
        return message;
    }

    //返回成功信息//如果数据信息已经存在或添加数据信息失败，发送错误信息
    private ServerMessage handleDMalloc(String variableId){
        System.out.println("收到初始化数据信息");
        //检查服务器中是否有这个数据
        if(server.variableExistsHeap(variableId)){
            return new ServerMessage(MessageType.DMA, OperationStatus.DATA_EXISTS);
        } else {
            //尝试在服务器堆中添加数据信息
            server.modifyHeapDMalloc(variableId);
            return new ServerMessage(MessageType.DMA, OperationStatus.SUCCESS);
        }
    }

    //检查是否有这个数据，如果存在并且未上锁，(如果还没有此客户的信息)尝试在服务器堆中添加数据信息。等待dRelease信息，接收到修改完毕消息后，设置成拥有最新消息客户(将数据放到双向链表头部代表此客户拥有最新数据信息)
    private ServerMessage handleDAccessWrite(String variableId, InetAddress host, int port) {
        System.out.println("收到写入请求");
        if(! server.variableExistsHeap(variableId)){
            return new ServerMessage(MessageType.DAW, OperationStatus.DATA_NOT_EXISTS);
        }

        switch (server.modifyHeapDAccessWrite(variableId, host, port)){
            case OperationStatus.SUCCESS -> {
                return new ServerMessage(MessageType.DAW, OperationStatus.SUCCESS);
            }
            case OperationStatus.LOCKED -> {
                return new ServerMessage(MessageType.DAW, OperationStatus.LOCKED);
            }
        }

        return null;
    }

    //检查是否有这个数据，如果存在并且未上锁，如果此客户不是最新消息客户，回信最新客户的地址用来联系。等待dRelease信息，接收到读取完毕消息后，设置成拥有最新消息客户(放到双向链表头部代表此客户拥有最新数据信息,如果出现问题无所谓)
    private Message handleDAccessRead(String variableId) {
        System.out.println("收到客户端阅读请求");
        if (!server.variableExistsHeap(variableId)) {
            return new ServerMessage(MessageType.DAR, OperationStatus.DATA_NOT_EXISTS);
        }

        System.out.println(server.modifyHeapDAccessRead(variableId).first().toString() + "aaaa"); //正常

        switch (server.modifyHeapDAccessRead(variableId).first()){
            case OperationStatus.SUCCESS ->{
                Pair p = (Pair) server.modifyHeapDAccessRead(variableId).second();
                System.out.println(p.first().toString()+"bbb");  //正常
                System.out.println(p.second().toString()+"ccc");    //正常

                System.out.println(new SendDataMessage((InetAddress) p.first(),(Integer) p.second()).toString()); //正常
                return new SendDataMessage((InetAddress) p.first(),(Integer) p.second());
            }
            case OperationStatus.LOCKED -> {
                return new ServerMessage(MessageType.DAW, OperationStatus.LOCKED);
            }
            default ->
                    throw new IllegalStateException("这句单纯用来凑数，防止ide报错: " + server.modifyHeapDAccessRead(variableId).first());
        }

    }


    //在这里收到Drelease是错误的，直接报错
    private ServerMessage handleDRelease(Channel channel,String variableId) throws IOException {
        System.out.println("数据使用完毕");


        switch (server.modifyHeapDRelease(variableId)){
            case OperationStatus.SUCCESS -> {
                return new ServerMessage(MessageType.DRE,OperationStatus.SUCCESS);
            }
            case OperationStatus.COMMAND_ERROR -> {
                return new ServerMessage(MessageType.DRE,OperationStatus.COMMAND_ERROR);
            }
        }

        return null;
    }

    //检查服务器是否有这个数据，如果存在，通知所有存在数据信息的客户删除数据，收到回信后删除这个数据信息
    private ServerMessage handleDFree(String variableId, Channel channel) throws IOException {
        System.out.println("收到客户端删除数据请求");
        //数据锁
        if (!server.variableExistsHeap(variableId)) {
            return new ServerMessage(MessageType.DFR,OperationStatus.DATA_NOT_EXISTS);

        } else {//如果存在
            //server.deleteVariable(variableId);
            return new ServerMessage(MessageType.DFR,OperationStatus.SUCCESS);
        }
        // server.modifyHeap("modifyHeapDFree", variableId, clientId);
    }
}
