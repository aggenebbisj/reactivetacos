package nl.ordina.tacos.boundary;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import rx.Subscription;
import rx.schedulers.Schedulers;

@Stateless
@ServerEndpoint("/sockets/tacos")
public class TacosSocket {
    private static final ConcurrentMap<Session, Subscription> clients = new ConcurrentHashMap<>();
    
    @Inject
    private TacosResource tacoSource;
    
    @OnOpen
    public void connect(Session client) {
        System.out.println("Opening session for client: " + client.getId());
        System.out.println("connect: " + Thread.currentThread().getName());
        
        Subscription subscription = tacoSource.infiniteTacos()
//                .take(10)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    taco -> {
//                        System.out.println("subscription: " + Thread.currentThread().getName());
                        client.getAsyncRemote().sendText(taco.toString()); 
                    },
                    error -> System.out.println("An error occurred")
        );
        
        System.out.println("Registered subscription for client " + client.getId());
        clients.put(client, subscription);
    }

    @OnMessage
    public void onMessage(String msg, Session client) {
        System.out.println("Received message from client " + client.getId());
        System.out.println("Unsubscribing client " + client.getId());
        clients.get(client).unsubscribe();
    }
    
    @OnClose
    public void close(Session client) {
        try {
            System.out.println("Unsubscribing client " + client.getId());
            Subscription s = clients.remove(client);
            if (s != null) {
                s.unsubscribe();
                client.close();
            }   
            System.out.println("Session closed!");
        } catch (IOException ex) {
            Logger.getLogger(TacosSocket.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
