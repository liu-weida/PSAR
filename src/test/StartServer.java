package test;

import machine.Server;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class StartServer {
    public static void main(String[] args) throws IllegalAccessException, InvocationTargetException, IOException, ClassNotFoundException, NoSuchMethodException {
        ServerProcessor serverProcessor = new ServerProcessor();
        Server server = new Server(8080, "Server", serverProcessor);
        serverProcessor.setServer(server);
        Thread t = new Thread(server::start);
        t.start();
        // client2.respond();
        // client.write
        // client2.read(
        // Sot(compter)
    }
}