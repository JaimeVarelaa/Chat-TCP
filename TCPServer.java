import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class TCPServer {

    public static ArrayList<String> mensajes = new ArrayList<String>();
    static String evento;
    public static String archivo = "eventos" + LocalDate.now() + LocalTime.now() + ".txt";

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
                    evento = "Conexión desde: " + clienteIP + " con el nombre: " + username + " Time: "
                            + LocalTime.now() + " Date: " + LocalDate.now();
                    System.out.println(evento);
                    escribir(evento);
                    Connection c = new Connection(clientSocket, connections, username);
                    connections.add(c);

                    /*A un nuevo usuario se manda todos los mensajes, no me parece útil bajo este contexto
                    for(String mensaje : mensajes){
                        c.out.writeUTF(mensaje);
                    }*/

                    for (Connection connection : connections) {
                        if (!connection.clientSocket.isClosed() && !connection.equals(c)) {
                            connection.out.writeUTF(username + ": se ha conectado");
                            mensajes.add(username + ": se ha conectado");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void escribir(String evento) {
        try {
            FileWriter fileWriter = new FileWriter(archivo, true);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            writer.write(evento);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            System.out.println("Error al escribir en el archivo: " + e.getMessage());
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
                TCPServer.evento = clienteIP + " - " + data + " Time: " + LocalTime.now() + " Date: " + LocalDate.now();
                System.out.println(TCPServer.evento);
                TCPServer.escribir(TCPServer.evento);
                TCPServer.mensajes.add(data);
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
                TCPServer.evento="Desconexión desde: " + this.clientSocket.getInetAddress().getHostAddress() + " con el nombre: "+ this.username + " Time: " + LocalTime.now() + " Date: " + LocalDate.now();
                System.out.println(TCPServer.evento);
                TCPServer.escribir(TCPServer.evento);
                for (Connection connection : connections) {
                    if (!connection.clientSocket.isClosed() && !connection.equals(this)) {
                        connection.out.writeUTF(username + ": se ha ido");
                        TCPServer.mensajes.add(username + ": se ha ido");
                    }
                }
                connections.remove(this);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}