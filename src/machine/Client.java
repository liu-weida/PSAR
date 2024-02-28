package machine;

import utils.channel.Channel;

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
    public void respond() {

    }

    //
    private void dMalloc(int size, String id){

    }

    private int dAccessWrite(String id){
        return 1;
    }

    private void dRelease(Object obj){

    }
    private void dFree(String id){

    }

}
