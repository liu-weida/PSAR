package test;

import machine.Server;
import utils.exception.ServerException;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class StartServer {
    public static void main(String[] args) throws ServerException, ClassNotFoundException {
        ServerProcessor serverProcessor = new ServerProcessor();
        Server server = new Server(8080, "Server", serverProcessor);
        serverProcessor.setServer(server);
        server.start();
    }
}