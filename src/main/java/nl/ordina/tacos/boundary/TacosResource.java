package nl.ordina.tacos.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;

import nl.ordina.tacos.entity.Taco;
import rx.Observable;
import rx.Observer;

@Produces(MediaType.APPLICATION_JSON)
@Path("tacos")
public class TacosResource {

    private static final String BASE_URL = "http://taco-randomizer.herokuapp.com";
    private WebTarget tacoProvider;

    @PostConstruct
    public void init() {
        final Client client = ClientBuilder.newClient();
        tacoProvider = client.target(BASE_URL + "/random/?full-taco=true");
    }

    @GET
    public void getTacos(@Suspended AsyncResponse response) {
        getRandomTacoFromFuture() //
                .mergeWith(getRandomTacoFromInvocationCallback()) //
                .collect(ArrayList<Taco>::new, (list, taco) -> list.add(taco)).subscribe(
                        // GenericEntity is a hack to prevent type information loss
                        // and keep json marshaller working
                        // Also, inlining the local variable tacos leads to problems due to Java
                        // generics type erasure
                        result -> response.resume(new GenericEntity<List<Taco>>(result) {
                        }),
                        error -> response.resume(error));
    }

    @GET
    @Path("{number}")
    public List<Taco> getTacos(@PathParam("number") int number) {
        List<Taco> result = new ArrayList<>();
        
        infiniteTacos().take(number).toList().subscribe(new Observer<List<Taco>>() {

            @Override
            public void onCompleted() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void onError(Throwable e) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void onNext(List<Taco> tacos) {
                result.addAll(tacos);
            }
            
        });
        
        return result;
    }
    
    private Observable<Taco> getRandomTacoFromFuture() {
        return Observable.from(
                tacoProvider.request()
                .accept(MediaType.APPLICATION_JSON)
                .async()
                .get(Taco.class)
        );
    }

    private Observable<Taco> getRandomTacoFromInvocationCallback() {
        return Observable.create(subscriber -> tacoProvider //
                .request() //
                .accept(MediaType.APPLICATION_JSON) //
                .async() //
                .get(new InvocationCallback<Taco>() {
                    @Override
                    public void completed(Taco Taco) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(Taco);
                            subscriber.onCompleted();
                        }
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(throwable);
                        }
                    }
                }));
    }

    public Observable<Taco> infiniteTacos() {
        Stream<Observable<Taco>> tacos = Stream.generate(this::getRandomTacoFromFuture);
        return Observable.from(tacos::iterator).flatMap(taco -> taco);
    }

}
