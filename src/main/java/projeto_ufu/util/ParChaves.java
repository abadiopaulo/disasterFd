package projeto_ufu.util;

import java.util.Objects;

public class ParChaves {
    private final int deviceID;
    private final int idMensagem;

    public ParChaves(int deviceID, int idMensagem) {
        this.deviceID = deviceID;
        this.idMensagem = idMensagem;
    }

	public int getDeviceID() {
		return deviceID;
	}

	public int getIdMensagem() {
		return idMensagem;
	}

	@Override
	public int hashCode() {
		return Objects.hash(deviceID, idMensagem);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParChaves other = (ParChaves) obj;
		return deviceID == other.deviceID && idMensagem == other.idMensagem;
	}    
    
}
