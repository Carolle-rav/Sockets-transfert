import transfert.Slave_socket;

public class App {
    public static void main(String[] args) {

        //Slave 2
        Slave_socket slave2 = new Slave_socket(1010, "D:\\Dossier\\Sockets\\Slave_socket\\Slave_2");
        slave2.startSlave();
    }
}
