package transfert;

import java.io.*;
import java.net.Socket;
import java.util.Vector;

public class Client_socket {
    private String ipServer;
    private int portServer;
    private Socket socket;
    private String save_path;

    public String getSave_path() {
        return save_path;
    }

    public void setSave_path(String save_path) {
        this.save_path = save_path;
    }
    public Client_socket(String port, String ip) {
        try{
            this.ipServer = ip;
            this.portServer = Integer.parseInt(port);
            System.out.println("Serveur configuré : IP = " + ipServer + ", Port = " + portServer);

        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Client_socket(String fichierServer) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fichierServer))) {
            String line = reader.readLine(); // Lecture de la première ligne du fichier
            if (line != null) {
                String[] parts = line.split(":---:"); // Découper la ligne avec le délimiteur
                if (parts.length == 3) {
                    this.ipServer = parts[0];
                    this.portServer = Integer.parseInt(parts[1]);
                    System.out.println("Serveur configuré : IP = " + ipServer + ", Port = " + portServer);
                    this.save_path = parts[2];
                    System.out.println("Dossier de sauvegarde : "+this.getSave_path());
                } else {
                    throw new IllegalArgumentException("Format incorrect dans le fichier server.txt");
                }
            } else {
                throw new IOException("Le fichier server.txt est vide.");
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            System.out.println("Erreur lors de la lecture du fichier server.txt");
        }
    }

    // Méthode pour se connecter au serveur   Cree une connexion TCP vers le serveur à l'adresse ipServer:portServer
    public void connectToServer() {
        try {
            socket = new Socket(ipServer, portServer);
            System.out.println("Connecté au serveur : " + ipServer + ":" + portServer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     
    public Vector<String> requestFileList() {
        Vector<String> data = new Vector<String>();
        try (DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
             DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {

            // Envoyer une requête pour lister les fichiers
            dataOut.writeUTF("LIST_FILES");
            dataOut.flush();

            // System.out.println("Requête LIST_FILES envoyée au serveur.");

            // Lire la réponse contenant la liste des fichiers
            String response;
            System.out.println("Liste des fichiers sur le serveur :");
            while (!(response = dataIn.readUTF()).equals("END_OF_LIST")) {
                System.out.println("- "+response);
                data.add(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la requête LIST_FILES.");
        }
        return data;
    }


    public void sendFile(String filePath) {
        try {
            // Normaliser le chemin
            filePath = filePath.replace("\\", "/");
    
            try (FileInputStream fis = new FileInputStream(filePath);
                 DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
    
                // Envoi du nom du fichier
                File file = new File(filePath);
                dataOut.writeUTF(file.getName()); // Envoi du nom du fichier
                dataOut.flush();
    
                byte[] buffer = new byte[4096];
                int bytesRead;
                System.out.println("Envoi du fichier en cours...");
                while ((bytesRead = fis.read(buffer)) > 0) {
                    dataOut.write(buffer, 0, bytesRead);
                }
                dataOut.flush();
                System.out.println("Fichier envoyé avec succès !");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void downloadFile(String fileName, String savePath) {
        try (DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
             DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {

            // Envoyer la commande DOWNLOAD avec le nom du fichier
            dataOut.writeUTF("DOWNLOAD");
            dataOut.writeUTF(fileName);
            dataOut.flush();

            // Réception de la réponse du serveur
            String response = dataIn.readUTF();
            if ("FILE_FOUND".equals(response)) {
                System.out.println("Téléchargement du fichier : " + fileName);

                // Préparer le fichier local pour sauvegarder
                try (FileOutputStream fos = new FileOutputStream(savePath + File.separator + fileName)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = dataIn.read(buffer)) > 0) {
                        fos.write(buffer, 0, bytesRead);
                    }

                    System.out.println("Fichier téléchargé avec succès dans : " + savePath + File.separator + fileName);
                }
            } else {
                System.out.println("Fichier non trouvé sur le serveur.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du téléchargement du fichier.");
        }
    }

    public void removeFile(String fileName) {
        try (DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
             DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {
    
            // Envoyer la commande REMOVE avec le nom du fichier
            dataOut.writeUTF("REMOVE");
            dataOut.writeUTF(fileName);
            dataOut.flush();
    
            // Réception de la réponse du serveur
            String response = dataIn.readUTF();
            if ("FILE_REMOVED".equals(response)) {
                System.out.println("Le fichier '" + fileName + "' a été supprimé avec succès.");
            } else if ("FILE_NOT_FOUND".equals(response)) {
                System.out.println("Le fichier '" + fileName + "' n'existe pas sur le serveur.");
            } else {
                System.out.println("Réponse inattendue du serveur : " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la requête de suppression.");
        }
    }
    public void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Déconnecté du serveur.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public void close() {
        this.disconnectFromServer();
    }
    
}
