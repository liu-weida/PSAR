package utils.processor;

import machine.Server;
import utils.exception.ServerException;
import utils.message.ClientMessage;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.Message;
import utils.message.ServerMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;

public class ServerProcessor implements Processor{
    private Server server;

    public ServerProcessor(Server server){
        this.server = server;
    }

    public Message process(Socket socket){

        Channel channel = new ChannelBasic(socket);
        try {
            int clientPort = channel.getRemotePort();
            InetAddress clienthost = channel.getRemoteHost();
            ClientMessage clientMessage = (ClientMessage)channel.recv();
            // Class<?> clazz = clientMessage.getClazz();
            String clientId = clientMessage.getClientId();
            String varibaleId = clientMessage.getVaribaleId();
            Object obj = clientMessage.getObj();


            switch (clientMessage.getCommand()){
                case "dMalloc":
                    System.out.println("收到初始化数据信息");
                    //如果已经存在此数据
                    //回复客户端传输错误信息
                    try{
                        server.insertData(clientId, varibaleId);
                    }catch (serverException){//满了加不进去
                        //回复客户端传输错信息
                        break;
                    }
                    //回复客户端传输顺利信息
                    break;
                case "dAccessWrite":
                    System.out.println("收到写入请求");
                    //如果不存在此数据
                    //回复客户端错误信息
                    //数据锁
                    try{
                        server.insertData(clientId, varibaleId);
                    }catch(serverException){
                        //回复客户端错误信息
                        break;
                    }
                    //如果数据未上锁则上锁并返回顺利信息并等待客户端dRelease信息
                    //根据信息处理...
                    break;
                case "dAccessRead":
                    System.out.println("收到客户端阅读请求");
                    //如果不存在此数据
                    //回复客户端错误信息
                    //数据锁
                    try{
                        server.insertData(clientId, varibaleId);
                    }catch(serverException){
                        //回复客户端错误信息
                        break;
                    }
                    //如果数据未上锁则上锁并返回顺利信息并等待客户端dRelease信息
                    //根据信息处理...
                    break;
                case "dRelease":
                    System.out.println("数据使用完毕信息");
                    //回复错误信息
                    break;
                case "dFree":
                    System.out.println("收到客户端删除数据请求");
                    if(!server.variableExistsHeap(varibaleId)){
                        //回复报错信息
                    }else{//如果存在
                        //数据锁
                        server.deleteData(varibaleId,clientId);
                    }
                    break;
                default:
                    // no such method
                    ServerException serverException = new ServerException("No such method!!!");
                    channel.send(serverException);
            }


            //les restes a faire
            ServerMessage serverMessage = null;
            channel.send(serverMessage);

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return null;

    }
}
