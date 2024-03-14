package utils.processor;

import utils.message.Message;

import java.net.Socket;

public interface Processor {


    public Message process(Socket socket);

}
