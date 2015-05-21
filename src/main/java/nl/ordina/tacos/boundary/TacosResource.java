package nl.ordina.tacos.boundary;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
		final Observable<List<Taco>> tacos = //
				getRandomTacoFromFuture() //
				.mergeWith(getRandomTacoFromInvocationCallback()) //
				.collect(ArrayList::new, (list, taco) -> list.add(taco));
		tacos.subscribe(
				// GenericEntity is a hack to prevent type information loss
				// and keep json marshaller working
		// Also, inlining the local variable tacos leads to problems due to Java
		// generics type erasure
				result -> response.resume(new GenericEntity<List<Taco>>(result) {}),
				error -> response.resume(error));
	}

	private Observable<Taco> getRandomTacoFromFuture() {
		return Observable.from(
				tacoProvider.request()
				.accept(MediaType.APPLICATION_JSON)
				.async()
				.get(Taco.class)
				);
	}

	public Observable<Taco> getRandomTacoFromInvocationCallback() {
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
}
