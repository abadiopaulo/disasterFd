package projeto_ufu.metricas;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import projeto_ufu.principal.CoAPController;
import projeto_ufu.service.Diretorio;


public class Metricas {
	
	/* É o erro mais recentemente registrado para um dispositivo.*/
	private Map<Integer, Long> erroAnterior = new HashMap<>();
	private Map<Integer, Boolean> dispositivoFuncionando = new HashMap<>();
    private Map<Integer, Integer> contadorFalhas = new HashMap<>(); 
    
    public  static Map<Integer, Integer> errosDispositivo = new HashMap<>();
    public  static Map<Integer, Integer> acertosDispositivo = new HashMap<>();
    private static  Map<Integer, Integer> ultimoErroRegistrado = new HashMap<>();
        
    public Metricas() {}

        
    /* Método calcular TD - Tempo de Detecção 
     * verifica se um dispositivo estava funcionando anteriormente e, se sim, calcula o Tempo de Detecção (TD) de falha,
     * registra o erro, verifica se houve duas falhas consecutivas e, se for o caso, calcula o (TMR).
     * Por fim, atualiza o estado do dispositivo para "não funcionando".
     * */
   public void calcularTD(int dispositivoId, long tFalha, long tUltimoHeartbeat_Trusted) throws IOException {
    	
        /*Se o dispositivo estava funcionando anteriormente*/
       // if (dispositivoFuncionando.getOrDefault(dispositivoId, true)) {

        	long td = tFalha - tUltimoHeartbeat_Trusted;
            registrarTD(dispositivoId, tFalha, tUltimoHeartbeat_Trusted, td);
            
            /*Incrementa o contador de falhas para o dispositivo*/
            contadorFalhas.put(dispositivoId, contadorFalhas.getOrDefault(dispositivoId, 0) + 1);
            
            /*Contador de Erros - Mensagens Faltantes*/
            contabilizarErro(dispositivoId);
            
            /*Registra o Erro no arquivo de Log*/
              historicoDispositivo(dispositivoId,tFalha,errosDispositivo.get(dispositivoId),"contadorErrosDispositivo");
            
           /*Se houver duas falhas consecutivas, calcule o TMR*/
            if (contadorFalhas.get(dispositivoId) == 2) {
            	calcularTMR(dispositivoId,tFalha);
            	
                /*Redefina o contador de falhas para o dispositivo*/
                contadorFalhas.put(dispositivoId, 0);
            }
            
            erroAnterior.put(dispositivoId, tFalha);
        //}       
        
        /*Atualizamos o estado do dispositivo para 'não funcionando'*/
        //dispositivoFuncionando.put(dispositivoId, false);
   }
  
   private void registrarTD(int dispositivoId, long tFalha, long tUltimoHeartbeat_Trusted ,long td) throws IOException {
       
    	String nomeArquivo = Diretorio.caminho_SO() + "TD.csv";
        
    	try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivo, true))){
        
    	 writer.write( dispositivoId +";"+millissegundosData(tFalha)+";"+millissegundosData(tUltimoHeartbeat_Trusted)+";"+td+"\n");    		
        }   	
       
   }
    
   /*  O método caclularTMR - Determina o tempo entre dois erros consecutivos cometidos pelo dispositivo
    *  com base no momento atual e no último erro registrado . Além de registrar esse método 
    *  limpa o registro do último erro do dispositivo.*/
   public void calcularTMR(int dispositivoId, long tempoAtual) throws IOException {

      	long tmr = tempoAtual - erroAnterior.get(dispositivoId);
      	registrarTMR(dispositivoId, tempoAtual, erroAnterior.get(dispositivoId), tmr);

        /*Remova o registro do erro anterior após calcular o TMR*/
        erroAnterior.remove(dispositivoId);
   }
    
   public void registrarTMR(int dispositivoId,  long tempoAtual, long tempoAnterior, long tmr) throws IOException {
    	
    	String nomeArquivo = Diretorio.caminho_SO() + "TMR.csv";
    	
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivo, true))) {
            writer.write(dispositivoId + ";" + millissegundosData(tempoAtual) + ";" + millissegundosData(tempoAnterior) + ";" + tmr + "\n");
        } 
   }
    
   
   /*  O método calcularTM - Duração de um erro (Mistake duration - TM). Representa o tempo que o dispositivo permanece
    *  em situação de erro. Ele pega o último hertbeat recebido ou seja o momento em que o dispositivo volta a funcionar
    *  e faz a subtração do tempo em que foi registrado a última falha para o dispositivo.*/   
   public void calcularTM(int dispositivoId, long tFalha,long tUltimoHeartbeat_Trusted) throws IOException{
	   
	   if( erroAnterior.containsKey(dispositivoId) ) {
	     
		   long tm = tUltimoHeartbeat_Trusted - tFalha;	   
		   
		   registrarTM(dispositivoId, tUltimoHeartbeat_Trusted, tFalha, tm);
	   }
	   	   
	   /*Contador de Acertos - Mensagens Recebidas*/
	   contabilizarAcerto(dispositivoId);
	   
	   /*Registra o Acerto no arquivo de Log*/
	     historicoDispositivo(dispositivoId,tUltimoHeartbeat_Trusted,acertosDispositivo.get(dispositivoId),"contadorAcertosDispositivo");
   }
   
   public void registrarTM(int dispositivoId,  long tFalha, long tUltimoHeartbeat_Trusted, long tm) throws IOException {
   	
   	String nomeArquivo = Diretorio.caminho_SO() + "TM.csv";
   	
       try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivo, true))) {
           writer.write(dispositivoId + ";" + millissegundosData(tUltimoHeartbeat_Trusted) + ";" + millissegundosData(tFalha) + ";" + tm + "\n");
       } 
   }
   
   public void contabilizarErro(int dispositivoId) {
	   
       errosDispositivo.put(dispositivoId, errosDispositivo.getOrDefault(dispositivoId, 0) + 1);
   }
   
   public void contabilizarAcerto(int dispositivoId) {
	   
	   acertosDispositivo.put(dispositivoId, acertosDispositivo.getOrDefault(dispositivoId, 0) + 1);
   }
   
   public double calcularMediaErros(int dispositivoId) {    
	   
	   int idMensagemInt = CoAPController.lista_topicos.get(dispositivoId).getIdMensagem();


       return (double) errosDispositivo.get(dispositivoId) / idMensagemInt;
   }
   
    
   public void historicoDispositivo(int dispositivoId, long dataRegistro, int quantidade, String tipoRegistro) throws IOException {
	    
	    String nomeArquivo = Diretorio.caminho_SO() + tipoRegistro + ".txt";
	        
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivo, true))) {
	        writer.write(dispositivoId + ";" + dataRegistro + ";" + quantidade + "\n");
	    }
	}

   
  /* public static void contadorErrosTemporal() throws IOException {
	   
	    String nomeArquivoErro = Diretorio.caminho_SO() + "contadorErrosTemporal.csv"; 

	    for (Integer dispositivoId : errosDispositivo.keySet()) {
	    	
	        if (!ultimoErroRegistrado.containsKey(dispositivoId) || !ultimoErroRegistrado.get(dispositivoId).equals(errosDispositivo.get(dispositivoId))) {
	        
	        	// Se não houver valor anterior ou se o valor anterior for diferente do valor atual, registre.
	            System.err.println("Caminho do arquivo .." + nomeArquivoErro);
	            try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivoErro, true))) {
	                writer.write(dispositivoId + ";" + errosDispositivo.get(dispositivoId) + ";" + System.currentTimeMillis() + "\n");
	            }
	            //Atualize o último valor registrado.
	            ultimoErroRegistrado.put(dispositivoId, errosDispositivo.get(dispositivoId));
	        } 
	        else {
	               //Se o valor não mudou (erro não aumentou), escreva uma mensagem indicando isso.
	               try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivoErro, true))) {
	                writer.write(dispositivoId + ";Sem novos erros;" + System.currentTimeMillis() + "\n");
	               }
	             }
	    }
   }*/
   
   private static String millissegundosData(long lCDateTime) {

   	 DateFormat simple = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
 
   	 Date result = new Date(lCDateTime);

   	 return simple.format(result);
   }
    
}
