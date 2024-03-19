package utils;

public class Pair {
    private Object o1;
    private Object o2;

    public Pair(Object o1, Object o2){
        this.o1 = o1;
        this.o2 = o2;
    }

    public Object first(){
        return o1;
    }

    public Object second(){
        return o2;
    }

    @Override
    public String toString() {
        return "[" + o1.toString() + ", " + o2.toString() + "]";
    }
}
