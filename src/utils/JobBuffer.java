package utils;

import utils.message.Message;
import utils.processor.Processor;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class JobBuffer {
    private HashMap<String, Integer> requestNumMap = new HashMap<>();
    private LinkedList<Message> requestList = new LinkedList<>();
    private ServerProcessor processor;
    private boolean isStopped = false;

    public JobBuffer(ServerProcessor processor) {
        this.processor = processor;
    }

    public HashMap<String, Integer> getRequestNumMap() {
        return requestNumMap;
    }

    public LinkedList<Message> getRequestList() {
        return requestList;
    }

    public int count(String variableId) {
        if (requestNumMap.containsKey(variableId))
            return requestNumMap.get(variableId);
        return -1;
    }

    public void setStopped(boolean isStopped) {
        this.isStopped = isStopped;
    }

    public void insertData(String variableId, Message message) {
        requestList.add(message);
        if (requestNumMap.containsKey(variableId))
            requestNumMap.put(variableId, requestNumMap.get(variableId)+1);
        else
            requestNumMap.put(variableId, 1);
    }

    public Message pop() {
        Message message = requestList.pop();
        requestNumMap.put(message.getVariableId(), requestNumMap.get(message.getVariableId())-1);
        return requestList.pop();
    }

    public void startProcess() throws InterruptedException, IOException {
        while (! isStopped) {
            if (requestNumMap.isEmpty())
                wait(3000);
            else {
                Message message = this.pop();
                processor.process(message);
            }
        }
    }
}
