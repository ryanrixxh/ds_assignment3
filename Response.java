package paxos;
import java.io.*;
import java.net.*;

import paxos.Request;

public class Response extends Request {
  public int value;

  public Response (String type_in, int id_in) {
    value = -1;
    type = type_in;
    id = id_in;
  }

  public Response (String type_in, int id_in, int value_in) {
    value = value_in;
    type = type_in;
    id = id_in;
  }
}
