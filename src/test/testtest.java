package test;

import machine.Client;
import utils.channel.Channel;
import utils.channel.ChannelBasic;

import java.io.IOException;
import java.net.Socket;

public class testtest {

    public testtest() throws IOException {
    }

    Channel channel = new ChannelBasic(new Socket("localhost", 8080));
    Client client1 = new Client( 6060,"client1");








}
