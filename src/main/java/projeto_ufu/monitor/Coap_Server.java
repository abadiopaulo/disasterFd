package projeto_ufu.monitor;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;

import projeto_ufu.json.dominio.Conf;

public class Coap_Server {
	
	public static Conf conf;
	
	public static void iniciarServidor(String serverIP ) throws SocketException, UnknownHostException {

	
	/* Niveis de saida de log Depuração (debug) - Informação (info) - Aviso (warn)
	 * Erro (error) - Desligado (off)
	 */
	 System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");	
	
	     
	/*Especifique a porta do servidor CoAP*/
	int serverPort = 5683;

	CoapServer server = new CoapServer();

	/*Configuração do endpoint para IPv4-Ipv6*/
	InetSocketAddress ipv4_6_Endpoint = new InetSocketAddress(serverIP, serverPort);
	
	CoapEndpoint endpoint = new CoapEndpoint.Builder()
	        .setInetSocketAddress(ipv4_6_Endpoint)
	        .build();

	server.addEndpoint(endpoint);
	
	server.add(new HelloWorldResource("helloworld"));
	server.add(new TimezoneResource("timezone"));
	server.add(new HostnameResource("hostname"));
	server.add(new IpAddressResource("ipserver"));
	server.add(new ActiveJar("ativo"));
	
	/*Configurar o nivel de log do Californium para "debug"*/
	//LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
	//Logger californiumLogger = context.getLogger("org.eclipse.californium");
	//californiumLogger.setLevel(ch.qos.logback.classic.Level.OFF);
	
	server.start();

   }
 }	
