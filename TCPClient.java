import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.sound.sampled.*;

public class TCPClient extends JFrame {
    private JTextField textField;
    // private JButton botonEnviar;
    private JTextPane areaMensajes;
    private String username = null;
    private StyledDocument doc;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private JScrollPane scrollPane;
    private ArrayList<Integer> coloresUsuarioR = new ArrayList<Integer>();
    private ArrayList<Integer> coloresUsuarioG = new ArrayList<Integer>();
    private ArrayList<Integer> coloresUsuarioB = new ArrayList<Integer>();
    private ArrayList<String> usernameOnline = new ArrayList<String>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TCPClient ventana = new TCPClient();
            ventana.setVisible(true);
            ventana.textField.requestFocus();
        });
    }

    public TCPClient() {
        setTitle("Ventana de Mensaje");
        setSize(400, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        textField = new JTextField();
        textField.setPreferredSize(new Dimension(350, 30));
        textField.setBackground(new Color(255, 199, 255));
        textField.setForeground(Color.BLACK);

        // botonEnviar = new JButton("Enviar");

        areaMensajes = new JTextPane();
        areaMensajes.setEditable(false);
        areaMensajes.setBackground(new Color(36, 27, 53));
        doc = areaMensajes.getStyledDocument();
        scrollPane = new JScrollPane(areaMensajes);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (textField.getText().length() > 0) {
                        sendMessage(textField.getText());
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeConnection();
                }
            }
        });

        /*
         * botonEnviar.addActionListener(e -> {
         * sendMessage(textField.getText());
         * });
         */

        JPanel panelSuperior = new JPanel();
        panelSuperior.setBackground(new Color(36, 27, 53));
        panelSuperior.add(textField);
        // panelSuperior.add(botonEnviar);
        add(panelSuperior, BorderLayout.SOUTH);
        add(scrollPane, BorderLayout.CENTER);

        new Thread(() -> initializeConnection()).start();
    }

    private void initializeConnection() {

        try {
            while (username == null || username.length() < 4) {
                username = JOptionPane.showInputDialog("Ingrese su usuario (mínimo 4 caracteres): ");
                if (username == null) {
                    System.exit(0);
                }
            }

            int serverPort = 7896;
            socket = new Socket("192.168.43.120", serverPort);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            out.writeUTF(username);

            new Thread(() -> {
                try {
                    while (true) {
                        String response = in.readUTF();
                        /*
                         * if ("/exit".equals(response)) {
                         * closeConnection();
                         * break;
                         * }
                         */
                        String[] responseSplit = response.split(":", 2);
                        String username = responseSplit[0].trim();
                        String msgs = responseSplit[1];
                        msgs = msgs.substring(1);
                        if (msgs.equals("se ha conectado")) {
                            statusCon(username, searchUser(username), msgs,
                                    new Color(3, 234, 215));
                            try {
                                reproducirAudio("into.wav");
                            } catch (Exception e) {
                            }
                        } else if (msgs.equals("se ha ido")) {
                            statusCon(username, searchUser(username), msgs,
                                    new Color(234, 3, 22));
                            try {
                                reproducirAudio("outo.wav");
                            } catch (Exception e) {
                            }
                            for (int i = 0; i < usernameOnline.size(); i++) {
                                if (usernameOnline.get(i).equals(username)) {
                                    usernameOnline.remove(i);
                                    coloresUsuarioR.remove(i);
                                    coloresUsuarioG.remove(i);
                                    coloresUsuarioB.remove(i);
                                    break;
                                }
                            }
                        } else {
                            // appendText(username, new Color(234, 215, 3), false);
                            // appendText(msgs, new Color(255, 199, 255), false);
                            LocalTime horaActual = LocalTime.now();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                            appendText(username, msgs, horaActual.format(formatter), false);
                        }
                    }
                } catch (IOException e) {
                    // No imprimir mensaje porque cerro conexion
                    // e.printStackTrace();
                }
            }).start();
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(this, "Error de conexion: " + e.getMessage());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error de E/S: " + e.getMessage());
        }
    }

    private void sendMessage(String message) {
        try {
            out.writeUTF(username + ": " + message);
            textField.setText("");
            // appendText(message, Color.WHITE, true);

            LocalTime horaActual = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            appendText("Tu", message, horaActual.format(formatter), true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error de E/S: " + e.getMessage());
        }
    }

    private void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private void appendText(String user, String msgs, String time, boolean rightAlign) {
        int indiceUsuario = searchUser(user);

        SimpleAttributeSet attributesU = new SimpleAttributeSet();
        if (user.equals("Tu") == false) {
            StyleConstants.setForeground(attributesU, new Color(
                    coloresUsuarioR.get(indiceUsuario), coloresUsuarioG.get(indiceUsuario),
                    coloresUsuarioB.get(indiceUsuario)));
            StyleConstants.setAlignment(attributesU,
                    rightAlign ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            doc.setParagraphAttributes(doc.getLength(), 1, attributesU, false);
        }

        SimpleAttributeSet attributesM = new SimpleAttributeSet();
        StyleConstants.setForeground(attributesM, new Color(255, 199, 255));
        StyleConstants.setAlignment(attributesM, rightAlign ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
        doc.setParagraphAttributes(doc.getLength(), 1, attributesM, false);

        SimpleAttributeSet attributesT = new SimpleAttributeSet();
        StyleConstants.setForeground(attributesT, new Color(203, 128, 255));
        StyleConstants.setAlignment(attributesT, rightAlign ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
        doc.setParagraphAttributes(doc.getLength(), 1, attributesT, false);
        try {
            if (user.equals("Tu") == false) {
                doc.insertString(doc.getLength(), user + "\n", attributesU);
                try {
                    reproducirAudio("msgs.wav");
                } catch (Exception e) {
                }
            }
            doc.insertString(doc.getLength(), msgs + "\n", attributesM);
            doc.insertString(doc.getLength(), time + "\n\n", attributesT);
            scrollToBottom();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        scrollToBottom();
    }

    private void statusCon(String username, int indiceUser, String text, Color color2) {
        SimpleAttributeSet attributesU = new SimpleAttributeSet();
        StyleConstants.setForeground(attributesU, new Color(
                coloresUsuarioR.get(indiceUser), coloresUsuarioG.get(indiceUser),
                coloresUsuarioB.get(indiceUser)));
        StyleConstants.setAlignment(attributesU, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(doc.getLength(), 1, attributesU, false);

        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color2);
        StyleConstants.setAlignment(attributes, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(doc.getLength(), 1, attributes, false);
        try {
            doc.insertString(doc.getLength(), username + " ", attributesU);
            doc.insertString(doc.getLength(), text + "\n", attributes);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        scrollToBottom();
    }

    private void scrollToBottom() {
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }

    private int searchUser(String user) {
        int indiceUsuario = -1;
        for (int i = 0; i < usernameOnline.size(); i++) {
            if (usernameOnline.get(i).equals(user)) {
                indiceUsuario = i;
                break;
            }
        }
        if (indiceUsuario == -1) {
            usernameOnline.add(user);
            Random rand = new Random((System.currentTimeMillis()) * 11);
            Integer red = 46 + rand.nextInt(210);
            coloresUsuarioR.add(red);
            rand = new Random((System.currentTimeMillis()) * 5);
            Integer green = 37 + rand.nextInt(219);
            coloresUsuarioG.add(green);
            rand = new Random((System.currentTimeMillis()) * 7);
            Integer blue = 63 + rand.nextInt(193);
            coloresUsuarioB.add(blue);
            indiceUsuario = usernameOnline.size() - 1;
            // System.out.println(indiceUsuario + " " + user + " " + red + " " + green + " "
            // + blue + " ");
        }
        return indiceUsuario;
    }

    public static AudioInputStream cargarAudio(String ruta) throws UnsupportedAudioFileException, IOException {
        File archivoWAV = new File(ruta);
        return AudioSystem.getAudioInputStream(archivoWAV);
    }

    public static void reproducirAudio(String ruta) throws LineUnavailableException {
        Thread t = new Thread(() -> {
            try (AudioInputStream audioInputStream = cargarAudio(ruta)) {
                AudioFormat format = audioInputStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int bytesRead;

                while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) >= 0) {
                    line.write(buffer, 0, bytesRead);
                }

                line.drain();
                line.close();
            } catch (UnsupportedAudioFileException e) {
                e.printStackTrace();
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        t.start();
    }
}