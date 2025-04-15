package projeto_ufu.util;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import projeto_ufu.fd.DisasterFd;
import projeto_ufu.json.dominio.Conf;
import projeto_ufu.service.Diretorio;

public class Tarefas {

	private static final Logger logger = LoggerUtil.getLogger(Tarefas.class);
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    private final DisasterFd disasterFd;
    
    private final String regionBaseDir;
    
    private final Conf conf; 
                
    public Tarefas(DisasterFd disasterFd, Conf conf, String regionBaseDir) {
        this.disasterFd = disasterFd;  
        this.conf = conf;
        this.regionBaseDir = regionBaseDir;
    }

    public void iniciarTarefas() {
       
    	agendarLeituraDiretorio();
        agendarEstatisticas();
        //registrarMetricas();
    }

    private void agendarLeituraDiretorio() {
    	scheduler.scheduleAtFixedRate(() -> {
            try {
                
            	   // Executa a leitura do diretório apenas se a região for local (alfa == true)
                   if (conf.getAlfa()) {                	   
                     logger.info("Executando leitura do diretório para a região LOCAL: " + conf.getRegionName());  
                     Diretorio.leituraDiretorio(conf.getTime_envio_mensagem(), regionBaseDir);
                   }                
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, conf.getTime_consumo_energia(), TimeUnit.MINUTES);
    }

    private void agendarEstatisticas() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                disasterFd.realizarEstatistica();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, conf.getTime_analiseRede(), TimeUnit.MINUTES);
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
