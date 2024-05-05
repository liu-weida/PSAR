package rmi;

import utils.enums.ServerState;
import java.rmi.Remote;
import java.rmi.RemoteException;

// Interface RMI pour arrêt forcé du serveur
public interface ForcedServerShutdown extends Remote {
    void forcedserverShutdown() throws RemoteException;
}