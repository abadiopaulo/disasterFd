package projeto_ufu.monitor;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class IpAddressResource extends CoapResource {

	public IpAddressResource(String name) {
		super(name);
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				Enumeration<InetAddress> addresses = ni.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (!addr.isLinkLocalAddress() && !addr.isLoopbackAddress()
							&& addr.getHostAddress().indexOf(":") == -1) {
						exchange.respond(addr.getHostAddress());
						return;
					}
				}
			}
		} catch (SocketException e) {
			exchange.respond("Could not determine IP address: " + e.getMessage());
		}
	}
}