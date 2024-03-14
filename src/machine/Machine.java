package machine;

import utils.message.Message;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface Machine {
    void request(String methodType, String args) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException, ClassNotFoundException, InstantiationException;

    void respond() throws IllegalAccessException, InvocationTargetException, IOException, ClassNotFoundException;


}
