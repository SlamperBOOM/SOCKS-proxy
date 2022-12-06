import proxy.ProxyThread;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args){
        ProxyThread proxyThread;
        try {
            if (args.length > 0) {
                proxyThread = new ProxyThread(Integer.parseInt(args[0]));
                System.out.println("Proxy started at port " + args[0]);
            } else {
                proxyThread = new ProxyThread(1080);
                System.out.println("Proxy started at port 1080");
            }
        }catch (IOException e){
            e.printStackTrace();
            System.out.println("Error while starting proxy. Exiting");
            return;
        }
        proxyThread.start();
        System.out.println("Write \"stop\" or \"quit\" to stop server correctly and safely");
        Scanner scanner = new Scanner(System.in);
        while (true){
            String line = scanner.nextLine();
            if(line.equalsIgnoreCase("stop") || line.equalsIgnoreCase("quit")){
                proxyThread.setStopped();
                break;
            }
        }
    }
}
