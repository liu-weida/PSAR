package utils.processor;

import utils.message.Message;

import java.io.IOException;
import java.net.Socket;

public interface Processor {


    public Message process(Socket socket) throws IOException, ClassNotFoundException;

}
