package utils.processor;

import utils.channel.Channel;
import utils.exception.ServerException;
import utils.message.Message;

import java.io.IOException;
import java.net.Socket;

public interface Processor {
    Void process(Channel channel, String id) throws IOException, ClassNotFoundException, ServerException;
}
