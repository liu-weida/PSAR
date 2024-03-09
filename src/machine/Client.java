package machine;

import annotations.CommandMethod;
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
    private final String clientId;
    private Channel channel;

    public Client(int port, String clientId, Channel channel){
        this.port = port;
        this.clientId = clientId;
        this.channel = channel;
    }

    public String getId() {
        return clientId;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public void request(String methodType, List<Object> args) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        for (Method method : getClass().getDeclaredMethods()) {
            // 检查方法是否有@CommandMethod注解
            if (method.isAnnotationPresent(CommandMethod.class)) {
                if (methodType.equals(method.getName())) {
                    if (method.getParameterTypes().length == args.size()) {
                        method.setAccessible(true);
                        try {
                            method.invoke(this, args.toArray());
                            return;
                        } catch (IllegalArgumentException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                }
            }
        }
        throw new NoSuchMethodException("Method " + methodType + " with " + args.size() + " parameters not found or not annotated with @CommandMethod.");
    }



    @Override
    public void respond(Message message) {

    }

    @CommandMethod
    private void dMalloc(String id) throws IOException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        ClientMessage message = new ClientMessage("dMalloc", getId(), id);
        channel.send(message);
    }
    @CommandMethod
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
    @CommandMethod
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
    @CommandMethod
    private void dRelease(String variableId) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dRelease", variableId);
        channel.send(message);
        ServerMessage serverMessage = (ServerMessage) channel.recv();
    }
    @CommandMethod
    private void dFree(String id) throws IOException, ClassNotFoundException{
        ClientMessage message = new ClientMessage("dFree", id);
        channel.send(message);
        ServerMessage serverMessage = (ServerMessage) channel.recv();
    }

}
