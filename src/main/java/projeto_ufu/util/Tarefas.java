package projeto_ufu.util;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import projeto_ufu.fd.DisasterFd;
import projeto_ufu.metricas.Metricas;
import projeto_ufu.principal.CoAPController;
import projeto_ufu.service.Diretorio;

public class Tarefas {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    
    private final DisasterFd disasterFd;
                
    public Tarefas(DisasterFd disasterFd) {
        this.disasterFd = disasterFd;        
    }

    public void iniciarTarefas() {
       
    	agendarLeituraDiretorio();
        agendarEstatisticas();
        //registrarMetricas();
    }

    private void agendarLeituraDiretorio() {
    	scheduler.scheduleAtFixedRate(() -> {
            try {
                
                Diretorio.leituraDiretorio(CoAPController.conf.getTime_envio_mensagem());
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, CoAPController.conf.getTime_consumo_energia(), TimeUnit.MINUTES);
    }

    private void agendarEstatisticas() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                disasterFd.realizarEstatistica();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, CoAPController.conf.getTime_analiseRede(), TimeUnit.MINUTES);
    }
    
   /* private void registrarMetricas() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Metricas.contadorErrosTemporal();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }, 1, CoAPController.conf.getAlfa(), TimeUnit.MINUTES);
    }   
*/

}
