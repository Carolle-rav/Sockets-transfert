package sous_serveur;

import java.io.*;
import java.net.*;

public class SubServer {
    private static final int BUFFER_SIZE = 1024; // Taille du tampon
    private static final String STORAGE_DIR = "sub_server_files"; // Répertoire de stockage

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java SubServer <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Sous-serveur démarré sur le port : " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     InputStream in = clientSocket.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

                    String fileName = reader.readLine();
                    File storageDir = new File(STORAGE_DIR);
                    if (!storageDir.exists()) storageDir.mkdirs();

                    File partFile = new File(storageDir, fileName);
                    try (FileOutputStream fos = new FileOutputStream(partFile)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }

                    System.out.println("Partie " + fileName + " reçue et stockée.");
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur du sous-serveur : " + e.getMessage());
        }
    }
}
//systeme replications
//auto enregistrement slaves / broadcast/ s identifie direct au master