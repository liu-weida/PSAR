package machine;

import utils.message.Message;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface Machine {
    void request(String methodType, List<Object> args) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException, ClassNotFoundException;

    void respond() throws IllegalAccessException, InvocationTargetException, IOException, ClassNotFoundException;


}
