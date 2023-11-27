package projeto_ufu.json.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ListaObjetos_Converter<T> {

    void collectionToJson(String file, List<T> entity) throws IOException;

    List<T> jsonToCollection(InputStream is) throws IOException;

}
