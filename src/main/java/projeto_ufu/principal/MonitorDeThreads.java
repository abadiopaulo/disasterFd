package projeto_ufu.principal;

import java.util.concurrent.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import projeto_ufu.json.dominio.Url;
import projeto_ufu.service.Diretorio;

import java.util.concurrent.ConcurrentHashMap;

public class MonitorDeThreads {
	
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, ThreadState> threadStates = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final CoAPController controller;

     public MonitorDeThreads(CoAPController controller, int threadPoolSize) {
    	
        this.controller = controller;
        this.scheduler = Executors.newScheduledThreadPool(threadPoolSize);
    }

    public void agendarTarefa(Url topico) {

    	String topicoId = topico.getAddress();
    	
    	// Inicializar o estado da thread com lastRun antigo
        ThreadState state = new ThreadState();
        state.setLastRun(System.nanoTime() - TimeUnit.DAYS.toNanos(1));
        threadStates.put(topicoId, state);
        
        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(() -> {

        	threadStates.get(topicoId).setLastRun(System.nanoTime());
            controller.sendCoapRequest(topico);

        }, 0, controller.conf.getTime_envio_mensagem(), TimeUnit.NANOSECONDS);

        scheduledTasks.put(topicoId, scheduledTask);
    }

    public void monitorarThreads() {
        scheduler.scheduleAtFixedRate(this::checkAndRestartThreads, 0, 1, TimeUnit.MINUTES);
    }

    private void checkAndRestartThreads() {
    	
    	String nomeArquivo = Diretorio.caminho_SO() + "logEstadoThreads.txt";
    	
        long threshold = TimeUnit.SECONDS.toNanos(5);

        //Obter a data e hora atuais formatadas
        LocalDateTime now = LocalDateTime.now();
        String formattedDateTime = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivo, true))) {
         
            writer.write("Verificação do estado das threads em " + formattedDateTime + "...\n");

            for (Entry<String, ThreadState> entry : threadStates.entrySet()) {
                String topicoId = entry.getKey();
                ThreadState state = entry.getValue();
                long lastRunDelta = System.nanoTime() - state.getLastRun();

                boolean isThreadActive = lastRunDelta <= threshold;
                String logLine = "Thread ID: " + topicoId + " - Estado: " + (isThreadActive ? "Ativa" : "Inativa") + "\n";
                writer.write(logLine);

                if (!isThreadActive) {
                    restartThread(topicoId);
                }
            }
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void restartThread(String topicoId) {
    	
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(topicoId);
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }

        Url topico = controller.getTopicoById(topicoId);
        if (topico == null) {
            System.err.println("Tópico não encontrado para ID: " + topicoId);
            return;
        }

        agendarTarefa(topico);
    }
    
        
}
