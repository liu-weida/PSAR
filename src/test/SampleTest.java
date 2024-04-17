package test;

import machine.Client;
import machine.Server;
import utils.channel.Channel;
import utils.channel.ChannelBasic;
import utils.processor.ServerProcessor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.*;

public class SampleTest {
        public static void main(String[] args) throws IOException {
            Client client1 = new Client(6060, "1");
            Client client2 = new Client(6061, "2");
            client1.setObject("test", 10);
            client2.setObject("test", 2);

            ExecutorService executor = Executors.newFixedThreadPool(2);

            // 线程A：执行malloc函数后睡眠10秒再执行release函数
            Future<?> futureA = executor.submit(() -> {
                try {
                    client1.request("dMalloc", "test");
                    client1.request("dAccessWrite", "test");
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                try {
                    Thread.sleep(10000); // 睡眠10秒
                } catch (InterruptedException e) {
                    System.out.println("Thread A was interrupted.");
                    return;
                }

                try {
                    client1.request("dRelease", "test");
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

            });

            // 线程B：执行write函数后睡眠10秒再进行write
            Future<?> futureB = executor.submit(() -> {
                try {
                    Thread.sleep(10000); // 睡眠10秒
                } catch (InterruptedException e) {
                    System.out.println("Thread A was interrupted.");
                    return;
                }

                try {
                    client2.request("dAccessWrite", "test");
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                try {
                    Thread.sleep(10000); // 睡眠10秒
                } catch (InterruptedException e) {
                    System.out.println("Thread B was interrupted.");
                    return;
                }
                try {
                    client2.request("dAccessWrite", "test");
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

            });

            // 关闭线程池
            executor.shutdown();

            try {
                // 等待线程A和线程B执行完毕
                futureA.get();
                futureB.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

        }

}