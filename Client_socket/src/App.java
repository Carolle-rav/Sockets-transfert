import front.Fenetre;
import transfert.Client_socket;
import reader.Handler_socket;


public class App {
    public static void main(String[] args) {
        Handler_socket handler = new Handler_socket();
        handler.handel();
    }
}
