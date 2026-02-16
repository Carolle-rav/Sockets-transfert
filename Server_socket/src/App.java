import transfert.Server_socket;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class App {
    private static Server_socket server;

    public static void main(String[] args) {
        String slaveConfigPath = "D:\\S4\\Dossier\\Sockets\\Slave_socket\\src\\slaves.txt";
        String localIp = "localhost";
        int port = 12345;

      
        try {
            localIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Impossible de récupérer l'adresse IP locale. Utilisation de l'adresse par défaut : " + localIp);
        }

        
        try {
            System.out.println("Démarrage du serveur sur l'adresse IP : " + localIp + " et le port : " + port);
            server = new Server_socket(port, slaveConfigPath);
            server.startServer(); 
            System.out.println("Serveur démarré avec succès !");
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur : " + e.getMessage());
        }

        // Ajout d'un hook pour fermer proprement le serveur à la fin de l'exécution
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null) {
                System.out.println("Fermeture du serveur...");
                server.closeServer();
                System.out.println("Serveur arrêté.");
            }
        }));
    }
}
