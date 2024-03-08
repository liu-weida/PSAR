package utils.processor;

import machine.Server;
import utils.exception.ServerException;
import utils.message.ClientMessage;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.message.Message;
import utils.message.ServerMessage;

import java.io.IOException;
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

            ClientMessage clientMessage = (ClientMessage)channel.recv();
            // Class<?> clazz = clientMessage.getClazz();
            String clientId = clientMessage.getClientId();
            String varibaleId = clientMessage.getVaribaleId();
            Object obj = clientMessage.getObj();


            switch (clientMessage.getCommand()){
                case "dMalloc":
                    System.out.println("收到初始化数据信息");
                    //si已经存在这个数据，返回错误

                    //sinon
                    server.modifyHeap(clientId, varibaleId);
                    break;
                case "dAccessWrite":
                    System.out.println("收到写入请求");
                    //si服务器没有记录此数据或客户端信息，记录进heap
                    server.modifyHeap(clientId, varibaleId);
                    //如果数据上锁则等待信号
                    //如果数据未上锁则上锁并返回servermessage信息并等待客户端回信
                    break;
                case "dAccessRead":
                    System.out.println("暂时占位");
                    break;
                case "dRelease":
                    System.out.println("数据使用完毕信息");
                    //如果数据上锁
                    //给数据解锁
                    break;
                case "dFree":
                    System.out.println("暂时占位");
                    //如果没有此数据，回复报错信息
                    //如果存在，通知所有拥有数据的客户，收到所有回复后从heap中删除
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
