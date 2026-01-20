package eu.bmdproject.dataspace.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.klojang.util.ExceptionMethods;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class JsonUtil {

  private static final ObjectMapper MAPPER = ObjectMapperFactory.getObjectMapper();
  private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<Map<String, Object>>> MAP_LIST_TYPE = new TypeReference<>() {};

  public static Map<String, Object> toMap(InputStream in) throws IOException {
    return MAPPER.readValue(in, MAP_TYPE);
  }

  public static Map<String, Object> toMap(byte[] in) throws IOException {
    return MAPPER.readValue(in, MAP_TYPE);
  }

  public static List<String> toListOfString(InputStream in) throws IOException {
    return MAPPER.readValue(in, STRING_LIST_TYPE);
  }

  public static List<Map<String, Object>> toListOfMap(byte[] in) throws IOException {
    return MAPPER.readValue(in, MAP_LIST_TYPE);
  }

  public static List<Map<String, Object>> toListOfMap(InputStream in) throws IOException {
    return MAPPER.readValue(in, MAP_LIST_TYPE);
  }

  public static byte[] toBytes(Map<String, Object> map) throws IOException {
    return MAPPER.writeValueAsBytes(map);
  }

  public static byte[] toBytes(List<Map<String, Object>> maps) throws IOException {
    return MAPPER.writeValueAsBytes(maps);
  }

  public static String toString(Map<String, Object> map) throws IOException {
    return MAPPER.writeValueAsString(map);
  }

  public static String toString(List<Map<String, Object>> maps) throws IOException {
    return MAPPER.writeValueAsString(maps);
  }

  public static String prettyPrint(Object obj) {
    try {
      return MAPPER
          .writer()
          .withDefaultPrettyPrinter()
          .writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw ExceptionMethods.uncheck(e);
    }
  }

}
