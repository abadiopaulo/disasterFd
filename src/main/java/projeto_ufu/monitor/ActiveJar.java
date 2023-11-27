package projeto_ufu.monitor;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ActiveJar extends CoapResource {

    public ActiveJar(String name) {
        super(name);
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        boolean isRunning = isProcessRunning("iot-lab.jar");
        exchange.respond(Boolean.toString(isRunning));
    }

    private boolean isProcessRunning(String processName) {
        String os = System.getProperty("os.name").toLowerCase();
        String line;

        try {
            ProcessBuilder processBuilder;
            if (os.contains("win")) {
                processBuilder = new ProcessBuilder("wmic", "PROCESS", "WHERE", "name LIKE 'java%'", "get", "Commandline");
            } 
            else {
            	processBuilder = new ProcessBuilder("sh", "-c", "ps aux | grep -v grep | grep " + processName);
            }

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            while ((line = reader.readLine()) != null) {
                line = line.toLowerCase().replaceAll("\\s+", ""); 
                
                if (line.contains(processName.toLowerCase())) {
                    return true;
                }
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
