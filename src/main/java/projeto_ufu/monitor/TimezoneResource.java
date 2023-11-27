package projeto_ufu.monitor;

import java.util.TimeZone;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class TimezoneResource extends CoapResource {

  public TimezoneResource(String name) {
    super(name);
  }

  @Override
  public void handleGET(CoapExchange exchange) {
    String tz = TimeZone.getDefault().getID();
    exchange.respond(tz);
  }
}