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
	private static final ConcurrentHashMap<String, Long> pingSendTime = new ConcurrentHashMap<>();

	public static PingResult ping(String ipv6Address, long timeoutMilli) {
		
		int currentMID = deviceMID.computeIfAbsent(ipv6Address, k -> new AtomicInteger(0)).incrementAndGet();		
		
		long sendTimePing = System.nanoTime(); // Registrar o tempo de envio aqui
		
	    registerSendTime(ipv6Address, currentMID, sendTimePing); // Adicionar essa linha
		
		ProcessBuilder processBuilder;

		if (IS_WINDOWS) {
			processBuilder = new ProcessBuilder("ping", "-n", "1", ipv6Address);
		}
		else {
			processBuilder = new ProcessBuilder("ping", "-c", "1", ipv6Address);
		}

		/**
		 Modificação no tratamento de falhas no comando ping:
		 Além de capturar exceções, agora verificamos explicitamente se o processo terminou dentro do tempo limite
		 e se o código de saída é zero. Se qualquer uma dessas condições não for satisfeita, tratamos como falha.
		 Nesse caso, retornamos um PingResult com timestamp definido como -1, indicando a falha.
		**/
		
		try {
			  
			  Process process = processBuilder.start();
			  boolean finishedInTime = process.waitFor(timeoutMilli, TimeUnit.MILLISECONDS);
			  int exitCode = process.exitValue();
			  boolean success = exitCode == 0 && finishedInTime;
			  
			  if (!success) {
			        criarArquivo(ipv6Address, false);
			        return new PingResult(false, -1, currentMID);
			  }
			  
			  criarArquivo(ipv6Address, success);
			  return new PingResult(success, System.nanoTime(), currentMID);
		} 
		catch (Exception e) {
			  criarArquivo(ipv6Address, false);
			  return new PingResult(false, -1, currentMID);
		}
	}
	
	public static void registerSendTime(String ipv6Address, int currentMID, long sendTime) {
        pingSendTime.put(ipv6Address + "_" + currentMID, sendTime);
    }

    public static Long getAndRemoveSendTime(String ipv6Address, int currentMID) {
        return pingSendTime.remove(ipv6Address + "_" + currentMID);
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


