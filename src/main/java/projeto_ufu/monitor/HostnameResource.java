package projeto_ufu.monitor;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class HostnameResource extends CoapResource {

  public HostnameResource(String name) {
    super(name);
  }

  @Override
  public void handleGET(CoapExchange exchange) {
    try {
      String hostname = InetAddress.getLocalHost().getHostName();
      exchange.respond(hostname);
    } catch (UnknownHostException e) {
      exchange.respond("Hostname not available");
    }
  }
}
