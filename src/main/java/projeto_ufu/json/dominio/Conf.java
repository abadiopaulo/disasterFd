package projeto_ufu.json.dominio;

import java.io.Serializable;

public class Conf implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public String server;
    public int windows_size;
    public long margin;
    public int threshold;
    public long alfa;
    public String trace;
    public long timeout_dispositivo;  //tempo em milissegundos 60000ms para timeout de cada dispositivo
    public long timeout_mensagem;  //tempo em milissegundos 40ms para timeout de cada mensagem
    public long time_envio_mensagem;  //tempo em nanosegundos 60000000000ns o que equivale a 60000 millisegundos
    public long time_analiseRede;  // tempo em milissegundos 120000ms o que equivale a 2 minutos
    public long time_consumo_energia; //tempo em milissegundos 300000ms o que equivale a 5 minutos
    public HostMonitor[] outros_monitores;
    
    public Conf() {
        super();
    }

    public Conf(String server, int windows_size, long margin, int threshold, long alfa, String trace,
			long timeout_dispositivo, long timeout_mensagem, long time_envio_mensagem, long time_analiseRede,
			long time_consumo_energia, HostMonitor[] outros_monitores) {

		this.server = server;
		this.windows_size = windows_size;
		this.margin = margin;
		this.threshold = threshold;
		this.alfa = alfa;
		this.trace = trace;
		this.timeout_dispositivo = timeout_dispositivo;
		this.timeout_mensagem = timeout_mensagem;
		this.time_envio_mensagem = time_envio_mensagem;
		this.time_analiseRede = time_analiseRede;
		this.time_consumo_energia = time_consumo_energia;
		this.outros_monitores = outros_monitores;
	}

	public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getWindows_size() {
        return windows_size;
    }

    public void setWindows_size(int windows_size) {
        this.windows_size = windows_size;
    }

    public long getMargin() {
        return margin;
    }

    public void setMargin(long margin) {
        this.margin = margin;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public long getAlfa() {
        return alfa;
    }

    public void setAlfa(long alfa) {
        this.alfa = alfa;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

	    
	public long getTimeout_dispositivo() {
		return timeout_dispositivo;
	}

	public void setTimeout_dispositivo(long timeout_dispositivo) {

		this.timeout_dispositivo = timeout_dispositivo;
		// Converte o valor de nanosegundos para milissegundos no setter
       // this.timeout_dispositivo = timeout_dispositivo / 1000000L;
	}

	public long getTimeout_mensagem() {
		return timeout_mensagem;
	}

	public void setTimeout_mensagem(long timeout_mensagem) {
		this.timeout_mensagem = timeout_mensagem;
	}

	public long getTime_envio_mensagem() {
		return time_envio_mensagem;
	}

	public void setTime_envio_mensagem(long time_envio_mensagem) {
		this.time_envio_mensagem = time_envio_mensagem;
	}

	public long getTime_analiseRede() {
		return time_analiseRede;
	}

	public void setTime_analiseRede(long time_analiseRede) {
		this.time_analiseRede = time_analiseRede;
	}
	
	public long getTime_consumo_energia() {
		return time_consumo_energia;
	}

	public void setTime_consumo_energia(long time_consumo_energia) {
		this.time_consumo_energia = time_consumo_energia;
	}	
	
	public HostMonitor[] getOutros_monitores() {
		return outros_monitores;
	}

	public void setOutros_monitores(HostMonitor[] outros_monitores) {
		this.outros_monitores = outros_monitores;
	}
	
	public static class HostMonitor implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public String ip;
		public String hostname;
				
		public HostMonitor() {
			super();
		}
		
		public HostMonitor(String ip, String hostname) {
			this.ip = ip;
			this.hostname = hostname;			
		}
		
		public String getIp() {
			return ip;
		}
		
		public void setIp(String ip) {
			this.ip = ip;
		}
		
		public String getHostname() {
			return hostname;
		}
		
		public void setHostname(String hostname) {
			this.hostname = hostname;
		}
				
	}
}
