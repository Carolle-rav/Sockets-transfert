import transfert.Slave_socket;

public class App {
    public static void main(String[] args) {
        //Slave 1
        Slave_socket slave1 = new Slave_socket(9090, "D:\\Dossier\\Sockets\\Slave_socket\\Slave_1");
        slave1.startSlave();
    }
}
