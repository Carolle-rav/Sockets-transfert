package transfert;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Slave_socket {
    private int port;
    private String savePath;

    public Slave_socket(int port, String savePath) {
        this.port = port;
        this.savePath = savePath;
    }
    public void startSlave() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Slave démarré sur le port " + port + ", en attente de connexions...");

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    System.out.println("Connexion établie avec un client (probablement le serveur principal).");

                    // Traiter la requête du serveur principal
                    handleServerRequest(socket);
                } catch (IOException e) {
                    System.err.println("Erreur lors du traitement d'une connexion : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleServerRequest(Socket socket) {
        try (DataInputStream dataIn = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {

            String request = dataIn.readUTF(); // Lire la commande envoyée par le serveur principal
            System.out.println("Requête reçue : " + request);
          


            if ("REMOVE_PART".equalsIgnoreCase(request)) {
                String partName = dataIn.readUTF(); // Lire le nom de la partie à supprimer
                boolean success = removePartFile(partName);
                dataOut.writeUTF(success ? "PART_REMOVED" : "PART_NOT_FOUND");
                dataOut.flush();
            }
            else if ("SEND_PART".equalsIgnoreCase(request)) {
                String partName = dataIn.readUTF(); // Lire le nom de la partie demandée
                sendPartToServer(partName, dataOut);
            } else if ("RECEIVE_PART".equalsIgnoreCase(request)) {
                String partName = dataIn.readUTF(); // Lire le nom de la partie à recevoir
                saveReceivedPart(partName, dataIn);
            } else {
                System.out.println("Commande inconnue : " + request);
                dataOut.writeUTF("UNKNOWN_COMMAND"); // Répondre avec une commande inconnue
                dataOut.flush();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du traitement de la requête : " + e.getMessage());
            e.printStackTrace();
        }
    }
    private boolean removePartFile(String partName) {
        File file = new File(savePath, partName);
        if (file.exists() && file.delete()) {
            System.out.println("Partie supprimée : " + partName);
            return true;
        } else {
            System.err.println("Échec de la suppression de la partie : " + partName);
            return false;
        }
    }
    private void sendPartToServer(String partName, DataOutputStream dataOut) {
        File partFile = new File(savePath, partName);

        if (partFile.exists() && partFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(partFile)) {
                System.out.println("Envoi de la partie : " + partName);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) > 0) {
                    dataOut.write(buffer, 0, bytesRead);
                }
                dataOut.flush();

                System.out.println("Partie envoyée avec succès : " + partName);
            } catch (IOException e) {
                System.err.println("Erreur lors de l'envoi de la partie : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Partie demandée introuvable : " + partName);
            try {
                dataOut.writeUTF("PART_NOT_FOUND"); // Indiquer que la partie est introuvable
                dataOut.flush();
            } catch (IOException e) {
                System.err.println("Erreur lors de l'envoi du message PART_NOT_FOUND : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void saveReceivedPart(String partName, DataInputStream dataIn) {
        File partFile = new File(savePath, partName);

        try (FileOutputStream fos = new FileOutputStream(partFile)) {
            System.out.println("Réception de la partie : " + partName);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataIn.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
            }

            System.out.println("Partie sauvegardée avec succès dans : " + partFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde de la partie : " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Démarrer le slave pour écouter et sauvegarder les fichiers
    // public void startSlave() {
    //     try (ServerSocket serverSocket = new ServerSocket(port)) {
    //         System.out.println("Slave démarré sur le port " + port + ", en attente de fichiers...");
    
    //         while (true) {
    //             Socket socket = serverSocket.accept();
    //             System.out.println("Connexion établie, réception en cours...");
    
    //             try (InputStream in = socket.getInputStream();
    //                  DataInputStream dataIn = new DataInputStream(in)) {
    
    //                 // Lire le nom du fichier envoyé
    //                 String partName = dataIn.readUTF();
    //                 System.out.println("Nom du fichier reçu : " + partName);
    
    //                 // Préparer le fichier pour sauvegarder les données reçues
    //                 try (FileOutputStream fos = new FileOutputStream(savePath + "\\" + partName)) {
    
    //                     byte[] buffer = new byte[4096];
    //                     int bytesRead;
    //                     while ((bytesRead = dataIn.read(buffer)) > 0) {
    //                         fos.write(buffer, 0, bytesRead);
    //                     }
    
    //                     System.out.println("Fichier reçu et sauvegardé dans : " + savePath + "\\" + partName);
    //                 }
    //             }
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }
    // private void handleSendPartRequest(Socket socket) {
    //     try (DataInputStream dataIn = new DataInputStream(socket.getInputStream());
    //          DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
    
    //         String partName = dataIn.readUTF(); // Lire le nom de la partie demandée
    //         File partFile = new File(savePath + "\\" + partName);
    
    //         if (partFile.exists()) {
    //             try (FileInputStream fis = new FileInputStream(partFile)) {
    //                 byte[] buffer = new byte[4096];
    //                 int bytesRead;
    
    //                 while ((bytesRead = fis.read(buffer)) > 0) {
    //                     dataOut.write(buffer, 0, bytesRead);
    //                 }
    
    //                 dataOut.flush();
    //                 System.out.println("Partie envoyée : " + partName);
    //             }
    //         } else {
    //             System.err.println("Partie introuvable : " + partName);
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }
    
    
}
