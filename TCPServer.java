import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

public class TCPServer {
    public static void main(String[] args) {
        try {
            int serverPort = 7896;
            try (ServerSocket listenSocket = new ServerSocket(serverPort)) {
                List<Connection> connections = new ArrayList<>();

                System.out.println("Servidor corriendo en el puerto " + serverPort);

                while (true) {
                    Socket clientSocket = listenSocket.accept();
                    DataInputStream usernameStream = new DataInputStream(clientSocket.getInputStream());
                    String username = usernameStream.readUTF();
                    String clienteIP = clientSocket.getInetAddress().getHostAddress();
                    System.out.println("Conexión desde: " + clienteIP + " con el nombre: " + username + " Time: "
                            + LocalTime.now() + " Date: " + LocalDate.now());
                    Connection c = new Connection(clientSocket, connections, username);
                    connections.add(c);

                    for (Connection connection : connections) {
                        if (!connection.clientSocket.isClosed() && !connection.equals(c)) {
                            connection.out.writeUTF(username + ": se ha conectado");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

class Connection extends Thread {
    DataInputStream in;
    DataOutputStream out;
    Socket clientSocket;
    List<Connection> connections;
    String username;

    public Connection(Socket aClientSocket, List<Connection> connections, String username) {
        try {
            clientSocket = aClientSocket;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            this.connections = connections;
            this.username = username;
            this.start();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void run() {
        try {
            while (true) {
                String data = in.readUTF();
                if (data == null) {
                    break;
                }
                String clienteIP = clientSocket.getInetAddress().getHostAddress();
                System.out
                        .println(clienteIP + " - " + data + " Time: " + LocalTime.now() + " Date: " + LocalDate.now());

                for (Connection connection : connections) {
                    if (!connection.clientSocket.isClosed() && !connection.equals(this)) {
                        connection.out.writeUTF(data);
                    }
                }
            }
        } catch (IOException e) {
        } finally {
            try {
                clientSocket.close();
                System.out.println(
                        "Desconexión desde: " + this.clientSocket.getInetAddress().getHostAddress() + " con el nombre: "
                                + this.username + " Time: " + LocalTime.now() + " Date: " + LocalDate.now());
                for (Connection connection : connections) {
                    if (!connection.clientSocket.isClosed() && !connection.equals(this)) {
                        connection.out.writeUTF(username + ": se ha ido");
                    }
                }
                connections.remove(this);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}