package transfert;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Slave1 {
    private int port;
    private String savePath;

    public Slave1(int port, String savePath) {
        this.port = port;
        this.savePath = savePath;
    }

    // Démarrer le slave pour écouter et sauvegarder les fichiers
    public void startSlave(String savePath) {
        // Vérifiez si le répertoire de sauvegarde existe, sinon créez-le
        File directory = new File(savePath);
        if (!directory.exists() && !directory.mkdirs()) {
            System.err.println("Impossible de créer le répertoire de sauvegarde : " + savePath);
            return;
        }
    
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Slave démarré sur le port " + port + ", en attente de fichiers...");
    
            while (true) { // Boucle principale
                System.out.println("En attente d'une connexion...");
                try (Socket socket = serverSocket.accept()) {
                    System.out.println("Connexion établie, réception en cours...");
                    handleFileReception(socket, savePath);
                } catch (IOException e) {
                    System.err.println("Erreur lors de la connexion avec le client : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du slave : " + e.getMessage());
        }
    }
    
    // Méthode pour gérer la réception et l'enregistrement d'un fichier
    private void handleFileReception(Socket socket, String savePath) {
        try (InputStream in = socket.getInputStream();
             DataInputStream dataIn = new DataInputStream(in)) {
    
            // Lire le nom du fichier envoyé
            String partName = dataIn.readUTF();
            System.out.println("Nom du fichier reçu : " + partName);
    
            // Préparer le fichier pour sauvegarder les données reçues
            File file = new File(savePath + "\\" + partName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
    
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = dataIn.read(buffer)) > 0) {
                    fos.write(buffer, 0, bytesRead);
                }
    
                System.out.println("Fichier reçu et sauvegardé dans : " + file.getAbsolutePath());
            }
    
        } catch (IOException e) {
            System.err.println("Erreur lors de la réception du fichier : " + e.getMessage());
        }
    }
    
    private void handleSendPartRequest(Socket socket) {
        try (DataInputStream dataIn = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
    
            String partName = dataIn.readUTF(); // Lire le nom de la partie demandée
            File partFile = new File(savePath + "\\" + partName);
    
            if (partFile.exists()) {
                try (FileInputStream fis = new FileInputStream(partFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
    
                    while ((bytesRead = fis.read(buffer)) > 0) {
                        dataOut.write(buffer, 0, bytesRead);
                    }
    
                    dataOut.flush();
                    System.out.println("Partie envoyée : " + partName);
                }
            } else {
                System.err.println("Partie introuvable : " + partName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
}
