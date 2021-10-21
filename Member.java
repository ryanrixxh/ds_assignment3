import java.io.*;
import java.util.*;

import java.net.*;
import org.w3c.dom.*;

import paxos.Request;

class Member extends Thread {
  static ServerSocket server = null;
  static String command = "";
  static Scanner sys_in = new Scanner(System.in);

  public static int current_leader = 0;
  public static int total_members = 5;

  public static int id;
  public static int id_count = id;
  public static int value = id;

  public static int majority = 3;
  public static Boolean propose_init = false;
  public static int promise_count = 0;
  public static int accept_count = 0;
  public static int fail_count = 0;

  public static int promised_id = -1;
  public static int promised_value = -1;
  public static int max_prepare_id = 0;
  public static int max_id = 0;

  public static Boolean send_prior_prop = false;
  public static Boolean prior_recieved = false;
  public static int accepted_id = -1;
  public static int accepted_value = id;

  public static void main(String[] args) {
    try {
      current_leader = find_leader();
      System.out.println("Current Leader: " + current_leader);

      id = Integer.parseInt(args[0]);
      int p = 2000 + id;
      Server server = new Server(p);
      new Thread(server).start();

      Scanner sys_in = new Scanner(System.in);

      while (!command.equals("end")) {
        command = sys_in.nextLine();
        if(command.equals("prepare")) {
          send_prepare(id);
        } else if (command.equals("who")) {
          int leader_found = find_leader();
          System.out.println("Current Leader: " + find_leader());
          if (current_leader != leader_found)
            current_leader = leader_found;
        } else if (command.equals("propose")) {
          propose_init = true;
        }
      }

    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      System.exit(0);
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
      int recID = recReq.id;
      System.out.println(resType);

      if (resType.equals("prepare")) {
        System.out.println("Recieved prepare request from id " + recID);
        System.out.println(max_prepare_id);
        if (recID >= max_prepare_id) {
          max_prepare_id = recID;
          if (send_prior_prop) {
            send_previous_accept(id, accepted_id, recID, value);
          } else {
            send_promise(id, recID);
          }
        } else {
          send_fail(id, recID);
        }

      } else if (resType.equals("prepare-ok")) {
        System.out.println("Member " + recReq.id + "  has promised");
        promise_count++;
        System.out.println("Promise count: " + promise_count);
        if (promise_count >= majority) {
          promise_count = 0;
          if(prior_recieved) {
            System.out.println("I have majority accepts. Proposing M" + accepted_value);
            send_proposal(id, accepted_value);
          } else {
            System.out.println("I have majority accepts. Proposing myself");
            send_proposal(id, id);
          }
        }

      } else if (resType.equals("preAccept")) {
        prior_recieved = true;
        System.out.println("Member " + recReq.id + "  has already accepted value " + recReq.value + " from " + recReq.accepted_id);
        promise_count++;
        accepted_value = recReq.value;
        System.out.println("Promise count: " + promise_count);
        if (promise_count >= majority) {
          promise_count = 0;
          if(prior_recieved) {
            System.out.println("I have majority accepts. Proposing M" + accepted_value);
            send_proposal(id, accepted_value);
          } else {
            System.out.println("I have majority accepts. Proposing myself");
            send_proposal(id, id);
          }
        }

      } else if (resType.equals("propose")) {
        if(recID == max_prepare_id) {
          System.out.println("I promised to id " + max_prepare_id + ". The proposal id is: " + recID);
          max_id = recID;
          send_accept(id, recReq.id, recReq.value);
          send_prior_prop = true;
          value = recReq.value;
          accepted_id = recReq.id;
          System.out.println("I have accepted value " + value + " from Member " + recID);
        } else {
          send_fail(id, recID);
        }

      } else if (resType.equals("Accepted")) {
        System.out.println("Member " + recReq.id + " has accepted value " + recReq.value);
        accept_count++;
        if(accept_count == majority) {
          accept_count = 0;
          if(prior_recieved) {
            System.out.println("The new council leader is " + accepted_value);
            change_leader(accepted_value);
          } else {
            System.out.println("I am the new council leader");
            change_leader(id);
          }
        }

      } else if (resType.equals("fail")) {
        fail_count++;
        if (fail_count >= (total_members - 1)/2) {
          Thread.sleep(200);
          promise_count = 0;
          fail_count = 0;
          System.out.println("Prepare has failed. Not enough promises");
        }
      }
    }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static void send_prepare(int id) {
      Request req = new Request("prepare",id);

      for (int i = 1; i < total_members + 1; i++) {
        if (i != id) {
          try {
          Socket socket = new Socket("localhost",2000 + i);
          ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
          out.writeObject(req);
          }
          catch (ConnectException e) {
            fail_count++;
            System.out.println("Peer " + i + " unavailable. Fails: " + fail_count);
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }

  }

  //Send a promise to accept a proposal along with the id the promise came from
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

  private static void send_previous_accept(int id, int accepted_id, int p, int value) {
    try {
      int port = 2000 + p;
      Request promise = new Request("preAccept", id, accepted_id, value);
      Socket socket = new Socket ("localhost", port);
      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      out.writeObject(promise);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  //Sends a fail along with the ID the fail came from
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

  //Sends a proposal with the id of the proposer and the value being proposed
  private static void send_proposal(int id, int value) {
      Request proposal = new Request("propose", id, value);

      for (int i = 1; i < total_members + 1; i++) {
        if(i != id) {
          try {
            Socket socket = new Socket("localhost",2000 + i);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(proposal);
          }
          catch (ConnectException e) {
            fail_count++;
            System.out.println("Peer " + i + " unavailable. Fails: " + fail_count);
          }
          catch (Exception e) {
            e.printStackTrace();
          }
      }
    }
  }

  //Sends an accept along with the ID it came from and the value it has accepted
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

  //Reads the Council file to find the current leader
  private static int find_leader() {
    int found = -1;
    try {
      File council = new File("Council.txt");
      Scanner file_in = new Scanner(council);
      String leader = file_in.nextLine();

      leader = leader.replaceAll("[^\\d]","");
      found = Integer.parseInt(leader);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      return found;
    }
  }
  //Changes the leader information by writing to a new file and replacing the old file
  private static void change_leader(int id) {
    try {
      File council = new File("Council.txt");
      File temp = new File("temp.txt");
      PrintWriter pw = new PrintWriter(temp);
      Scanner sc = new Scanner(council);

      String newLead = "Leader: " + id + "\r\n";
      int i = 0;
      pw.print(newLead);
      sc.nextLine();
      while(sc.hasNextLine()) {
        pw.print(sc.nextLine());
      }

      sc.close();
      pw.close();

      council.delete();
      temp.renameTo(council);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
