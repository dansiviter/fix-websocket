# FIX WebSocket #

A simple JSR WebSocket implementation that uses CDI for lifecycle and eventing. For example usage take a look at `uk.dansiviter.fixws.LogonTest`.

There are a number of limitations at the moment:
* This only permit dynamic FIX 5.0 sessions at this time. Ideally there will also be static sessions provider too to limit connecions. In a production system this would likely be centralised in a datastore so it could be dynamically updated,
* FIX specification version is fixed at the moment at 5.0. Both the WebSocket subprotocols and the Logon Handshake should permit different versions, but I just wanted to get this working,
* More testing is required to see if older FIXT 1.1/5.0 protocols work as they are a single spec rather than split,

## Usage ##

Either annotated:

	@ServerEndpoint(value = "/fix", subprotocols = "fix50")
	public class MyFixEndpoint extends FixEndpoint { }

...or via `javax.websocket.server.ServerApplicationConfig`:

	public static class MyApplicationConfig implements ServerApplicationConfig {
		@Override
		public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
			return Set.of(FixEndpoint.config("/fix", List.of(FixVersions.FIX50)));
		}

		@Override
		public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
			return emptySet();
		}
	}


## Receiving Messages ##

A type-safe way:

	public void on(@Observes @FromApp MarketDataRequest req) {
		// ... do something
	}

...or a message-type way:

	public void on(@Observes @FromApp @MsgType(MarketDataRequest.MSGTYPE) Message msg) {
		// ... do something
	}

...or super generic:

	public void on(@Observes @FromApp Message msg) {
		// ... do something
	}


## Sending ##

Either:

	@Inject @ToApp
	private Event<Message> event;
	...
	event.fire(message);

...or:

	@Inject
	private Event<Message> event;
	...
	event.select(ToApp.Literal.toApp()).fire(message);

...or:

	beanManager.fireEvent(message, ToApp.Literal.toApp());


## Metrics ##

| Name                | Type    | Unit | Labels                   |
|---------------------|---------|------|--------------------------|
| `fix/message.count` | Counter | 1    | sessionId, msgType, kind |

## Frequently Asked Questions ##

**I've created a new message, but how does it know where to send it?**

The `quickfix.Message` must have the session it wishes to send to of the CDI event. Use the methods in `uk.dansiviter.fixws.FixUtil` to assist with this.
