package projeto_ufu.fd;

import static projeto_ufu.principal.CoAPController.lista_topicos;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import projeto_ufu.json.converter.ListaObjetos_Converter;
import projeto_ufu.json.converter.Url_Converter;
import projeto_ufu.json.dominio.Url;
import projeto_ufu.metricas.Metricas;
import projeto_ufu.principal.CoAPController;
import projeto_ufu.service.Diretorio;
import projeto_ufu.util.LoggerUtil;

/**
 * Classe DisasterFd que verifica a pulsação das requesições solicitadas ao
 * servidor COAP
 * 
 * @author Abadio de Paulo, Prof. Dr. Paulo Coelho
 * @version 1.0
 */
public class DisasterFd {

	protected class DeviceReply {
		protected long mid;
		protected long arrivalTime;

		protected DeviceReply(long mid, long arrivalTime) {
			this.mid = mid;
			this.arrivalTime = arrivalTime;
		}
	}

	/*
	 * A variável Ai é um valor constante que representa o intervalo de tempo entre
	 * os heartbeats (pulsos)
	 * enviados pelos dispositivos na rede.
	 */
	private final long Ai = CoAPController.conf.time_envio_mensagem; // nanoseconds 400000000 = 400 miliseconds - intervalo envio
	private final Queue<DeviceReply>[] A; // Queue ou Fila usado para armazenar tempos de resposta medidos de cada no
	private long EA = 0;
	private String server;
	private int windows_size = 0;// Tamanho da Janela;
	private long margin;
	private int threshold;
	private int alfa;
	private int lin;
	private long trustLevel = 0;

	private final boolean[] trusted;
	
	private Metricas metricas = new Metricas();

	private static final Logger logger = LoggerUtil.getLogger(DisasterFd.class);

	/*
	 * Biblioteca Apache Commons Math, onde especificamente a classe
	 * DescriptiveStatistics é usada
	 * para calcular estatísticas como exemplo (média, desvio padrão, mínimo,
	 * máximo, etc.)
	 */
	private final DescriptiveStatistics samplesDT;

	/*
	 * to - > Tempo para o Timeout - previsão de chegada do próximo heartbeat
	 * tPrevious -> Tempo do último heartbeat que chegou
	 * roundTime -> Tempo de Ida e Volta de um Heartbeat confiável
	 */
	private long[] to, tPrevious, tFalha, tUltimoHeartbeat_Trusted;
	private long error, tEnd, timeoutPrev, timeoutIni, tsIni, mistakeTime;
	private long errorIni = 0, dif, totTime = 0; // ts -> timestamp / tsIni -> initial timestamp / totTime -> total time
	private String status_rede;
	private boolean untrusted = false;

	public DisasterFd(int threshold, long margin, int windows_size, String server) {

		/*
		 * Variável que será usado para calcular estatísticas descritivas para um
		 * conjunto de dados que podem
		 * conter até 45140000 valores.
		 */
		samplesDT = new DescriptiveStatistics(45140000); // estatasticas de tempo de deteccao

		this.windows_size = windows_size;
		this.margin = margin;
		this.threshold = threshold;
		this.server = server;
		this.A = new Queue[lista_topicos.size()];
		this.lin = 1;
		this.error = 0;
		this.tEnd = 0;
		this.timeoutPrev = 0;
		this.timeoutIni = 0;
		this.tsIni = 0;
		this.mistakeTime = 0;
		this.trusted = new boolean[lista_topicos.size()];
		Arrays.fill(this.trusted, false); // Inicializar com Valor True a Lista
		this.errorIni = 0;
		this.dif = 0;
		this.totTime = 0;

		for (int j = 0; j < lista_topicos.size(); j++) {
			this.A[j] = new LinkedList();
		}

		this.to = new long[lista_topicos.size()]; // timeout
		this.tPrevious = new long[lista_topicos.size()]; // previous timeout

		this.tFalha = new long[lista_topicos.size()];
		this.tUltimoHeartbeat_Trusted = new long[lista_topicos.size()];

		for (int j = 0; j < lista_topicos.size(); j++) {
			to[j] = 0;
			tPrevious[j] = 0;
			tFalha[j] = 0;
			tUltimoHeartbeat_Trusted[j] = 0;
		}

	}

	/** Método que verifica o nível de confiança do sistema como um todo */
	public synchronized void execute(int id, long data_recebimento, boolean timeout, int idmensagem)
			throws FileNotFoundException, IOException {
		try {
			if (this.lin == 1) {
				/*
				 * Armazena o Tempo inicial de chegada do primeiro heartbeat ao iniciar o
				 * sistema
				 */
				this.tsIni = data_recebimento;
			}

			if (A[id].size() > 0) {
				this.timeoutIni = to[id];
			} else {
				/* Se a lista que armazena o tempo de chegada dos heartbeat estiver vazia */
				if (this.lin == 1) { // if first line
					timeoutIni = data_recebimento;
				} else {
					timeoutIni = timeoutPrev;
				}
			}

			/*
			 * Caso o Timeout seja verdade não chegou pulsação neste caso o Nó não será
			 * confiável,
			 * estamos armazenando o tFalha (Momento da falha e Milissegundos para calcular
			 * o TD e TMR)
			 */
			if (timeout) {
				trusted[id] = false;
				trustedDispositivo(false, id);

				tFalha[id] = System.currentTimeMillis();

				trustLevel = sumTrusted(); /* Calcule o nível de confiança da rede */

			} else {
				trusted[id] = true;
				trustedDispositivo(true, id);
				//lista_topicos.get(id).setFuncionouPeloMenosUmaVez(true);
			}

			/*
			 * Loop para verificar se o tempo limite de cada um dos nos expirou
			 */
			for (int j = 0; j < lista_topicos.size(); j++) {
				/*
				 * Se o fator de impacto for diferente de 0 e o tempo limite estimado do no for
				 * diferente de 0 e o no for diferente do ID Atual (calcula tb para noh atual) e
				 * o no for confiavel.
				 * Não usar && j != this.getId()
				 */
				if (lista_topicos.get(j).getFactorImpact() != 0 && (to[j] != 0) && j != id && trusted[j]) {
					/*
					 * Se o timestamp atual for maior que o tempo limite estimado de j e o tempo
					 * limite estimado de J for maior que ultimo tempo limite (se ainda nao tiver
					 * sido calculado antes) J confiavel
					 */
					if ((data_recebimento > to[j]) && (to[j] > tEnd)) {
						/* neste caso J considerado suspeito e sai da lista de confiaveis */
						trusted[j] = false;
						trustedDispositivo(false, j);
						trustLevel = sumTrusted(); /* Calcule o nível de confiança da rede */

						if (trustLevel < this.threshold) {
							if (to[j] < this.timeoutIni) {
								this.timeoutIni = to[j]; /* o tempo limite de J e tempo de erro inicial */
							}
						}
					}
				}
			}

			/*
			 * O código verifica se um dispositivo falhou e, se sim, calcula o Tempo de
			 * Detecção (TD).
			 * Se o dispositivo não falhou, ele verifica se o dispositivo estava
			 * anteriormente em falha
			 * e agora voltou a funcionar; se for esse o caso, ele calcula o Tempo de
			 * Manutenção (TM)
			 */
			if (!trusted[id] && timeout
					/*&& tUltimoHeartbeat_Trusted[id] != 0
					&& lista_topicos.get(id).isFuncionouPeloMenosUmaVez()*/) {

				metricas.calcularTD(id, tFalha[id], tUltimoHeartbeat_Trusted[id]);
			} 
			else {
				    tUltimoHeartbeat_Trusted[id] = System.currentTimeMillis();

		            /* Se o dispositivo estava anteriormente em falha e agora está funcionando */
				    //if (!Metricas.dispositivoFuncionando.getOrDefault(id, true)) {
					metricas.calcularTM(id, tFalha[id], tUltimoHeartbeat_Trusted[id]);
 				    //}

			        //Metricas.dispositivoFuncionando.put(id, true);			      
				 }

			if (!timeout) {

				trustLevel = sumTrusted(); /* Calcule o nível de confiança da rede */

				if (trustLevel < this.threshold) { /* Não é Confiável */
					if (!this.untrusted) { /* Se antes o sistema era confiável */
						this.errorIni = this.timeoutIni;
						samplesDT.addValue(getDiference(this.to, this.tPrevious));						 
					}
					this.untrusted = true;

					/*Registrar no CSV* - 100 para "não confiável"*/
					nivelTrustRede(100, System.currentTimeMillis());
				} 
				else {
					   // trusted
					   this.timeoutIni = this.to[id];
					   if ((this.lin != 1) && this.untrusted) {/* Se antes o sistema não era confiável */
						 this.mistakeTime += data_recebimento - this.errorIni; /* Incrementa o tempo do Erro */
						 this.error++;	
					   }
					   this.untrusted = false; /* Neste ponto a rede agora é confiável */
					
					   /*Registrar no CSV* - 200 para "Confiável"*/
					   nivelTrustRede(200, System.currentTimeMillis());
				      }

				this.timeoutPrev = this.timeoutIni;
				this.tEnd = data_recebimento;

				if (A[id].size() == windows_size) {
					A[id].poll();
				}

				A[id].add(new DeviceReply(idmensagem, data_recebimento));
				this.tPrevious[id] = data_recebimento;
				this.lin++;

				// EA = (long) computeEA(i, this.getId()); /* calcular a chegada estimada do
				// proximo heartbeat */
				EA = (long) computeEAWithMsgId(idmensagem, id);
				to[id] = EA + this.margin; // estimated arrival + safety margin
				this.timeoutIni = to[id];

				/**faz a inserção do TimeOut do proximo heartbeat - URL*/
				Url topico = lista_topicos.get(id);				
				topico.setTimeout_dispositivo(to[id]);

				// to use with Anubis original code
				tempoProximoHeartbeat(id, idmensagem, to[id], data_recebimento);
			} /**fim verificação Timeout para Dispositivo sem heartbeat*/

			totTime = data_recebimento - tsIni; // total time

		} catch (NumberFormatException ex) {
			logger.log(Level.SEVERE, "Erro geral na função Execute da Classe DisaterFD.", ex);
		}
	}

	/**
	 * Método que Soma o fator de Impactos dos processos confiáveis retornando o
	 * trusLevel
	 * para a rede ser considerada confiável o trusLevel deve ser maio ou igual ao
	 * threshold
	 * 
	 * @return int - trusLevel
	 */
	public int sumTrusted() { /* Soma o fator de impacto de processos confiaveis */

		int trusLevel = 0;
		for (int i = 0; i < lista_topicos.size(); i++) {
			if (trusted[i]) {
				trusLevel += lista_topicos.get(i).getFactorImpact();
			}
		}
		return trusLevel;
	}

	/**
	 * Método que calcula o tempo de chegada estimado do próximo heartbeat
	 * 
	 * @return double - avg
	 */
	// calcular a chegada estimada do proximo heartbeat
	public double computeEA(long l, int id) {
		// id of node
		// l = highest number of heartbeat sequence received
		double tot = 0, avg = 0;
		int i = 0;
		long ts;
		try {
			Queue<DeviceReply> q = new LinkedList<>();
			q.addAll(A[id]);
			while (!q.isEmpty()) {
				ts = q.poll().arrivalTime;
				i++;
				tot += ts - (Ai * i);
			}
			if (l > 0) {
				avg = ((1 / (double) l) * (tot)) + (((double) l + 1) * Ai);
			}
			return avg;
		} catch (Exception e) {
			System.out.println("ERRO " + e.getMessage());
			return 0;
		}
	}

	// calculate the estimated arrival of the next heartbeat
	public double computeEAWithMsgId(long l, int id) {
		// id of node
		// l = highest number of heartbeat sequence received
		double partial = 0, tot = 0, avg = 0;
		DeviceReply dr;
		try {
			Queue<DeviceReply> q = new LinkedList<>();
			q.addAll(this.A[id]);
			int n = q.size();
			while (!q.isEmpty()) {
				dr = q.poll();
				partial = dr.arrivalTime - (this.Ai * dr.mid);
				tot += partial;
			}
			if (l > 0) {
				avg = tot / n + (l + 1) * this.Ai;
			}
			return avg;
		} catch (Exception e) {
			System.out.println("ERRO " + e.getMessage());
			return 0;
		}
	}

	/**
	 * Método que retorna o calculo da diferença de tempo entre o timestamp atual e
	 * o anterior
	 * leva a maior diferença entre todos os nós
	 * 
	 * @return long - dif
	 */
	public long getDiference(long[] to, long[] tPrev) {
		long dif = 0, timeDif;
		for (int i = 0; i < lista_topicos.size(); i++) {
			timeDif = (to[i] - tPrev[i]);
			if (timeDif > dif) {
				dif = timeDif;
			}
		}
		return dif;
	}

	/**
	 * Método que reinicializa a Lista de Nós de tempos em tempos conforme o valor
	 * atribudo na variavél Time_analiseRede da Classe Conf.
	 * A cada verificação da confiabilidade do sistema a Lista será reinicializada
	 * verificando se novos novos Nós foram adicionados ou removidos no arquivo JSON (no.json)
	 */
	public static void reinicializarLista(String path) throws IOException {

		/*
		 * Cria um conversor para transformar o arquivo JSON em uma lista de objetos Url
		 */
		ListaObjetos_Converter<Url> converter = new Url_Converter();

		InputStream is = Files.newInputStream(Paths.get(path));

		/* Converte o arquivo JSON em uma nova lista de objetos Url */
		List<Url> newList = converter.jsonToCollection(is);

		/* Cria um conjunto de IDs de dispositivos que existem no novo arquivo JSON */
		Set<Integer> newIds = new HashSet<>();
		for (Url newId : newList) {
			newIds.add(newId.getDeviceID());
		}

		for (Url newId : newList) {
			boolean found = false;
			for (Url existingId : lista_topicos) {
				if (existingId.getDeviceID() == newId.getDeviceID()) {
					existingId.setAddress(newId.getAddress());
					found = true;
					break;
				}
			}
			if (!found) {
				lista_topicos.add(newId);
			}
		}
	}

	/**
	 * Método que atribuir o valor TRUE ou FALSE para um determinado Nó
	 * considerando o mesmo confiável ou não confiável
	 */
	public void trustedDispositivo(boolean valor, int id) {

		for (Url dispositivo : lista_topicos) {

			if (dispositivo.getDeviceID() == id)
				dispositivo.setTrusted(valor);
		}
	}

	/**
	 * Método que a cada verificação da confiabilidade do sistema
	 * irá renomear o arquivo Log_Coap.txt e criará um novo arquivo
	 * para armazenar o tempo de chegada de cada Nó.
	 */
	public void verificarJanela() {

		// String caminho_SO = Diretorio_Copy.caminho_SO();

		try {
			System.out.println("---------------------------------------------");
			System.out.println("Chamei o metodo da classe DisasterFD");
			// System.out.println("Contador..: " + counter);
			System.out.println(
					"Hora Chamada : " + CoAPController.millissegundosData(CoAPController.tempoAtualMilissegundos()));
			System.out.println("---------------------------------------------");

			// Gera Relatório dos Dispositivos (Confiáveis ou Não)
			relatorio(CoAPController.millissegundosData(CoAPController.tempoAtualMilissegundos()));

			// Reinicializar Lista de Nós
			reinicializarLista(CoAPController.path);

			for (Url topico : lista_topicos) {
				System.out.println(topico.toString());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Método que armazena em um arquivo TXT Log_Coap.txt o tempo de envio e
	 * resposta
	 * das requisições solicitadas ao Servidor COAP.
	 */
	public void logCoapTrace(int idDispositivo, int idMensagem, long time_request, long time_response, int alpha) {

		/**
		 * Prepara para escrever no arquivo TXT gerando o histórico
		 * dos heartbeat
		 */
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(CoAPController.path3, true))) {

			/* Escreve no arquivo */
			bw.write(idDispositivo + ";" + idMensagem + ";" + time_request + ";" + time_response + ";" + alpha + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void tempoProximoHeartbeat(int idDispositivo, int idMensagem, long timeout, long arrivalTime) {

		/**
		 * Prepara para escrever no arquivo TXT gerando o histórico
		 * dos heartbeat
		 */
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(CoAPController.path8, true))) {

			/* Escreve no arquivo */
			bw.write(idDispositivo + ";" + idMensagem + ";" + timeout + ";" + arrivalTime + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void nivelTrustRede(int status, long timestamp) {
		
		String nomeArquivo = Diretorio.caminho_SO() + "trustLog.csv";
      
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(nomeArquivo, true))) {
            bw.write(status + ";" + timestamp + "\n");      
        }
		catch (IOException e) {
            e.printStackTrace();
        }
    }

	/**
	 * Método que a cada verificação da confiabilidade do sistema
	 * irá gerar um relatório dos Nós que responderam e não responderam
	 * a solicitação da requisição solicitada pelo Monitor
	 */
	public void relatorio(String horaRelatorioGerado) throws IOException {

		Collections.sort(lista_topicos);

		String reportTitle = "Relatorio Dispositivos";
		String[] columnHeaders = { "ID", "URL", "NÓ RESPONDEU", "Fator de Impacto" };

		// Carrega o arquivo de Relatório
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(CoAPController.path4, true))) {

			bw.write(reportTitle);
			bw.newLine();
			bw.newLine();

			// imprimir titulo da coluna
			for (String columnHeader : columnHeaders) {
				bw.write(String.format("%-38s", columnHeader));
			}
			bw.newLine();

			// imprimir dados
			for (Url dispositivo : lista_topicos) {

				bw.write(String.format("%-15s", dispositivo.getDeviceID()));
				bw.write(String.format("%-65s", dispositivo.getAddress()));
				bw.write(String.format("%-40s", dispositivo.isTrusted()));
				bw.write(String.format("%-35s", dispositivo.getFactorImpact()));
				bw.newLine();
				bw.newLine();
			}
			
			String status_rede = this.threshold > this.trustLevel ? "false" : "true";
			
			bw.write("*********** " + status_rede + " = " + trustLevel + " ***********");
			bw.write(" Relatório Gerado em : " + horaRelatorioGerado);
			bw.write(" *********** FIM DO RELATÓRIO ************** ");
			bw.newLine();
			bw.newLine();
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Método que realiza os calculos estatísticos retornando o tempo total de
	 * funcionamento da rede
	 * o tempo em que a rede não foi confiável, número de erros da rede (quando a
	 * rede não foi confiável)
	 * a taxa de acurácia (capacidade de não suspeitar de um processos corretos)
	 */
	public void realizarEstatistica() throws IOException {

		double rate, pa;
		NumberFormat f = new DecimalFormat("0.000000000000000");

		/*
		 * Estamos calculando a taxa de erro do programa. Onde getError() é uma variável
		 * que conta o número de erros
		 * encontrados durante a execução do programa. O tempo total de execução
		 * (totTime) foi calculado anteriormente
		 * e convertido para segundos dividindo por 1000000000 (pois o tempo está em
		 * nanossegundos).
		 * A divisão de error por totTime resulta na taxa de erro, em erros por segundo.
		 */
		rate = this.error / ((double) this.totTime / 1000000000L); /* error rate */

		/*
		 * Estamos calculando a métrica "PA" (probabilidade de acerto), onde mistakeTime
		 * é o tempo total em que o sistema
		 * estava em um estado não confiável (untrusted). totTime é o tempo total de
		 * execução do programa em segundos.
		 */
		pa = (1 - ((double) this.mistakeTime / (double) this.totTime)); /* calculate pa */

		
    	File file = new File(CoAPController.path2);
        boolean escreverCabecalho = !file.exists() || file.length() == 0;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
          
       	if (escreverCabecalho) {
         bw.write("#server;sizelist;margin;threshold;trust_level;trust_vector;error;mistake_time;error_rate;total_time;pa;td_mean;td_max;date\n");
        }

        String trustedStr = "";
        for (int x = 0; x < trusted.length; ++x) {
              trustedStr += trusted[x] ? "T" : "F";
        }

		/**
		 * Valores que serão armazenado no arquivo Log_Coap.txt
		 * CAMPOS : server; nset; windows_size; margin; threshold; trustLevel; error;
		 * mistaketime; error rate;total time; pa; td mean ; td max; data
		 *
		 * A divisão por 1.000.000 é realizada para converter nanossegundos em
		 * milissegundos.
		 */
		bw.write(this.server + ";" + this.windows_size + ";" + this.margin + ";" + this.threshold
				+ ";" + this.trustLevel + ";" + trustedStr + ";" + this.error + ";" + this.mistakeTime + ";" + f.format(rate)
				+ ";" + this.totTime + ";" + f.format(pa) + ";" + f.format(samplesDT.getMean() / 1000000) + ";"
				+ f.format(samplesDT.getMax() / 1000000) + ";" 
				+ CoAPController.millissegundosData(CoAPController.tempoAtualMilissegundos()) + "\n");
		
	   }
     verificarJanela();  
	}
}
