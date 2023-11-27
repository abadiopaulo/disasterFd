package projeto_ufu.monitor;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

//import ping.Fd_Disaster;
import projeto_ufu.json.dominio.Conf;
import projeto_ufu.principal.CoAPController;

/*
 * A classe Client é responsável por implementar a lógica do cliente em um sistema de monitoramento. 
 * Essa classe faz uso de sockets para estabelecer uma conexão com um servidor remoto e enviar mensagens
 * periódicas para verificar se o servidor está respondendo corretamente.
 * */
public class Client {

 /*PING_INTERVAL define o intervalo de tempo em milissegundos entre cada envio de mensagem de ping para o servidor.*/
 public static final long PING_INTERVAL = 5000; // 5 segundos
 
 /*TIMEOUT define o tempo máximo de espera em milissegundos para uma resposta do servidor.*/
 public static final long TIMEOUT = 20000; // 20 segundos
    
 /*
  * O método clientMonitor é o ponto de entrada para o monitoramento dos servidores. Ele recebe um parâmetro cliente 
  * que é utilizado para definir a porta de conexão com o servidor. O método cria um pool de threads que serão responsáveis 
  * por monitorar os servidores. 
  * 
  * */   
 public void clientMonitor(int cliente) {
        ExecutorService executorService = Executors.newFixedThreadPool(CoAPController.conf.outros_monitores.length);

        for (Conf.HostMonitor hostMonitor : CoAPController.conf.outros_monitores) {
            executorService.execute(() -> {

            	boolean connected = false;
                boolean serverResponded = false;
                
                Socket socket = null;
                PrintWriter pr = null;
                BufferedReader br = null;
                
                String serverAddress = hostMonitor.ip;
                String hostname = hostMonitor.hostname;

                while (true) {
                	
                    try { 
                    	
                    	socket = new Socket(serverAddress, cliente); 
                        socket.setSoTimeout((int) TIMEOUT); // Timeout de 20 segundos
                        pr = new PrintWriter(socket.getOutputStream());
                        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        connected = true;
                        System.out.println("Conexão bem-sucedida com o servidor " + hostname + " (" + serverAddress + ")");

                        /*
                         * O loop while (true) é um loop infinito que executa continuamente o código dentro dele até que uma 
                         * condição de saída seja encontrada ou a execução seja interrompida manualmente. Dentro do loop, o
                         *  código estabelece uma conexão com um servidor, envia mensagens de ping e recebe respostas. 
                         *  Ele verifica e exibe mensagens de acordo com as respostas recebidas. Se ocorrer um timeout 
                         *  de resposta ou uma exceção de IO, ele exibe mensagens correspondentes. O loop continua executando 
                         *  até que uma condição de saída seja alcançada.
                         * */
                        while (true) {
                            pr.println("ping:"); 
                            pr.flush();

                            try {
                                
                            	  String response = br.readLine();
                            	  if ("pong".equals(response)) {
                                      System.out.println("Resposta recebida do servidor em " + hostname + " (" + serverAddress + ")");
                                      serverResponded = true;
                                  } else if (serverResponded) {
                                      System.out.println("Resposta inválida do servidor em " + hostname + " (" + serverAddress + ")");
                                      serverResponded = false;
                                  }
                            } 
                            catch (SocketTimeoutException e) {
                                     System.out.println("Timeout de resposta do servidor em " + hostname + " (" + serverAddress + ")");
                                     break;
                            }

                            try {
                                Thread.sleep(PING_INTERVAL);
                            } catch (InterruptedException e) {
                                System.out.println("Thread interrompida");
                                break;
                            }
                        }

                        pr.close();
                        br.close();
                    } catch (IOException e) {
                        if (connected) {
                            System.out.println("Desconexão do servidor em " + hostname + " (" + serverAddress + ")");
                            connected = false;
                        } 
                        else {
                               System.out.println("Não foi possível se conectar ao servidor em " + hostname + " (" + serverAddress + ")");
                        }
                    }

                    try {
                          Thread.sleep(PING_INTERVAL);
                    } 
                    catch (InterruptedException e) {
                        System.out.println("Thread interrompida");
                        break;
                    }
                }
            });
        }

        // Encerra o pool de threads após as tarefas serem concluídas
        executorService.shutdown();
    }
}
