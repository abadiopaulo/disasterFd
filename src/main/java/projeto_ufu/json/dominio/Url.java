package projeto_ufu.json.dominio;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import projeto_ufu.principal.CoAPController;

public class Url implements Serializable, Comparable<Url> {

	private static final long serialVersionUID = 1L;
	
	public int deviceID;
	public String protocol;
	public int idMensagem;
	public long data_envio;
	public long data_recebimento;
	public String address;
	public int factorImpact;
	public boolean trusted;
	public long timeout_dispositivo;
	public boolean funcionouPeloMenosUmaVez ;

	
	public Url() {
	 super();	

	 this.timeout_dispositivo = ( System.nanoTime() + CoAPController.conf.getTimeout_dispositivo() );
	}

	public Url(int deviceID, String protocol, int idMensagem, long data_envio, long data_recebimento, String address,
			int factorImpact, boolean funcionouPeloMenosUmaVez , boolean trusted ) {
		
		super();
		this.deviceID = deviceID;
		this.protocol = protocol;
		this.idMensagem = idMensagem;
		this.data_envio = data_envio;
		this.data_recebimento = data_recebimento;
		this.address = address;
		this.factorImpact = factorImpact;
		this.trusted = trusted;
		this.funcionouPeloMenosUmaVez  = funcionouPeloMenosUmaVez ;
		
	}

	public Url(int deviceID, String protocol, String address, int factorImpact) {
		
		super();
		this.deviceID = deviceID;
		this.protocol = protocol;		
		this.data_envio = 0;
		this.data_recebimento = 0;
		this.address = address;
		this.factorImpact = factorImpact;		
		this.timeout_dispositivo = CoAPController.conf.getTimeout_dispositivo();
		this.trusted = true;
		this.funcionouPeloMenosUmaVez = false;
	}

	public int getDeviceID() {
		return deviceID;
	}

	public void setDeviceID(int deviceID) {
		this.deviceID = deviceID;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public int getIdMensagem() {
		return idMensagem;
	}

	public void setIdMensagem(int i) {
		this.idMensagem = i;
	}

	public long getData_envio() {
		return data_envio;
	}

	public void setData_envio(long data_envio) {
		this.data_envio = data_envio;
	}

	public long getData_recebimento() {
		return data_recebimento;
	}

	public void setData_recebimento(long data_recebimento) {
		this.data_recebimento = data_recebimento;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getFactorImpact() {
		return factorImpact;
	}

	public void setFactorImpact(int factorImpact) {
		this.factorImpact = factorImpact;
	}

	public boolean isTrusted() {
		return trusted;
	}

	public void setTrusted(boolean trusted) {
		this.trusted = trusted;
	}	
		
	public long getTimeout_dispositivo() {
		return timeout_dispositivo;
	}

	public void setTimeout_dispositivo(long timeout_dispositivo) {
		this.timeout_dispositivo = timeout_dispositivo;
	}

	public boolean isFuncionouPeloMenosUmaVez() {
		return funcionouPeloMenosUmaVez;
	}

	public void setFuncionouPeloMenosUmaVez(boolean funcionouPeloMenosUmaVez) {
		this.funcionouPeloMenosUmaVez = funcionouPeloMenosUmaVez;
	}

	@Override
	public String toString() {
		return "Url [deviceID=" + deviceID + ", protocol=" + protocol + ", idMensagem=" + idMensagem + ", data_envio=" + data_envio
				+ ", data_recebimento=" + data_recebimento + "," + " address=" + address + ", "
				+ "TimeOut_Dispositivo=" + timeout_dispositivo + " factorImpact=" + factorImpact + ", trusted=" + trusted + "]";
	}

	@Override
	public int compareTo(Url url) {
		return Integer.compare(this.deviceID, url.deviceID);
	}
}