package projeto_ufu.json.converter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import projeto_ufu.json.dominio.Url;

public class Url_Converter implements ListaObjetos_Converter<Url> {
	
    private long defaultTimeout;

    // Recebe o timeout padrão no construtor
    public Url_Converter(long defaultTimeout) {
       this.defaultTimeout = defaultTimeout;
    }


    @Override
    public void collectionToJson(String file, List<Url> urls) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        mapper.writeValue(new FileOutputStream(file), urls);
    }

    /*@Override
    public List<Url>jsonToCollection(InputStream file) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(file, new TypeReference<List<Url>>() {});
    }
   */
    
    @Override
    public List<Url> jsonToCollection(InputStream file) throws IOException {
     
   	  ObjectMapper mapper = new ObjectMapper();
        
      List<Url> urls = mapper.readValue(file, new TypeReference<List<Url>>() {});
      
      //Captura o valor atual de System.nanoTime() uma vez e o usa para todas as URLs
      long hora_atual = System.nanoTime();
        
      //Define o timeout de cada URL usando o valor vindo do objeto de configuração
      for (Url url : urls) {
        url.setTimeout_dispositivo(defaultTimeout + hora_atual);
      }
      return urls;
    }

}
