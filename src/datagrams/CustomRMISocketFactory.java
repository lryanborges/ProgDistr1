package datagrams;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

public class CustomRMISocketFactory implements RMIClientSocketFactory, RMIServerSocketFactory {

    private final InetAddress address;

    public CustomRMISocketFactory(InetAddress address) {
        this.address = address;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(address, port);
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port, 0, address);
    }

    public static void main(String[] args) throws UnknownHostException, RemoteException {
        InetAddress address = InetAddress.getByName("26.95.199.60");
        CustomRMISocketFactory socketFactory = new CustomRMISocketFactory(address);

        Registry registry = LocateRegistry.createRegistry(5000, socketFactory, null);
        System.out.println("Created RMI registry on " + address + ":" + 5000);
    }
}
