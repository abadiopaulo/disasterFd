package projeto_ufu.principal;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.*;
import org.eclipse.californium.core.coap.CoAP.Type;

import projeto_ufu.fd.DisasterFd;
import projeto_ufu.json.converter.Conf_Converter;
import projeto_ufu.json.converter.ListaObjetos_Converter;
import projeto_ufu.json.converter.Url_Converter;
import projeto_ufu.json.dominio.Conf;
import projeto_ufu.json.dominio.Url;
import projeto_ufu.protocol.ping.PingResult;
import projeto_ufu.protocol.ping.ResultadoPing;
import projeto_ufu.util.Tarefas;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CoAPController {

	private MonitorDeThreads monitorDeThreads;
	public  List<Url> lista_topicos;
    public  Conf conf;
    private final Map<String, AtomicInteger> deviceMID = new HashMap<>();
    private final Map<String, AtomicInteger> deviceTokenIds = new HashMap<>();
    
    private final ConcurrentHashMap<String, Long> timestampEnvio = new ConcurrentHashMap<>();

    //private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(lista_topicos.size());
    
    private DisasterFd disasterFd;
        
    private Tarefas tarefas;
    
    private String regionBaseDir;
    
    private ResultadoPing resultadoPing;
    
    //public static String path = "", path1 = "", path2 = "", path3 = "", path4 = "", path5 = "", path6 = "", path7 ="", path8 ="";
    
    public CoAPController(List<Url> lista_topicos, Conf conf,String logCoapTracePath, String statisticPath,
            String relatorioPath, String tempoHeartbeatPath, String regionBaseDir) {
    	
     this.lista_topicos = lista_topicos; 
     this.conf = conf;
     this.regionBaseDir = regionBaseDir;
     this.resultadoPing = new ResultadoPing(regionBaseDir); 

     // Crie o DisasterFd passando os caminhos específicos para a região
     this.disasterFd = new DisasterFd(conf.threshold, conf.margin, conf.windows_size, conf.server,
                           logCoapTracePath, statisticPath, relatorioPath, tempoHeartbeatPath,
                           conf.getTime_envio_mensagem(), regionBaseDir, lista_topicos);
     
     this.tarefas = new Tarefas(this.disasterFd, conf, regionBaseDir);
     
    }
     
    public void start() {
    	
     //Inicialização da variável monitorDeThreads       

    	this.monitorDeThreads = new MonitorDeThreads(this, lista_topicos.size(), regionBaseDir);


     for (Url topico : lista_topicos) {
         monitorDeThreads.agendarTarefa(topico);
     }

     //Inicie todas as tarefas adicionais
     monitorDeThreads.monitorarThreads();         
     tarefas.iniciarTarefas();   

    }

    public Url getTopicoById(String topicoId) {
        return lista_topicos.stream()
                            .filter(t -> t.getAddress().equals(topicoId))
                            .findFirst()
                            .orElse(null);
    }

        
    public void sendCoapRequest(Url topico) {
    	
        if ("coap".equals(topico.getProtocol())) {
        
            try {
            
                    URI uri = new URI(topico.getAddress());
                    CoapClient client = new CoapClient(uri);
                                  
                    /** Use URI.toString() em vez de apenas getAddress(), para obter a combinação única de endereço + caminho */
                     String uniqueDeviceIdentifier = uri.toString();

                    /** Agora, use esse identificador único para gerar o MID */
                    int currentMID = deviceMID.computeIfAbsent(uniqueDeviceIdentifier, k -> new AtomicInteger(0)).incrementAndGet();
                 //   String tokenString = String.valueOf(deviceTokenIds.computeIfAbsent(uniqueDeviceIdentifier, k -> new AtomicInteger(0)).incrementAndGet());

                    Request coapRequest = new Request(CoAP.Code.GET, Type.CON);
                    coapRequest.setMID(currentMID);
                  //  coapRequest.setToken(tokenString.getBytes());
                    topico.setIdMensagem(currentMID);
                    
                    /** Registrar o tempo de envio em nanossegundos da mensagem */
                    String key = topico.getAddress() + "_" + currentMID;
                    
                    timestampEnvio.put(key, System.nanoTime());

                    client.setTimeout(TimeUnit.NANOSECONDS.toMillis(topico.getTimeout_dispositivo()) - System.currentTimeMillis() ); // Estabelece o timeout para a requisição.
                             
                    
                    client.advanced(new CoapHandler() {
                        @Override
                        public void onLoad(CoapResponse response) {
                            processResponse(response, topico, coapRequest);
                        }

                        @Override
                        public void onError() {
                            processError(topico, coapRequest);                            
                        }
                    }, coapRequest);      
                    
             
            } 
            catch (URISyntaxException e) {
                System.err.println("Invalid URI: " + e.getMessage());
            }
        } 
        else {

        	/**Implementação para outro protocolo, ICMP*/
        	
        	String ipv6Address = topico.getAddress();

        	PingResult result = this.resultadoPing.ping(ipv6Address, TimeUnit.NANOSECONDS.toMillis(topico.getTimeout_dispositivo() - System.currentTimeMillis()));
        	
        	topico.setIdMensagem(result.getMID());
        	    		
        	if (result.isSuccessful()) {        	
        		
        	  Long sendTime = this.resultadoPing.getAndRemoveSendTime(ipv6Address, result.getMID());
        	  
              if (sendTime != null) {  
        		  topico.setData_recebimento(result.getTimestamp());
        		  System.out.println("#################MENSAGEM ICMP############################################");
				  System.out.println("ID da mensagem :  " + topico.getIdMensagem());
				  System.out.println("Ping OK. Dispositivo " + topico.getAddress() + " funcionando!");
				  System.out.println("Intervalo Envio..: " + TimeUnit.NANOSECONDS.toMillis(conf.getTime_envio_mensagem()) +"ms");
				  System.out.println("#################FIM MENSAGEM ICMP########################################\n");
				        	        
				  try {
					  disasterFd.execute(topico.getDeviceID(), topico.getData_recebimento(),false,topico.getIdMensagem());
				  } 
				  catch (IOException e) {
				
					e.printStackTrace();
				  }
				disasterFd.logCoapTrace(topico.getDeviceID(), topico.getIdMensagem(),sendTime,topico.getData_recebimento(), conf.alfa);
        	 }
            }        	 
        	else {
        		     
        		     Long sendTime = this.resultadoPing.getAndRemoveSendTime(ipv6Address, result.getMID());        	    
        		     System.err.println("#################FALHA DA MENSAGEM ICMP############################################");
  			         System.err.println("ID da mensagem :  " + topico.getIdMensagem());
  			         System.err.println("Time :  " + result.getTimestamp());
  			         System.err.println("Ping Falhou, Dispositivo " + topico.getAddress() + " parado ou não está funcionando!");
  			         System.err.println("Intervalo Envio..: " + TimeUnit.NANOSECONDS.toMillis(conf.getTime_envio_mensagem()) +"ms");
                     System.err.println("#################FIM DA FALHA MENSAGEM ICMP########################################\n");
                     
                     topico.setData_recebimento(result.getTimestamp()); // Definir -1 em caso de falha
        	               	        
 	        	   try {
 	        		      disasterFd.execute(topico.getDeviceID(),topico.getData_recebimento(), true,topico.getIdMensagem());
				   }
 	        	   catch (IOException e) {
					
					e.printStackTrace();
				   }
     		      disasterFd.logCoapTrace(topico.getDeviceID(), topico.getIdMensagem(),sendTime,topico.getData_recebimento(), conf.alfa);
        	    }
        }
    }

    private void processResponse(CoapResponse response, Url topico, Request coapRequest) {
    	
    	long rTT_Mensagem = 0;
    	
        if (response != null) {
        	
            int responseMID = response.advanced().getMID();
            String responseToken = response.advanced().getTokenString();
            
            int sentMIDVal = deviceMID.get(topico.getAddress()).get();
            String sentToken = coapRequest.getTokenString();

            if (sentMIDVal != responseMID && !sentToken.equals(responseToken)) {
                System.err.println("Erro: MID ou token na resposta não corresponde ao MID ou token enviado!");
                return;
            }
            
            String key = topico.getAddress() + "_" + responseMID;
            
            String sentKey = topico.getAddress() + "_" + sentMIDVal;
            
            if (!key.equals(sentKey)) {
                System.err.println("Chave de resposta não corresponde à chave de envio!");
                System.err.println("Key on send Error: " + sentKey);
                System.err.println("Key on response Error: " + key);
                return;
            }
            else {
                   Long sentTime = timestampEnvio.get(key);            
                   topico.setData_envio(sentTime);
                   topico.setData_recebimento(System.nanoTime());
                
                    timestampEnvio.remove(key); 
                            
                         
                   if (sentTime != null) {
                     rTT_Mensagem = topico.getData_recebimento() - topico.getData_envio();                
                   }
            }
            
            System.out.println("#################COAP############################################");
            System.out.println("ID da mensagem no Received..: " + topico.getIdMensagem());
            System.out.println("URL..: " + topico.getAddress());
            System.out.println("Mensagem..: " + response.getResponseText());
            System.out.println("TOKEN enviado..: " + sentToken);
            System.out.println("TOKEN recebido..: " + responseToken);
            System.out.println("Para o Endereço..: " + topico.getAddress());
            System.out.println("Expected MID: " + sentMIDVal + ", Received MID: " + responseMID);
            System.out.println("Expected Message ID: " + topico.getIdMensagem());
            System.out.println("Tempo Rtt em milissegundos: " + TimeUnit.NANOSECONDS.toMillis(rTT_Mensagem) +"ms");
            System.out.println("Intervalo Envio..: " + TimeUnit.NANOSECONDS.toMillis(conf.getTime_envio_mensagem()) +"ms");
            System.out.println("#################COAP FIM##########################################\n");
            
            try {
				  disasterFd.execute(topico.getDeviceID(), topico.getData_recebimento(),false,topico.getIdMensagem());
			} 
            catch (IOException e) {
			
				e.printStackTrace();
			}
            disasterFd.logCoapTrace(topico.getDeviceID(), topico.getIdMensagem(),topico.getData_envio(),
            		   topico.getData_recebimento(), conf.alfa);
            
        }
    }

    private void processError(Url topico, Request coapRequest) {
    	
    	// Captura o MID e o Token da mensagem enviada
        int sentMID = coapRequest.getMID();
        String sentKey = topico.getAddress() + "_" + sentMID;

        // Verifica se a chave de envio corresponde a um registro em timestampEnvio
        Long sentTime = timestampEnvio.get(sentKey);
        
        if (sentTime == null) {
            System.err.println("Erro: Não há registro de envio correspondente para o MID: " + sentMID);
            return;
        }
    	
    	System.err.println("#################ERROR COAP MENSAGEM#####################################");
        System.err.println("Erro: Não foi possível obter uma resposta do servidor para a URL: " + topico.getAddress());
        System.err.println("MID da mensagem falha: " + coapRequest.getMID());
        System.err.println("TOKEN da mensagem falha: " + coapRequest.getTokenString());
        System.err.println("Intervalo Envio..: " + TimeUnit.NANOSECONDS.toMillis(conf.getTime_envio_mensagem()) +"ms");
        System.err.println("#################FIM ERROR COAP MENSAGEM#################################\n");
                
        //Registra a falha no arquivo de log
        disasterFd.logCoapTrace(topico.getDeviceID(), sentMID, sentTime, -1, conf.alfa); // -1 indica erro
        
        try {
     	       topico.setData_recebimento(System.nanoTime());								   
	    	   disasterFd.execute(topico.getDeviceID(),topico.getData_recebimento(), true, topico.getIdMensagem());
	    } 
        catch (IOException e) {
		      e.printStackTrace();
	   }
        
       // Remove o registro de envio para evitar vazamento de memória
       timestampEnvio.remove(sentKey); 
    } 
   

    public static void main(String[] args) throws IOException {
    	
    	if(args.length < 2) {
            System.out.println("Uso: java -jar SeuJar.jar <baseDirectory> <region1,region2,...>");
            System.exit(1);
        }
        
        // Diretório base para os arquivos
        String baseDir = args[0];
        if(!baseDir.endsWith(File.separator)) {
            baseDir += File.separator;
        }
        
        // Lista de regiões, separadas por vírgula
        String[] regioes = args[1].split(",");
        
        // Para cada região, constrói os caminhos e inicia o monitoramento
        for(String regiao : regioes) {
            try {
                // Define o diretório específico da região
                String regionDir = baseDir + regiao + File.separator;
                File regionFolder = new File(regionDir);
                if(!regionFolder.exists()) {
                    regionFolder.mkdirs();
                }
                
                // Constrói os caminhos dos arquivos para essa região
                String configPath = regionDir + "execplan.json";
                String devicesPath = regionDir + "nos.json";
                String logCoapPath = regionDir + "Log_Coap.csv";
                String relatorioPath = regionDir + "relatorio.txt";
                String statisticPath = regionDir + "statistic.csv";
                // Se necessário, outro arquivo, por exemplo:
                String tempoHeartbeatPath = regionDir + "tempoProximoHeartbeat.csv";
                
                // Carrega a configuração da região
                File configFile = new File(configPath);
                Conf_Converter confConv = new Conf_Converter();
                Conf config = confConv.fromJSON(configFile);
                
                // Carrega a lista de dispositivos da região
                InputStream devicesIS = Files.newInputStream(Paths.get(devicesPath));
                ListaObjetos_Converter<Url> urlConv = new Url_Converter(config.getTimeout_dispositivo());
                List<Url> devices = urlConv.jsonToCollection(devicesIS);
                   
                // Cria a instância do monitor para a região
                // Aqui usamos uma classe wrapper "RegionMonitor" para encapsular os componentes
                MonitorRegiao monitor = new MonitorRegiao(regiao, configPath, devicesPath, logCoapPath, relatorioPath, statisticPath);
                
                // Inicia o monitoramento em uma thread separada
                new Thread(() -> monitor.start()).start();
                
                System.out.println("Monitoramento iniciado para a região: " + regiao);
            } catch (Exception e) {
                System.err.println("Erro ao iniciar monitoramento para a região: " + regiao);
                e.printStackTrace();
            }
        }
     }
    
    /**
     * Método que que retorna o tempo atual em Nanossegundos como um valor long. Esse valor é obtido
     * usando a classe Calendar que representa um calendário com os campos de data e hora, estamos 
     * usando essa Unidade de Tempo para registrar o tempo de envio/recebimento da Requisição COAP e ICMP.
     * @return long - tempoAtualNanossegundos
     */
   	
  	public static long tempoAtualNanossegundos() {	 
  	 
  	    return System.nanoTime();
  	}		
  	
  	/**
  	* Método que que retorna o tempo atual em milissegundos como um valor long. Esse valor é obtido
  	* usando a classe Calendar que representa um calendário com os campos de data e hora.
  	* Estamos usando esse paramentro para armazenar o tempo TD (Tempo de Detecção da Falha) e o 
  	* tUltimoHeartbeat_Trusted (Último Heartbeat Recebido com Sucesso)
  	* @return long - tempoAtualMilissegundos
  	*/
  	 
  	public static long tempoAtualMilissegundos() {
  		
  	Calendar lCDateTime = Calendar.getInstance();

  	return lCDateTime.getTimeInMillis();	  
  	
     } 
    

     /**
     * Método que recebe como parâmetro um valor long representando um tempo em milissegundos e retorna uma String
     * contendo uma representação formatada desse tempo no formato"dd/MM/yyyy HH:mm:ss.SSS", que representa
     * a data no formato dia/mês/ano, seguido de hora:minuto:segundo.milissegundo.
     */
    public static String millissegundosData(long lCDateTime) {

     	DateFormat simple = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

  	Date result = new Date(lCDateTime);

  	return simple.format(result);
    } 
    

}


