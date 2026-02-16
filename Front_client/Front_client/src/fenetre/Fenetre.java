package fenetre;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Fenetre {
    JFrame fenetre;

    public Fenetre() throws Exception {
        JFrame frame = new JFrame("Transfert de FIichier");
        this.fenetre = frame;
        
        frame.setSize(700, 1000);
        Dimension size = new Dimension(700, 1000);
        frame.setPreferredSize(size);
        frame.setBackground(Color.GRAY);
        frame.setLayout(new FlowLayout());

        JButton choosing_file = new JButton("Choisir un Fichier");
        frame.add(choosing_file,BorderLayout.WEST);

        JPanel contentPanel = new JPanel();
        frame.add(contentPanel, BorderLayout.WEST); // Ajouter le panneau au centre
        choosing_file.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'actionPerformed'");
            }
        });


        contentPanel.add(new JFileChooser());
        
        frame.pack();
        frame.setVisible(true);
        
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
