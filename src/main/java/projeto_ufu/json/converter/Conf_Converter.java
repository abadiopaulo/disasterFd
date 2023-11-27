package projeto_ufu.json.converter;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import projeto_ufu.json.dominio.Conf;

public class Conf_Converter implements Objeto_Converter<Conf> {
  @Override

  public Conf fromJSON(File jsonFile) throws IOException {

    ObjectMapper objectMapper = new ObjectMapper();

    return objectMapper.readValue(jsonFile, Conf.class);
  }
}