package reader;

import transfert.Client_socket;

import java.util.Scanner;

public class Handler_socket {
    private Client_socket client;

    public Client_socket getClient() {
        return client;
    }

    public void setClient(Client_socket client) {
        this.client = client;
    }

    public Handler_socket() {
        Scanner scanner = new Scanner(System.in);

        // Demander le chemin du fichier de configuration ou l'IP et le port
        System.out.println("Configuration du serveur :");
        System.out.print(
                "Entrez le chemin vers le fichier de configuration ou appuyez sur Entrée pour configurer manuellement : ");
        String configPath = scanner.nextLine();

        if (configPath.isEmpty()) {
            // Demande manuelle des paramètres
            System.out.print("Entrez l'IP du serveur : ");
            String ipServer = scanner.nextLine();

            System.out.print("Entrez le port du serveur : ");
            String portServer = scanner.nextLine();

            // Créer le client avec les paramètres fournis
            client = new Client_socket(portServer, ipServer);
            System.out.print("Entrez le chemin de sauvegarde des fichiers : ");
            String savePath = scanner.nextLine();
            client.setSave_path(savePath);
        } else {
            // Créer le client en utilisant le fichier de configuration
            client = new Client_socket(configPath);
        }

        // Demander le chemin de sauvegarde des fichiers

        System.out.println("Client configuré avec succès !");
    }

    // import java.util.Scanner;

    public void handel() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println(".....................................................");
        System.out.println(".......______........................................");
        System.out.println("....../..___/........................................");
        System.out.println("...../../............................................");
        System.out.println(".....\\..\\____........................................");
        System.out.println("......\\____..\\.......................................");
        System.out.println("...........\\..\\......................................");
        System.out.println("...._______/../......................................");
        System.out.println(".../_________/.OCKET.................................");
        System.out.println(".....................................................");
        
        System.out.println("Commandes disponibles :");
        System.out.println(" - upload \"chemin absolu vers le fichier\"");
        System.out.println(" - download \"nom du fichier\"");
        System.out.println(" - list");
        System.out.println(" - delete \"nom du fichier\"");
        System.out.println(" - exit ou quit pour quitter.");
    
        while (true) {
            System.out.print("\nDossier_socket > ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split(" ", 2); // Découpe la commande et ses arguments
    
            String command = parts[0].toLowerCase();
            String argument = parts.length > 1 ? parts[1].replace("\"", "").trim() : null; // Supprime les guillemets
    
            try {
                switch (command) {
                    case "upload":
                        if (argument != null && !argument.isEmpty()) {
                            client.connectToServer();
                            client.sendFile(argument);
                        } else {
                            System.out.println("Erreur : spécifiez le chemin absolu du fichier à envoyer.");
                        }
                        break;
    
                    case "download":
                        if (argument != null && !argument.isEmpty()) {
                            client.connectToServer();
                            String savePath = client.getSave_path();
                            client.downloadFile(argument, savePath);
                        } else {
                            System.out.println("Erreur : spécifiez le nom du fichier à télécharger.");
                        }
                        break;
    
                    case "list":
                        client.connectToServer();
                        client.requestFileList();
                        break;
    
                    case "delete":
                        if (argument != null && !argument.isEmpty()) {
                            client.connectToServer();
                            client.removeFile(argument);
                        } else {
                            System.out.println("Erreur : spécifiez le nom du fichier à supprimer.");
                        }
                        break;
    
                    case "exit":
                    case "quit":
                        System.out.println("Fermeture du gestionnaire de commandes.");
                        client.disconnectFromServer();
                        return; // Quitte la méthode
    
                    default:
                        System.out.println("Commande inconnue. Essayez à nouveau.");
                }
            } catch (Exception e) {
                System.err.println("Une erreur est survenue : " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Assure la fermeture de la connexion au serveur
                client.close();
            }
        }
    }
    
}
