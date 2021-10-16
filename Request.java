package paxos;
import java.io.*;
import java.net.*;

public class Request implements Serializable {
  public int id;
  public String type;
  public int value;

  public Request (String type_in, int id_in) {
    value = -1;
    type = type_in;
    id = id_in;
  }

  public Request (String type_in, int id_in, int value_in) {
    value = value_in;
    type = type_in;
    id = id_in;
  }
}
