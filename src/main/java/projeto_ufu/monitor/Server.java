package projeto_ufu.monitor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

//import ping.Fd_Disaster;
import projeto_ufu.json.dominio.Conf;
import projeto_ufu.principal.CoAPController;

/*O código Java fornecido é a implementação de um servidor que monitora a atividade de clientes conectados a ele.*/

public class Server {
    
    /*
     * Constante MAX_WAIT_TIME com o valor de 30.000 milissegundos (30 segundos). 
     * Essa constante representa o tempo máximo de espera antes de considerar um cliente como inativo.
     */
	public static final long MAX_WAIT_TIME = 30000; // 30 segundos
   
    public static String path;
    
    /*clientNames é um mapa que associa o endereço IP dos clientes aos seus nomes.*/
    private Map<String, String> clientNames;
  
    
    /*Essa classe interna estática define a estrutura de dados para armazenar informações de um cliente conectado ao servidor. 
     * Ela possui três campos: 
     * lastPingTime representa o tempo da última comunicação (ping) recebida do cliente.
       isActive indica se o cliente está ativo ou inativo.
       clientName armazena o nome do cliente.
    */
    static class ClientInfo {

    	long lastPingTime;
        boolean isActive;
        String clientName;

        ClientInfo(long lastPingTime, boolean isActive, String clientName) {
            this.lastPingTime = lastPingTime;
            this.isActive = isActive;
            this.clientName = clientName;
        }
    }

    /*clientMap é um mapa que associa o endereço IP dos clientes às informações relevantes sobre cada cliente.*/
    private Map<String, ClientInfo> clientMap = new HashMap<>();
  
    /*O construtor inicializa o mapa clientNames com os endereços IP e nomes dos clientes fornecidos por meio do objeto Fd_Disaster.conf.*/
    public Server() {
        
        clientNames = new HashMap<>();
       
        if (CoAPController.conf != null) {
		 
			  for (Conf.HostMonitor hostMonitor : CoAPController.conf.outros_monitores) {
		        clientNames.put(hostMonitor.ip, hostMonitor.hostname);
		      }
		  }
     }

    /*Esse método é responsável por iniciar o servidor na porta especificada (serverPort). 
     * Ele cria um ServerSocket e inicia uma thread para monitorar os clientes. Em um loop
     * infinito, aguarda conexões de clientes e, para cada conexão recebida, 
     * inicia uma nova thread para lidar com o cliente.
     */    
    public void startServer(int serverPort) {
    	
        try (
        		ServerSocket ss = new ServerSocket(serverPort)) {
                new Thread(this::monitorClients).start();

                while (true) {
                  Socket s = ss.accept();
                  new Thread(() -> handleClient(s)).start();
                }
        } 
        catch (IOException e) {
             e.printStackTrace();
        }
    }
    
    /*Esse método é executado em uma thread separada e é responsável por monitorar a atividade dos clientes
     * conectados. Ele chama o método writeToTextFile e, em seguida, aguarda por MAX_WAIT_TIME 
     * antes de verificar novamente.
     */
    private void monitorClients() {
    	
        while (true) {
        
         writeToTextFile();
            
         try {
                Thread.sleep(MAX_WAIT_TIME); 
             } 
        	 
         catch (InterruptedException e) {
                  e.printStackTrace();
            }
        }
    }

    /* Esse método é responsável por lidar com cada cliente conectado ao servidor. 
     * Ele recebe um objeto Socket representando a conexão com o cliente. Primeiro, ele obtém
     * o endereço IP do cliente e o nome associado a ele no mapa clientNames. Em seguida, ele 
     * adiciona o cliente ao mapa clientMap, definindo as informações iniciais. 
     * Dentro de um loop infinito, o método lê as mensagens recebidas do cliente por meio de um 
     * BufferedReader. Se a mensagem for ping:, o servidor responde com pong para indicar que o 
     * cliente está ativo e atualiza as informações no mapa clientMap. Se o tempo desde o último 
     * ping do cliente exceder MAX_WAIT_TIME, o servidor marca o cliente como inativo no mapa clientMap.
     * Em ambos os casos, o servidor chama o método writeToTextFile para atualizar o arquivo de texto 
     * correspondente ao cliente. Se ocorrer uma exceção SocketException, isso indica que a conexão 
     * com o cliente foi encerrada, e o servidor trata essa situação imprimindo uma mensagem apropriada.
     * No bloco finally, o servidor remove o cliente do mapa clientMap, chama writeToTextFile para atualizar
     *  o arquivo de texto correspondente e fecha a conexão.
     * */   
    private void handleClient(Socket s) {
    	
    String clientIp = s.getInetAddress().getHostAddress();
    String clientName = clientNames.getOrDefault(clientIp, clientIp.replace('.', '_'));
    
    clientMap.put(clientIp, new ClientInfo(System.currentTimeMillis(), true, clientName));

    try {
        
        InputStreamReader in = new InputStreamReader(s.getInputStream());
        BufferedReader bf = new BufferedReader(in);
        PrintWriter pr = new PrintWriter(s.getOutputStream());

        String str;
        
        while (true) {
        
        	try {
                str = bf.readLine();
                if (str == null) {
                    System.out.println("Conexão encerrada pelo cliente: " + clientName);
                    break;
                }

                if ("ping:".equals(str)) {
                    System.out.println("Ping recebido de: " + clientName);
                    clientMap.get(clientIp).lastPingTime = System.currentTimeMillis();
                    clientMap.get(clientIp).isActive = true;

                    pr.println("pong"); 
                    pr.flush();
                }

                if (System.currentTimeMillis() - clientMap.get(clientIp).lastPingTime > MAX_WAIT_TIME) {
                    System.out.println("O cliente " + clientName + " parou de funcionar.");
                    clientMap.get(clientIp).isActive = false;
                }

                writeToTextFile(clientName, clientMap.get(clientIp).isActive);
                
            } 
        	catch (SocketException e) {
                   System.out.println("Conexão encerrada pelo cliente: " + clientName);
                   break;
            }
        }
    }
    catch (IOException e) {
                 e.printStackTrace();
    }
    finally {
                clientMap.remove(clientIp);
                writeToTextFile(clientName, false);
                
                try {
                  s.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
             }
   }

   /*
    * Esse método é responsável por escrever no arquivo de texto correspondente ao cliente. 
    * Ele recebe o nome do cliente e o estado de atividade (isActive) como parâmetros. 
    * Em seguida, ele cria um FileWriter para o arquivo especificado pelo nome do cliente 
    * e escreve "1" se o cliente estiver ativo ou "0" se estiver inativo.
    * Em caso de exceção, uma mensagem de erro é exibida.
    * */
    
   private void writeToTextFile(String clientName, boolean isActive) {

    	String fileName = clientName + ".txt";
        
    	try {
            FileWriter writer = new FileWriter(fileName);
            writer.write(isActive ? "1\n" : "0\n");
            writer.close();
        } 
    	catch (IOException e) {
            System.out.println("Um erro ocorreu ao escrever no arquivo " + fileName);
            e.printStackTrace();
        }
    }

   /*
    * Esse método é uma sobrecarga do método writeToTextFile anterior e é chamado pelo método 
    * monitorClients. Ele percorre o mapa clientMap e atualiza os arquivos de texto para todos 
    * os clientes com base em seu estado de atividade. Ele faz uso das informações armazenadas 
    * no mapa clientMap para determinar se um cliente está ativo ou não e, em seguida, escreve 
    * o valor correspondente ("1" ou "0") no arquivo de texto adequado.
    * */ 
   private void writeToTextFile() {
    	
        for (Map.Entry<String, ClientInfo> entry : clientMap.entrySet()) {
            String clientName = entry.getValue().clientName;
            boolean isActive = System.currentTimeMillis() - entry.getValue().lastPingTime <= MAX_WAIT_TIME;

            String fileName = clientName + ".txt";
            try {
                FileWriter writer = new FileWriter(fileName);
                writer.write(isActive ? "1\n" : "0\n");
                writer.close();
            } catch (IOException e) {
                System.out.println("Um erro ocorreu ao escrever no arquivo " + fileName);
                e.printStackTrace();
            }
        }
    }   
}
