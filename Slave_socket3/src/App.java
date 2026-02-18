import transfert.Slave_socket;

public class App {
    public static void main(String[] args) {

        // Slave 3
        Slave_socket slave2 = new Slave_socket(2510, "D:\\S4\\Dossier\\Sockets\\Slave_socket\\Slave_3");
        slave2.startSlave();
    }
}
