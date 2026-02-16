package front;

import javax.swing.*;

import transfert.Client_socket;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

public class Fenetre {
    private JFrame principale;
    private JPanel panelListeFichiers;
    private JButton btnActualiser, btnChoisirFichier, btnEnvoyerFichier;
    private JTextField txtNomFichier;
    private JFileChooser fileChooser;
    Client_socket client_socket;
    String save_path;

    public String getSave_path() {
        return save_path;
    }

    public void setSave_path(String save_path) {
        this.save_path = save_path;
    }

    public Client_socket getClient_socket() {
        return client_socket;
    }

    public void setClient_socket(Client_socket client_socket) {
        this.client_socket = client_socket;
    }

    public Fenetre() {
        // Initialisation de la fenêtre principale
        principale = new JFrame("Transfert de fichier");
        principale.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        principale.setSize(600, 400);
        principale.setLayout(new BorderLayout());

        // Panel liste des fichiers
        panelListeFichiers = new JPanel();
        panelListeFichiers.setLayout(new BoxLayout(panelListeFichiers, BoxLayout.Y_AXIS));
        panelListeFichiers.setBorder(BorderFactory.createTitledBorder("Liste des fichiers sur le serveur"));

        // Bouton pour actualiser la liste
        btnActualiser = new JButton("Actualiser la liste");
        panelListeFichiers.add(btnActualiser);

        // Panel pour l'upload de fichier
        JPanel panelUpload = new JPanel(new GridLayout(2, 1));
        panelUpload.setBorder(BorderFactory.createTitledBorder("Upload de fichier"));

        // Ligne pour sélectionner un fichier
        JPanel ligneChoisirFichier = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtNomFichier = new JTextField(30);
        txtNomFichier.setEditable(false);
        btnChoisirFichier = new JButton("Choisir un fichier");
        ligneChoisirFichier.add(new JLabel("Fichier :"));
        ligneChoisirFichier.add(txtNomFichier);
        ligneChoisirFichier.add(btnChoisirFichier);

        // Ligne pour envoyer le fichier
        JPanel ligneEnvoyerFichier = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnEnvoyerFichier = new JButton("Envoyer le fichier");
        ligneEnvoyerFichier.add(btnEnvoyerFichier);

        // Ajout des lignes au panel upload
        panelUpload.add(ligneChoisirFichier);
        panelUpload.add(ligneEnvoyerFichier);

        // Ajout des panels à la fenêtre principale
        principale.add(panelListeFichiers, BorderLayout.CENTER);
        principale.add(panelUpload, BorderLayout.SOUTH);

        // Configurer JFileChooser
        fileChooser = new JFileChooser();

        // Ajout des listeners pour les boutons
        btnChoisirFichier.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int retour = fileChooser.showOpenDialog(principale);
                if (retour == JFileChooser.APPROVE_OPTION) {
                    txtNomFichier.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });

        btnEnvoyerFichier.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String cheminFichier = txtNomFichier.getText();
                if (!cheminFichier.isEmpty()) {
                    JOptionPane.showMessageDialog(principale, "Fichier envoyé : " + cheminFichier);
                    // Ajouter ici le code pour envoyer le fichier
                    client_socket.connectToServer();
                    client_socket.sendFile(cheminFichier);
                    client_socket.close();
                } else {
                    JOptionPane.showMessageDialog(principale, "Veuillez choisir un fichier avant d'envoyer.");
                }
            }
        });

        btnActualiser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ajouter ici le code pour actualiser la liste des fichiers
                // Exemple d'ajout de données à la liste

                panelListeFichiers.removeAll(); // Vide la liste actuelle
                panelListeFichiers.add(btnActualiser); // Réajoute le bouton d'actualisation
                // Simuler l'ajout de fichiers (vous pouvez remplacer cela par une récupération réelle depuis un serveur)
                client_socket.connectToServer();
                Vector<String> list = client_socket.requestFileList();
                client_socket.close();
                for (String string : list) {
                    addFileLink(string);
                }

                // Rafraîchit l'affichage
                panelListeFichiers.revalidate();
                panelListeFichiers.repaint();

                JOptionPane.showMessageDialog(principale, "Liste des fichiers actualisée.");
            }
        });

        // Rendre la fenêtre visible
        principale.setVisible(true);
    }

    // Méthode pour ajouter un lien cliquable pour chaque fichier
    private void addFileLink(String fileName) {
        JLabel fileLink = new JLabel("<html><a href='#'>" + fileName + "</a></html>");
        fileLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fileLink.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                // Affiche une boîte de dialogue pour choisir une action (Télécharger ou Supprimer)
                int option = JOptionPane.showOptionDialog(principale, 
                        "Que voulez-vous faire avec le fichier " + fileName + " ?", 
                        "Choisir une action", 
                        JOptionPane.DEFAULT_OPTION, 
                        JOptionPane.INFORMATION_MESSAGE, 
                        null, 
                        new String[]{"Télécharger", "Supprimer"}, 
                        "Télécharger");
                
                if (option == 0) {
                    // Code pour télécharger le fichier
                    client_socket.connectToServer();
                    client_socket.downloadFile(fileName, client_socket.getSave_path());
                    client_socket.close();
                    JOptionPane.showMessageDialog(principale, "Téléchargement du fichier : " + fileName);
                } else if (option == 1) {
                    // Code pour supprimer le fichier
                    client_socket.connectToServer();
                    client_socket.removeFile(fileName);
                    client_socket.close();
                    JOptionPane.showMessageDialog(principale, "Fichier supprimé : " + fileName);
                }
            }
        });
        panelListeFichiers.add(fileLink);
    }

    public JFrame getPrincipale() {
        return principale;
    }

    public void setPrincipale(JFrame principale) {
        this.principale = principale;
    }
}