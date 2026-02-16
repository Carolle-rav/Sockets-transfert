package transfert;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Server_socket {
    private int port;
    private ServerSocket serverSocket;
    private Vector<Slave> slaves = new Vector<>();
    private boolean isRunning = true;
    private List<String> filesList;
    
    public List<String> getFilesList() {
        return filesList;
    }
    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Serveur démarré sur le port " + port);

            while (isRunning) { 
                System.out.println("En attente d'une connexion client...");

                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Connexion établie avec le client.");

                    // Traiter la requête du client
                    handleClientRequest(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Erreur lors du traitement d'une connexion client : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeServer();
        }
    }
    private void handleClientRequest(Socket clientSocket) {
        try (DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream())) {

            String request = dataIn.readUTF(); // Lecture de la requête du client
            System.out.println("Requête reçue : " + request);
            if ("REMOVE".equalsIgnoreCase(request)) {
                handleRemoveRequest(dataIn, dataOut);
            }            
            else if ("LIST_FILES".equalsIgnoreCase(request)) {

                sendFileList(dataOut);
            } else if("DOWNLOAD".equalsIgnoreCase(request)) {
                handleDownloadRequest(dataIn, dataOut);
            } else {
                String fileName = request; // Si ce n'est pas une liste ou un téléchargement, on considère que c'est un fichier à UPLOADer
                byte[] fileBuffer = receiveFile(clientSocket);
                if (fileBuffer != null) {
                    distributeFileToSlaves(fileBuffer, fileName);
                } else {
                    System.out.println("Aucun fichier valide reçu.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void setFilesList(String logFilePath) {
        List<String> fileNames = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileNames.add(line.trim()); 
            }
            this.filesList = fileNames;
            System.out.println("Liste des fichiers initialisée depuis : " + logFilePath);
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier de log : " + logFilePath);
            e.printStackTrace();
        }
    }
    private void handleDownloadRequest(DataInputStream dataIn, DataOutputStream dataOut) {
        try {
            String fileName = dataIn.readUTF(); 
            setFilesList("D:\\Dossier\\Sockets\\Server_socket\\liste_data.txt");
            if (filesList != null && filesList.contains(fileName)) {
                dataOut.writeUTF("FILE_FOUND");
                dataOut.flush();
    
                // Rassembler les parties depuis les slaves
                byte[] completeFile = assembleFileFromSlaves(fileName);
    
                // Envoyer le fichier complet au client
                dataOut.write(completeFile);
                dataOut.flush();
                System.out.println("Fichier envoyé au client : " + fileName);
            } else {
                dataOut.writeUTF("FILE_NOT_FOUND");
                dataOut.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleRemoveRequest(DataInputStream dataIn, DataOutputStream dataOut) {
        try {
            String fileName = dataIn.readUTF(); 
            setFilesList("D:\\Dossier\\Sockets\\Server_socket\\liste_data.txt");
            if (filesList != null && filesList.contains(fileName)) {
                
                removeFileFromLog(fileName);

                removeFileFromSlaves(fileName);
    
                dataOut.writeUTF("FILE_REMOVED");
                dataOut.flush();
                System.out.println("Fichier supprimé : " + fileName);
            } else {
                dataOut.writeUTF("FILE_NOT_FOUND");
                dataOut.flush();
                System.out.println("Fichier non trouvé pour suppression : " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Server_socket(int port, String slaveConfigPath) {
        this.port = port;
        loadSlaves(slaveConfigPath);
    }


    private void loadSlaves(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":---:");
                if (parts.length == 3) {
                    String ip = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    String directory = parts[2];
                    this.slaves.add(new Slave(ip, port, directory));
                }
            }
            System.out.println("Slaves chargés depuis : " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void distributeFileToSlaves(byte[] fileBuffer, String originalFileName) {
        if (slaves.isEmpty() || fileBuffer == null) {
            System.out.println("Aucun slave configuré ou fichier vide.");
            return;
        }

        saveFileNameToLog(originalFileName);

        int partSize = fileBuffer.length / slaves.size();
        int remainingBytes = fileBuffer.length % slaves.size();

        for (int i = 0; i < slaves.size(); i++) {
            int start = i * partSize;
            int end = (i == slaves.size() - 1) ? (start + partSize + remainingBytes) : (start + partSize);

            byte[] part = new byte[end - start];
            System.arraycopy(fileBuffer, start, part, 0, part.length);

            String partName = "part__" + (i + 1) + "__" + originalFileName;
            sendPartToSlave(slaves.get(i), part, partName);
        }
    }
   
   
    
    
    
    private byte[] assembleFileFromSlaves(String fileName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
        for (int i = 0; i < slaves.size(); i++) {
            String partName = "part__" + (i + 1) + "__" + fileName;
    
            try (Socket socket = new Socket(slaves.get(i).getIp(), slaves.get(i).getPort());
                 DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {
    
                // commande pour récupérer une partie
                dataOut.writeUTF("SEND_PART");
                dataOut.writeUTF(partName);
                dataOut.flush();
    
              
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = dataIn.read(buffer)) > 0) {
                    baos.write(buffer, 0, bytesRead);
                }
    
            } catch (IOException e) {
                System.err.println("Erreur lors de la récupération de la partie " + partName);
                e.printStackTrace();
            }
        }
    
        return baos.toByteArray();
    }
    private void removeFileFromSlaves(String fileName) {
        for (int i = 0; i < slaves.size(); i++) {
            String partName = "part__" + (i + 1) + "__" + fileName;
    
            try (Socket socket = new Socket(slaves.get(i).getIp(), slaves.get(i).getPort());
                 DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
    
                //  la commande pour supprimer une partie
                dataOut.writeUTF("REMOVE_PART");
                dataOut.writeUTF(partName);
                dataOut.flush();
                System.out.println("Demande de suppression envoyée pour : " + partName);
    
            } catch (IOException e) {
                System.err.println("Erreur lors de la suppression de la partie " + partName);
                e.printStackTrace();
            }
        }
    }
    private void removeFileFromLog(String fileName) {
        String logFilePath = "D:\\Dossier\\Sockets\\Server_socket\\liste_data.txt";
        List<String> updatedList = new ArrayList<>();
    
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().equals(fileName)) {
                    updatedList.add(line.trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du log pour suppression.");
            e.printStackTrace();
        }
    
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath))) {
            for (String updatedFileName : updatedList) {
                writer.write(updatedFileName);
                writer.newLine();
            }
            System.out.println("Fichier supprimé du log : " + fileName);
        } catch (IOException e) {
            System.err.println("Erreur lors de la mise à jour du log.");
            e.printStackTrace();
        }
    
        // Mettre à jour la liste des fichiers en mémoire
        setFilesList(logFilePath);
    }
     // Recevoir le fichier complet dans un buffer
     private byte[] receiveFile(Socket clientSocket) {
        try (InputStream in = clientSocket.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            System.out.println("Réception du fichier en cours...");
            while ((bytesRead = in.read(buffer)) > 0) {
                baos.write(buffer, 0, bytesRead);
            }

            System.out.println("Fichier reçu avec succès !");
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private void sendPartToSlave(Slave slave, byte[] part, String fileName) {
        try (Socket socket = new Socket(slave.getIp(), slave.getPort());
             DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
    
            System.out.println("Connexion au slave : " + slave.getIp() + ":" + slave.getPort());
            
            // Envoyer la commande RECEIVE_PART
            dataOut.writeUTF("RECEIVE_PART");  
        
            dataOut.writeUTF(fileName);  
    
            // Ensuite, envoyer la partie du fichier
            dataOut.write(part);  
            dataOut.flush();  
    
            System.out.println(fileName + " envoyé avec succès.");
    
        } catch (IOException e) {
            System.err.println("Erreur d'envoi vers le slave : " + slave.getIp() + ":" + slave.getPort());
            e.printStackTrace();
        }
    }
    private void sendFileList(DataOutputStream dataOut) {
        String logFilePath = "D:\\Dossier\\Sockets\\Server_socket\\liste_data.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                dataOut.writeUTF(line); 
            }
            dataOut.writeUTF("END_OF_LIST"); // Indiquer la fin de la liste
            dataOut.flush();
            System.out.println("Liste des fichiers envoyée au client.");
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture ou de l'envoi de la liste des fichiers.");
            e.printStackTrace();
        }
    }
    
    
    public void closeServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Serveur fermé proprement.");
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture du serveur.");
            e.printStackTrace();
        }
    }

    
    


    
    
   
    
    
    private void saveFileNameToLog(String fileName) {
        String logFilePath = "D:\\Dossier\\Sockets\\Server_socket\\liste_data.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(fileName);  // Enregistrer uniquement le nom du fichier principal
            writer.newLine();
            System.out.println("Nom du fichier enregistré dans " + logFilePath);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'enregistrement dans le log.");
            e.printStackTrace();
        }
    }
    
    
    

    
}
