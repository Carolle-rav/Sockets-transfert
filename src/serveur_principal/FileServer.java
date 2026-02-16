package serveur_principal;

import java.io.*;
import java.net.*;
import java.util.List;

public class FileServer {
    private static final int BUFFER_SIZE = 1024; // Taille du tampon (1 Ko)
    private static final String STORAGE_DIR = "server_files"; // Répertoire temporaire

    public static void main(String[] args) {
        List<String> subServerAddresses = List.of("127.0.0.1:2345", "127.0.0.1:2346", "127.0.0.1:2347");

        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            System.out.println("Serveur principal démarré, en attente de connexions...");

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    System.out.println("Connexion acceptée de : " + clientSocket.getInetAddress().getHostAddress());

                    String command = reader.readLine(); // Commande du client
                    String[] parts = command.split(" ", 2);

                    if (parts[0].equalsIgnoreCase("UPLOAD") && parts.length > 1) {
                        receiveAndDistributeFile(clientSocket, parts[1], subServerAddresses);
                    } else {
                        writer.println("Commande inconnue ou invalide.");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur du serveur principal : " + e.getMessage());
        }
    }

    private static void receiveAndDistributeFile(Socket clientSocket, String fileName, List<String> subServers) throws IOException {
        // Réception du fichier
        File storageDir = new File(STORAGE_DIR);
        if (!storageDir.exists()) storageDir.mkdirs();

        File receivedFile = new File(storageDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(receivedFile);
             InputStream in = clientSocket.getInputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("Fichier reçu : " + fileName);
        }

        // Diviser et envoyer aux sous-serveurs
        distributeToSubServers(receivedFile, subServers);
    }

    private static void distributeToSubServers(File file, List<String> subServers) throws IOException {
        long partSize = file.length() / subServers.size();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            for (int i = 0; i < subServers.size(); i++) {
                long bytesToSend = i == subServers.size() - 1 ? file.length() - (partSize * i) : partSize;
                File partFile = new File(STORAGE_DIR, "part_" + (i + 1) + ".tmp");
                try (FileOutputStream fos = new FileOutputStream(partFile)) {
                    while (bytesToSend > 0) {
                        int bytesRead = fis.read(buffer, 0, (int) Math.min(BUFFER_SIZE, bytesToSend));
                        if (bytesRead == -1) break;
                        fos.write(buffer, 0, bytesRead);
                        bytesToSend -= bytesRead;
                    }
                }
                sendPartToSubServer(subServers.get(i), partFile);
            }
        }
        System.out.println("Toutes les parties du fichier ont été envoyées aux sous-serveurs.");
    }

    private static void sendPartToSubServer(String address, File partFile) {
        String[] parts = address.split(":");
        String subServerAddress = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket(subServerAddress, port);
             OutputStream out = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(out, true);
             FileInputStream fis = new FileInputStream(partFile)) {

            writer.println(partFile.getName()); // Envoyer le nom du fichier

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("Partie " + partFile.getName() + " envoyée au sous-serveur " + address);
        } catch (IOException e) {
            System.err.println("Erreur d'envoi au sous-serveur : " + e.getMessage());
        }
    }
}
