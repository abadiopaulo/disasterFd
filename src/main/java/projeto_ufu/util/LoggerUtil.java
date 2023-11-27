package projeto_ufu.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

//import ping.Fd_Disaster;
import projeto_ufu.principal.CoAPController;
import projeto_ufu.service.Diretorio;

public class LoggerUtil {
	
	private static String caminho_pasta;
	private static boolean handlerConfigured = false;

	public static synchronized Logger getLogger(Class<?> clazz) {
		
		if (Diretorio.e_Windows()) {
			caminho_pasta = CoAPController.path5;
		} else {
			caminho_pasta = CoAPController.path7;
		}

		if (!handlerConfigured) {
			configureGlobalLogger();
		}

		return Logger.getLogger(clazz.getName());
	}

	private static void configureGlobalLogger() {
		Logger globalLogger = Logger.getLogger("");

		// Verifique se o global logger já possui um FileHandler
		for (Handler handler : globalLogger.getHandlers()) {
			if (handler instanceof FileHandler) {
				// Já está configurado, retorne.
				handlerConfigured = true;
				return;
			}
		}

		try {
			FileHandler fileHandler = new FileHandler(caminho_pasta + "erro_log.txt", true);
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
			globalLogger.addHandler(fileHandler);
			handlerConfigured = true;
		} catch (IOException e) {
			globalLogger.log(Level.SEVERE, "Erro ao configurar o logger", e);
		}
	}
}
