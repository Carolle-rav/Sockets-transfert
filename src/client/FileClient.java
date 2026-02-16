package client;

import java.io.*;
import java.net.*;

public class FileClient {
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1"; // Adresse du serveur principal
        int serverPort = 1234;

        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connecté au serveur principal.");
            System.out.println("Entrez une commande (UPLOAD <fichier>) :");
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String command = consoleReader.readLine();

            writer.println(command);

            if (command.startsWith("UPLOAD")) {
                String[] parts = command.split(" ", 2);
                String fileName = parts[1];
                File file = new File(fileName);
                if (!file.exists()) {
                    System.out.println("Fichier introuvable : " + fileName);
                    return;
                }

                try (FileInputStream fis = new FileInputStream(file);
                     OutputStream out = socket.getOutputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("Fichier envoyé : " + fileName);
            } else {
                System.out.println("Commande invalide.");
            }
        } catch (IOException e) {
            System.err.println("Erreur du client : " + e.getMessage());
        }
    }
}
