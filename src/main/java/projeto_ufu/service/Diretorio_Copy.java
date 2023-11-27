package projeto_ufu.service;

import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

//import ping.Fd_Disaster;
import projeto_ufu.principal.CoAPController;

public class Diretorio_Copy {

    static DecimalFormat decimalFormat = new DecimalFormat("0.0######");
    
    /*Map para armazenar a última linha processada de cada arquivo*/
    static Map<String, Integer> lastProcessedLineMap = new HashMap<>();
    
    /*Map para armazenar estatísticas para cada arquivo - contendo mínimo, máximo, média e contagem total de elementos*/
    static Map<String, Object[]> statsMap = new HashMap<>();  

    /*Variável para armazenar o caminho da pasta*/
    static String folder;

    public static void leituraDiretorio(long getTimeEnvioMensagem) {

        if (Diretorio.e_Windows()) {
            folder = CoAPController.path5;
        } else {
            folder = CoAPController.path6;
        }

        /** Obter o diretório mais recente */
        File diretorioRecente = Arrays.stream(new File(folder).listFiles(File::isDirectory))
                .max(Comparator.comparing(File::lastModified))
                .orElse(null);
        
        /** Inicializar map para armazenar as linhas de cada arquivo */
        Map<String, List<String[]>> fileContentMap = new HashMap<>();

        /** Ler o conteúdo de cada arquivo com extensão *.OML na pasta consumption */
        // String consumptionPath = new File(diretorioRecente, "consumption").getAbsolutePath();
        //System.out.println("Diretório consumption encontrado: " + consumptionPath);
        
        /** Copiar arquivos *.oml para a pasta consumption_copy */
        Path origem = Paths.get(diretorioRecente.getAbsolutePath(), "consumption");
        Path destino = Paths.get(diretorioRecente.getAbsolutePath(), "consumption_copy");
        
        try {
        	
       	 /*verifica se o diretório de destino já existe*/
       	  if (!Files.exists(destino)) {
       	  
       		/*cria o diretório de destino se ele não existir*/
       	    Files.createDirectories(destino);
       	  }
       	  
       	     /*exclui todos os arquivos e diretórios dentro do diretório de destino*/
             Files.walk(destino)
                   .sorted(Comparator.reverseOrder())
                   .map(Path::toFile)
                   .forEach(File::delete);

            /*cria o diretório de destino (se não existir)*/
            Files.createDirectories(destino);
            
            /*copia os arquivos*/
            Files.walk(origem).forEach(source -> {
               try {
                     Path copiar_destino = destino.resolve(origem.relativize(source));
                     Files.copy(source, copiar_destino,StandardCopyOption.REPLACE_EXISTING);                    
               } 
               catch (IOException e) {
                   e.printStackTrace();
               }
           });
       } catch (IOException e) {
           e.printStackTrace();
       }

        /*Lê os arquivos com extensão .oml no diretório de consumo*/
        try {
        	
            String consumptionPath = destino.toAbsolutePath().toString();

            File consumptionDir = new File(consumptionPath);
            File[] files = consumptionDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".oml"));

            for (File file : files) {
                List<String[]> lines = new ArrayList<>();

                /*Obtém a última linha processada para este arquivo, ou 0 se não houver registro anterior*/
                int lastProcessedLine = lastProcessedLineMap.getOrDefault(file.getName(), 0);

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String linha;
                    int numeroLinha = 1;
                    
                    /*Lê cada linha do arquivo*/
                    while ((linha = reader.readLine()) != null) {
                    	/*Processa a linha somente se for após a última linha processada e após a Nona linha*/
                        if (numeroLinha > lastProcessedLine && numeroLinha >= 10) {
                            String[] values = linha.split("\t");
                            lines.add(values);
                        }
                        numeroLinha++;
                    }                    
                    lastProcessedLineMap.put(file.getName(), numeroLinha);
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }

                /*Armazena no mapa o nome do arquivos com suas respectivas linhas do arquivo lido*/
                fileContentMap.put(file.getName(), lines);
            }

            for (Map.Entry<String, List<String[]>> entry : fileContentMap.entrySet()) {

                String nome_arquivo = entry.getKey();

                int totalValoresAnteriores = 0;
                double valor_minimo_previo = Double.MAX_VALUE, valor_maximo_previo = Double.MIN_VALUE, valor_medio_previo = 0;

                if (statsMap.containsKey(nome_arquivo)) {
                    Object[] previousValues = statsMap.get(nome_arquivo);
                    valor_minimo_previo = (double) previousValues[0];
                    valor_maximo_previo = (double) previousValues[1];
                    valor_medio_previo = (double) previousValues[2];
                    totalValoresAnteriores = (int) previousValues[3];
                }

                List<String[]> linhas = entry.getValue();
                List<Double> quintaColuna = new ArrayList<>();
                
                for (String[] values : linhas) {
                	
                    if (values.length >= 5) {
                        try {
                              double column5Value = Double.parseDouble(values[5]);
                              quintaColuna.add(column5Value);
                        } 
                        catch (NumberFormatException e) {
                              System.err.println("Erro ao converter o valor: " + values[5] + " no arquivo " + nome_arquivo);
                        } 
                        catch (ArrayIndexOutOfBoundsException e) {
                              System.err.println("Índice fora dos limites no arquivo " + nome_arquivo);
                        }
                    } 
                    else {
                              System.err.println("Linha com menos de 6 colunas no arquivo " + nome_arquivo);
                         }
                }

                if (!quintaColuna.isEmpty()) {
                    double valor_minimo_novo = quintaColuna.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                    double valor_maximo_novo = quintaColuna.stream().mapToDouble(Double::doubleValue).max().orElse(0);

                    int totalNovosValores = quintaColuna.size();
                    double somaNovosValores = quintaColuna.stream().mapToDouble(Double::doubleValue).sum();

                    double valor_minimo = Math.min(valor_minimo_previo, valor_minimo_novo);
                    double valor_maximo = Math.max(valor_maximo_previo, valor_maximo_novo);
                    double valor_medio = (totalValoresAnteriores * valor_medio_previo + somaNovosValores) / (totalValoresAnteriores + totalNovosValores);

                    Object[] currentValues = {valor_minimo, valor_maximo, valor_medio, totalValoresAnteriores + totalNovosValores};
                    statsMap.put(nome_arquivo, currentValues);

                    double ultimoValor = quintaColuna.get(quintaColuna.size() - 1);
                    String arquivo_estatistica = "estatistica_consumo.txt";
                    escreverEstatisticas(arquivo_estatistica, nome_arquivo, valor_minimo, valor_maximo, valor_medio, ultimoValor, getTimeEnvioMensagem);
               }
              }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void escreverEstatisticas(String arquivo_estatistica, String nome_arquivo, double valor_minimo,
    	    double valor_maximo, double valor_medio, double ultimoValor, long getTimeEnvioMensagem) {
    	    	
    	    	String caminho_SO = Diretorio.caminho_SO();
    	    	
    	      try (
    	                FileWriter writer = new FileWriter( caminho_SO + arquivo_estatistica, true)) {
    	                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	                String date = dateFormat.format(new Date());
    	                String line =
    	                    nome_arquivo + ";"
    	                    + decimalFormat.format(valor_minimo) + ";"
    	                    + decimalFormat.format(valor_maximo) + ";"
    	                    + decimalFormat.format(valor_medio) + ";"
    	                    + decimalFormat.format(ultimoValor) + ";"
    	                    + getTimeEnvioMensagem + ";"
    	                    + date + "\n";
    	                writer.write(line);
    	                //System.out.println("Estatísticas salvas em " + arquivo_estatistica);
    	      }
    	      catch (IOException e) {
    	            //System.err.println("Erro ao salvar estatísticas em " + arquivo_estatistica);
    	        }
   }
    	   
    public static boolean e_Windows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().startsWith("windows");
    }

    public static String caminho_SO() {

        String caminho_Log_Coap;

        if (Diretorio.e_Windows()) {
            caminho_Log_Coap = CoAPController.path5;
        } else {
            caminho_Log_Coap = CoAPController.path7;
        }

        return caminho_Log_Coap;
    }
}
