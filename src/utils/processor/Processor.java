package utils.processor;

import utils.channel.Channel;
import utils.exception.ServerException;
import utils.message.Message;

import java.io.IOException;
import java.net.Socket;

public interface Processor {
    Message process(Channel channel, String id) throws IOException, ClassNotFoundException, ServerException;
}
