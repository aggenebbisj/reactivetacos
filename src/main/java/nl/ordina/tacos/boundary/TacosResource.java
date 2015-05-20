package nl.ordina.tacos.boundary;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import nl.ordina.tacos.entity.Taco;
import rx.Observable;
import rx.Subscription;

@Produces(MediaType.APPLICATION_JSON)
@Path("tacos")
public class TacosResource {

    private static final String BASE_URL = "http://taco-randomizer.herokuapp.com";
    private WebTarget tacoProvider;

    @PostConstruct
    public void init() {
        Client client = ClientBuilder.newClient();
        tacoProvider = client.target(BASE_URL + "/random/?full-taco=true");
    }

    @GET
    public void getTacos(@Suspended AsyncResponse response) {
        Observable<List<Taco>> tacos = getRandomTaco()
                .mergeWith(getRandomTaco()) // Can be different services
                .collect(ArrayList::new, (result, taco) -> result.add(taco));

        Subscription subscription = tacos.subscribe(
                // GenericEntity is a hack to prevent type information loss
                // and keep json marshaller working
                result -> response.resume(new GenericEntity<List<Taco>>(result) {}), 
                error -> response.resume(error),
                () -> System.out.println("Completed.")
        );
    }

    private Observable<Taco> getRandomTaco() {
        return Observable.from(
                tacoProvider.request()
                .accept(MediaType.APPLICATION_JSON)
                .async()
                .get(Taco.class)
        );
    }

}
