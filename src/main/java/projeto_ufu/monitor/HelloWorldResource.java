package projeto_ufu.monitor;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class HelloWorldResource extends CoapResource {

  public HelloWorldResource(String name) {
    super(name);
  }

  @Override
  public void handleGET(CoapExchange exchange) {
    exchange.respond("Hello World");
  }
}