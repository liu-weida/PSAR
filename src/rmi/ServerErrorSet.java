package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import utils.enums.ServerState;

// Interface RMI pour la gestion des erreurs serveur
public interface ServerErrorSet extends Remote {
    void setServerError(ServerState serverState) throws RemoteException;
}