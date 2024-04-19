package utils.tools;

import utils.channel.Channel;
import utils.message.ClientMessage;

import java.util.*;

public class Buffer {
    private List<Pair> jobList = new ArrayList<>();
    private HashMap<String, Integer> countMap = new HashMap<>();
    private int curr = 0;

    public void insert(ClientMessage message, Channel channel) {
        jobList.add(new Pair(message, channel));
        countMap.compute(message.getVariableId(),
                (key, oldValue) -> (oldValue == null) ? 1 : oldValue + 1);
    }

    public void remove(String key) {
        Iterator<Pair> it = jobList.iterator();
        while (it.hasNext()){
            Pair pair = it.next();
            ClientMessage cm = (ClientMessage) pair.first();
            if(cm.getVariableId().equals(key)){
                it.remove();
            }
        }
        countMap.remove(key);
    }

    public List<Pair> getJobList() {
        return jobList;
    }

    public synchronized void changeCurr(){
        if (jobList.size() == curr)
            curr = 0;
        else
            curr += 1;
    }

    public int getCurr() {
        return curr;
    }

    @Override
    public String toString() {
        StringBuilder str = null;
        for (Pair pair: jobList)
            str.append(pair.first().toString());
        return (str != null) ? str.toString() : null;
    }
}
