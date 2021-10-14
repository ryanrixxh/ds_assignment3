import java.io.*;
import java.util.*;

import java.net.*;
import org.w3c.dom.*;

import paxos.Request;

class Member extends Thread {
  static ServerSocket server = null;

  public static int id;
  public static int id_count = id;
  public static int value;

  public static int total_members;
  public static int majority;
  public static int accept_count;

  public static int promised_id = -1;
  public static int promised_value = -1;

  public static void main(String[] args) {
    id = Integer.parseInt(args[0]);
    int p = 2000 + id;
    Server server = new Server(p);
    new Thread(server).start();

    Scanner sys_in = new Scanner(System.in);
    String command = sys_in.nextLine();

    if(command.equals("prepare")) {
      send_prepare(id);
    }
  }

  private static class Server implements Runnable {
    private static int port;

    public Server(int p) {
      this.port = p;
    }

    public void run() {
      try {
        System.out.println("Starting Server at port: " + port + " ...");
        server = new ServerSocket(port);
        server.setReuseAddress(true);

        while (true) {
          Socket socket = server.accept();
          Handler handler = new Handler(socket);
          new Thread(handler).start();
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static class Handler implements Runnable {
    private final Socket recieveSocket;

    public Handler(Socket socket) {
      this.recieveSocket = socket;
    }

    public void run() {
      try {
      System.out.println("Recieved connection, starting handler ...");

      ObjectInputStream in = new ObjectInputStream(recieveSocket.getInputStream());
      Request recReq = (Request) in.readObject();
      String resType = recReq.type;
      System.out.println(resType);

      if (resType.equals("prepare")) {
        send_promise(id, recReq.id);
      } else if (resType.equals("prepare-ok")) {
        System.out.println("Member " + recReq.id + "  has promised");
      }


      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static void send_prepare(int id) {
    try {
      Request req = new Request("prepare",id);

      for (int i = 1; i < 4; i++) {
        if (i != id) {
          Socket socket = new Socket("localhost",2000 + i);
          ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
          out.writeObject(req);
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void send_promise(int id, int p) {
    try {
      int port = 2000 + p;
      Request promise = new Request("prepare-ok", id);
      Socket socket = new Socket("localhost", port);
      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      out.writeObject(promise);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
