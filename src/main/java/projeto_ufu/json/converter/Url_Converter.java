package projeto_ufu.json.converter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import projeto_ufu.json.dominio.Url;

public class Url_Converter implements ListaObjetos_Converter<Url> {

    @Override
    public void collectionToJson(String file, List<Url> urls) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        mapper.writeValue(new FileOutputStream(file), urls);
    }

    @Override
    public List<Url>jsonToCollection(InputStream file) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(file, new TypeReference<List<Url>>() {});
    }


}
