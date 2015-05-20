package nl.ordina.tacos.boundary;

import javax.annotation.PostConstruct;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
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
        Observable<JsonArray> tacos = getRandomTaco()
                .mergeWith(getRandomTaco()) // Can be different services
                .reduce(Json.createArrayBuilder(), (result, taco) -> result.add(taco))
                .map(builder -> builder.build());

        Subscription subscription = tacos.subscribe(
                result -> {
                    System.out.println("lol");
                    response.resume(result);
                }, //result.add(taco),
                error -> response.resume(error),
                () -> System.out.println("foo") //response.resume(result.build())
        );
    }

    private Observable<JsonObject> getRandomTaco() {
        return Observable.from(
            tacoProvider.request()
                .accept(MediaType.APPLICATION_JSON)
                .async()
                .get(JsonObject.class)
        );
    }

}
