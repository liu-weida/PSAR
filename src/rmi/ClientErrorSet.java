package rmi;

import utils.enums.ClientState;
import java.rmi.Remote;
import java.rmi.RemoteException;

// Interface RMI pour signaler les erreurs du client
public interface ClientErrorSet extends Remote {
    void setClientError(ClientState clientState) throws RemoteException;
}