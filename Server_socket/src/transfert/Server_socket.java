package transfert;

import java.io.*;
import java.net.InetSocketAddress;
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
            setFilesList("D:\\S4\\Dossier\\Sockets\\Server_socket\\liste_data.txt");
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
            setFilesList("D:\\S4\\Dossier\\Sockets\\Server_socket\\liste_data.txt");
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
    /**
     * Vérifie si un slave est en ligne en tentant une connexion avec un timeout.
     */
    private boolean isSlaveAlive(Slave slave) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(slave.getIp(), slave.getPort()), 2000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void distributeFileToSlaves(byte[] fileBuffer, String originalFileName) {
        if (slaves.isEmpty() || fileBuffer == null) {
            System.out.println("Aucun slave configuré ou fichier vide.");
            return;
        }

        // Vérifier que tous les slaves sont en ligne avant de distribuer
        for (int i = 0; i < slaves.size(); i++) {
            if (!isSlaveAlive(slaves.get(i))) {
                System.err.println("ERREUR : Le slave " + slaves.get(i).getIp() + ":" + slaves.get(i).getPort() + " est hors ligne.");
                System.err.println("Upload annulé : tous les slaves doivent être en ligne pour garantir la réplication.");
                return;
            }
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

            // Envoi au slave principal
            sendPartToSlave(slaves.get(i), part, partName);

            // Envoi de la réplique au slave suivant (circulaire)
            int replicaIndex = (i + 1) % slaves.size();
            String replicaName = "replica__" + (i + 1) + "__" + originalFileName;
            sendPartToSlave(slaves.get(replicaIndex), part, replicaName);
            System.out.println("Réplique " + replicaName + " envoyée au slave " + slaves.get(replicaIndex).getIp() + ":" + slaves.get(replicaIndex).getPort());
        }
    }
   
   
    
    
    
    /**
     * Récupère une part depuis un slave donné. Retourne null en cas d'échec.
     */
    private byte[] fetchPartFromSlave(Slave slave, String partName) {
        ByteArrayOutputStream partStream = new ByteArrayOutputStream();
        try (Socket socket = new Socket(slave.getIp(), slave.getPort());
             DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
             DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {

            dataOut.writeUTF("SEND_PART");
            dataOut.writeUTF(partName);
            dataOut.flush();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataIn.read(buffer)) > 0) {
                partStream.write(buffer, 0, bytesRead);
            }

            if (partStream.size() > 0) {
                return partStream.toByteArray();
            }
        } catch (IOException e) {
            System.err.println("Échec récupération de " + partName + " depuis " + slave.getIp() + ":" + slave.getPort());
        }
        return null;
    }

    private byte[] assembleFileFromSlaves(String fileName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
        for (int i = 0; i < slaves.size(); i++) {
            String partName = "part__" + (i + 1) + "__" + fileName;
            String replicaName = "replica__" + (i + 1) + "__" + fileName;
            int replicaIndex = (i + 1) % slaves.size();

            byte[] partData = null;

            // Essayer de récupérer depuis le slave principal
            if (isSlaveAlive(slaves.get(i))) {
                System.out.println("Tentative de récupération de " + partName + " depuis le slave principal...");
                partData = fetchPartFromSlave(slaves.get(i), partName);
            }

            // Si le slave principal a échoué, essayer la réplique sur le slave suivant
            if (partData == null) {
                System.out.println("Slave principal indisponible pour " + partName + ", tentative via la réplique...");
                if (isSlaveAlive(slaves.get(replicaIndex))) {
                    partData = fetchPartFromSlave(slaves.get(replicaIndex), replicaName);
                }
            }

            if (partData != null) {
                baos.write(partData, 0, partData.length);
                System.out.println("Partie " + (i + 1) + " récupérée avec succès.");
            } else {
                System.err.println("ERREUR CRITIQUE : Impossible de récupérer la partie " + (i + 1) + " (ni principal, ni réplique). Fichier incomplet !");
            }
        }
    
        return baos.toByteArray();
    }
    /**
     * Envoie une commande de suppression à un slave. Ignore les erreurs si le slave est hors ligne.
     */
    private void removePartFromSlave(Slave slave, String partName) {
        try (Socket socket = new Socket(slave.getIp(), slave.getPort());
             DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {

            dataOut.writeUTF("REMOVE_PART");
            dataOut.writeUTF(partName);
            dataOut.flush();
            System.out.println("Demande de suppression envoyée pour : " + partName);

        } catch (IOException e) {
            System.err.println("Slave hors ligne, suppression différée pour : " + partName);
        }
    }

    private void removeFileFromSlaves(String fileName) {
        for (int i = 0; i < slaves.size(); i++) {
            String partName = "part__" + (i + 1) + "__" + fileName;
            String replicaName = "replica__" + (i + 1) + "__" + fileName;
            int replicaIndex = (i + 1) % slaves.size();

            // Supprimer la part principale
            removePartFromSlave(slaves.get(i), partName);

            // Supprimer la réplique
            removePartFromSlave(slaves.get(replicaIndex), replicaName);
        }
    }
    private void removeFileFromLog(String fileName) {
        String logFilePath = "D:\\S4\\Dossier\\Sockets\\Server_socket\\liste_data.txt";
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
        String logFilePath = "D:\\S4\\Dossier\\Sockets\\Server_socket\\liste_data.txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    dataOut.writeUTF(line.trim());
                }
            }
            System.out.println("Liste des fichiers envoyée au client.");
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture ou de l'envoi de la liste des fichiers.");
            e.printStackTrace();
        } finally {
            try {
                dataOut.writeUTF("END_OF_LIST"); // Toujours envoyer la fin de liste
                dataOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        String logFilePath = "D:\\S4\\Dossier\\Sockets\\Server_socket\\liste_data.txt";
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
