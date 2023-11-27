package projeto_ufu.json.converter;

import java.io.File;
import java.io.IOException;

public interface Objeto_Converter<T> {

  T fromJSON(File jsonFile) throws IOException;

}