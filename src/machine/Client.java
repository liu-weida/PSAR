package machine;

import utils.message.ClientMessage;
import utils.channel.Channel;
import utils.message.Message;
import utils.message.ServerMessage;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class Client implements Machine{
    private final int port;
    private final String id;
    private Channel channel;

    public Client(int port, String id, Channel channel){
        this.port = port;
        this.id = id;
        this.channel = channel;
    }

    public String getId() {
        return id;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public void request() {

    }

    @Override
    public void respond(Message message) {

    }

    //
    private void dMalloc(String id) throws IOException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ClientMessage message = new ClientMessage("dMalloc", getId(), id);
        channel.send(message);
    }

    private int dAccessWrite(String id) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dAccessWrite", getId(), id);
        channel.send(message);
        ServerMessage serverMessage = (ServerMessage) channel.recv();
        if (serverMessage.getSuccesses()){
            System.out.println("ok");
        } else {
            System.out.println("not ok");
        }
        return 1;
    }

    // to do
    private int dAccessRead(String variableId) throws  IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dAccessRead", variableId);
        channel.send(message);
        ServerMessage serverMessage = (ServerMessage) channel.recv();
        if(serverMessage.getSuccesses()){
            System.out.println("ok");
        } else {
            System.out.println("not ok");
        }
        return 1;
    }

    private void dRelease(Object obj) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dRelease", obj);
        channel.send(message);
        ServerMessage serverMessage = (ServerMessage) channel.recv();
    }
    private void dFree(String id) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dFree", id);
        channel.send(message);
        ServerMessage serverMessage = (ServerMessage) channel.recv();
    }

}
