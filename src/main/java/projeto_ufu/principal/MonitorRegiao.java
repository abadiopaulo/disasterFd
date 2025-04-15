package projeto_ufu.principal;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import projeto_ufu.json.converter.Conf_Converter;
import projeto_ufu.json.converter.Url_Converter;
import projeto_ufu.json.dominio.Conf;
import projeto_ufu.json.dominio.Url;
import projeto_ufu.monitor.Client;
import projeto_ufu.monitor.Server;

public class MonitorRegiao {
    private String regionName;
    private Conf config;
    private List<Url> devices;
    private CoAPController controller;
    private Client client;
    private Server server;
    
    // Caminhos de log específicos para a região
    private String logCoapPath;
    private String relatorioPath;
    private String statisticPath;
    private String tempoHeartbeatPath;

    public MonitorRegiao(String regionName, String configPath, String devicesPath,
                         String logCoapPath, String relatorioPath, String statisticPath) throws Exception {
        this.regionName = regionName;
        this.logCoapPath = logCoapPath;
        this.relatorioPath = relatorioPath;
        this.statisticPath = statisticPath;
        
        // Carrega a configuração específica da região
        File configFile = new File(configPath);
        Conf_Converter confConv = new Conf_Converter();
        this.config = confConv.fromJSON(configFile);
        
        // Carrega a lista de dispositivos específicos da região
        InputStream devicesIS = Files.newInputStream(Paths.get(devicesPath));
        Url_Converter urlConv = new Url_Converter(config.getTimeout_dispositivo());
        this.devices = urlConv.jsonToCollection(devicesIS);
        
        // Define o diretório da região a partir do configPath
        String regionDir = new File(configPath).getParent() + File.separator;
        this.tempoHeartbeatPath = regionDir + "tempoProximoHeartbeat.txt";
        
        // regionBaseDir  será o mesmo que regionDir
        String regionBaseDir  = regionDir;
        
        // Cria o CoAPController passando os parâmetros adicionais
        this.controller = new CoAPController(devices, config, logCoapPath, statisticPath, relatorioPath, tempoHeartbeatPath, regionBaseDir );
        
        //Instancia o Client e Server com a configuração da região (mas nao executa a comunicação via Socket)        
        this.client = new Client(config);
        this.server = new Server(config);
    }
    
    public void start() {
        controller.start();
    }
}
