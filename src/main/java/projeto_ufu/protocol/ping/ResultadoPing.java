package projeto_ufu.protocol.ping;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import projeto_ufu.service.Diretorio;

public class ResultadoPing {
	
	private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
	
	private static final ConcurrentHashMap<String, AtomicInteger> deviceMID = new ConcurrentHashMap<>();

	public static PingResult ping(String ipv6Address, long timeoutMilli) {
		
		int currentMID = deviceMID.computeIfAbsent(ipv6Address, k -> new AtomicInteger(0)).incrementAndGet();
		
		ProcessBuilder processBuilder;

		if (IS_WINDOWS) {
			processBuilder = new ProcessBuilder("ping", "-n", "1", ipv6Address);
		}
		else {
			processBuilder = new ProcessBuilder("ping", "-c", "1", ipv6Address);
		}

		try {
			  
			  Process process = processBuilder.start();
			  process.waitFor(timeoutMilli, TimeUnit.MILLISECONDS);
			  int exitCode = process.exitValue();
			//   int exitCode = process.waitFor();
			  boolean success = exitCode == 0;
			  criarArquivo(ipv6Address, success);
			  return new PingResult(success, System.nanoTime(), currentMID);
		} 
		catch (Exception e) {
			  criarArquivo(ipv6Address, false);
			  return new PingResult(false, -1, currentMID);
		}
	}

	private static void criarArquivo(String ipv6Address, boolean success) {
	
		String nomeArquivo = Diretorio.caminho_SO() + ipv6Address + ".txt";

		try (FileWriter fileWriter = new FileWriter(nomeArquivo)) {
			fileWriter.write(success ? "1" : "0");
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
 }


