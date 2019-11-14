# FIX WebSocket #

A simple JSR WebSocket implementation that uses CDI for lifecycle and eventing.

> :information_source: This only permit dynamic sessions at this time as no static `SessionProvider` has been implemented.

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


## Frequently Asked Questions ##

**I've created a new message, but how does it know where to send it?**

Use `uk.dansiviter.fixws.Util.set(sessionId, message)` on the object and it'll be extracted and used when the event is fired.
