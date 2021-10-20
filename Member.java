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

  public static int total_members = 3;
  public static int majority = 2;
  public static int accept_count = 0;

  public static int promised_id = -1;
  public static int promised_value = -1;
  public static int max_id = 0;

  public static void main(String[] args) {
    id = Integer.parseInt(args[0]);
    int p = 2000 + id;
    Server server = new Server(p);
    new Thread(server).start();

    Scanner sys_in = new Scanner(System.in);
    String command = sys_in.nextLine();

    while (!command.equals("end")) {
      if(command.equals("prepare")) {
        send_prepare(id);
      }
      command = sys_in.nextLine();
    }

    System.exit(0);

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
      int recID = recReq.id;
      System.out.println(resType);

      if (resType.equals("prepare")) {
        send_promise(id, recID);

      } else if (resType.equals("prepare-ok")) {
        System.out.println("Member " + recReq.id + "  has promised");
        accept_count++;
        System.out.println("Accept count: " + accept_count);
        if (accept_count >= 2) {
          accept_count = 0;
          System.out.println("I have majority promises. I must now propose.");
          send_proposal(id, 1);
        }

      } else if (resType.equals("propose")) {
        if(recID >= max_id) {
          System.out.println("My current max id is: " + max_id + ". The proposal id is: " + recID);
          max_id = recID;
          send_accept(id, recReq.id, recReq.value);
        } else {
          send_fail(id, recID);
        }

      } else if (resType.equals("Accepted")) {
        System.out.println("Member " + recReq.id + " has accepted value " + recReq.value);
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

  private static void send_fail(int id, int p) {
    try {
      int port = 2000 + p;
      Request promise = new Request("fail", id);
      Socket socket = new Socket("localhost", port);
      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      out.writeObject(promise);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void send_proposal(int id, int value) {
    try {
      Request proposal = new Request("propose", id, 1);

      for (int i = 1; i < 4; i++) {
        if(i != id) {
          Socket socket = new Socket("localhost", 2000 + i);
          ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
          out.writeObject(proposal);
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void send_accept(int id, int p, int value) {
    try {
      int port = 2000 + p;
      Request accept = new Request("Accepted", id, value);
      Socket socket = new Socket("localhost", port);
      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      out.writeObject(accept);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
